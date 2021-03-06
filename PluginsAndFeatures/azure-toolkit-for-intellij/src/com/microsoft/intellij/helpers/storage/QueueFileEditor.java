/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers.storage;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.icons.AllIcons;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.intellij.forms.QueueMessageForm;
import com.microsoft.intellij.forms.ViewMessageForm;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.QueueMessage;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import icons.CommonIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.List;

public class QueueFileEditor implements FileEditor {
    static final String OPEN = "Open";
    static final String DEQUEUE = "Dequeue";
    static final String ADD_MESSAGE = "Add Message";
    static final String CLEAR_QUEUE = "Clear Queue";
    static final String REFRESH = "Refresh";

    private final Project project;
    private final VirtualFile virtualFile;
    private ClientStorageAccount storageAccount;
    private Queue queue;
    private JPanel mainPanel;
    private JButton dequeueMessageButton;
    private JButton refreshButton;
    private JButton addMessageButton;
    private JButton clearQueueButton;
    private JTable queueTable;
    private List<QueueMessage> queueMessages;

    private FileEditorVirtualNode fileEditorVirtualNode;

    public QueueFileEditor(final Project project, final VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        fileEditorVirtualNode = createFileEditorVirtualNode("");
        queueTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };

        model.addColumn("Id");
        model.addColumn("Message Text Preview");
        model.addColumn("Size");
        model.addColumn("Insertion Time (UTC)");
        model.addColumn("Expiration Time (UTC)");
        model.addColumn("Dequeue count");

        queueTable.setModel(model);
        queueTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        queueTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        queueTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        queueTable.getColumnModel().getColumn(3).setPreferredWidth(40);
        queueTable.getColumnModel().getColumn(4).setPreferredWidth(40);
        queueTable.getColumnModel().getColumn(5).setPreferredWidth(20);

        JTableHeader tableHeader = queueTable.getTableHeader();
        Dimension headerSize = tableHeader.getPreferredSize();
        headerSize.setSize(headerSize.getWidth(), 18);
        tableHeader.setPreferredSize(headerSize);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        queueTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getComponent() instanceof JTable) {
                    int r = queueTable.rowAtPoint(me.getPoint());

                    if (r >= 0 && r < queueTable.getRowCount()) {
                        queueTable.setRowSelectionInterval(r, r);
                    } else {
                        queueTable.clearSelection();
                    }

                    int rowIndex = queueTable.getSelectedRow();

                    if (rowIndex < 0) {
                        return;
                    }

                    if (me.getClickCount() == 2) {
                        viewMessageText();
                    }

                    if (me.getButton() == 3) {
                        QueueMessage message = getSelectedQueueMessage();

                        if (message != null) {
                            JPopupMenu popup = createTablePopUp(r == 0);
                            popup.show(me.getComponent(), me.getX(), me.getY());
                        }
                    }
                }
            }
        });

        queueTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    viewMessageText();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });

        refreshButton.setIcon(CommonIcons.Refresh);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(REFRESH).fireNodeActionEvent();
            }
        });

        dequeueMessageButton.setIcon(CommonIcons.Open);
        dequeueMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(DEQUEUE).fireNodeActionEvent();
            }
        });

        addMessageButton.setIcon(AllIcons.General.Add);
        addMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(ADD_MESSAGE).fireNodeActionEvent();
            }
        });

        clearQueueButton.setIcon(AllIcons.General.Reset);
        clearQueueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(CLEAR_QUEUE).fireNodeActionEvent();
            }
        });
    }

    private FileEditorVirtualNode createFileEditorVirtualNode(final String name) {
        FileEditorVirtualNode node = new FileEditorVirtualNode(this, name);
        node.addAction(REFRESH, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                fillGrid();
            }
        });
        node.addAction(DEQUEUE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                dequeueFirstMessage();
            }
        });
        node.addAction(ADD_MESSAGE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                QueueMessageForm queueMessageForm = new QueueMessageForm(project);
                queueMessageForm.setQueue(queue);
                queueMessageForm.setStorageAccount(storageAccount);

                queueMessageForm.setOnAddedMessage(new Runnable() {
                    @Override
                    public void run() {
                        fillGrid();
                    }
                });

                queueMessageForm.show();
            }
        });
        node.addAction(CLEAR_QUEUE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
            boolean isConfirm = DefaultLoader.getUIHelper().showYesNoDialog(
                mainPanel, "Are you sure you want to clear the queue \"" + queue.getName() + "\"?",
                "Clear Queue", null);

            if (isConfirm) {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Clearing queue messages", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        try {
                            StorageClientSDKManager.getManager().clearQueue(storageAccount, queue);

                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    fillGrid();
                                }
                            });
                        } catch (AzureCmdException e) {
                            String msg = "An error occurred while attempting to clear queue messages." + "\n" +
                                String.format(AzureBundle.message("webappExpMsg"), e.getMessage());
                            PluginUtil.displayErrorDialogAndLog(AzureBundle.message("errTtl"), msg, e);
                        }
                    }
                });
            }
            }
        });
        node.addAction(OPEN, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                viewMessageText();
            }
        });
        return node;
    }

    public void fillGrid() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading queue messages", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    queueMessages = StorageClientSDKManager.getManager().getQueueMessages(storageAccount, queue);

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DefaultTableModel model = (DefaultTableModel) queueTable.getModel();

                            while (model.getRowCount() > 0) {
                                model.removeRow(0);
                            }

                            for (QueueMessage queueMessage : queueMessages) {
                                String[] values = {
                                    queueMessage.getId(),
                                    queueMessage.getContent(),
                                    UIHelperImpl.readableFileSize(queueMessage.getContent().length()),
                                    new SimpleDateFormat().format(queueMessage.getInsertionTime().getTime()),
                                    new SimpleDateFormat().format(queueMessage.getExpirationTime().getTime()),
                                    String.valueOf(queueMessage.getDequeueCount()),
                                    };

                                model.addRow(values);
                            }

                            clearQueueButton.setEnabled(queueMessages.size() != 0);
                            dequeueMessageButton.setEnabled(queueMessages.size() != 0);
                        }
                    });

                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to get queue messages." + "\n"
                        + String.format(AzureBundle.message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(AzureBundle.message("errTtl"), msg, e);
                }
            }
        });
    }

    private JPopupMenu createTablePopUp(boolean isFirstRow) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(fileEditorVirtualNode.createJMenuItem(OPEN));
        JMenuItem dequeueMenu = fileEditorVirtualNode.createJMenuItem(DEQUEUE);
        dequeueMenu.setEnabled(isFirstRow);
        menu.add(dequeueMenu);

        return menu;
    }

    private void dequeueFirstMessage() {
        final boolean isConfirm = DefaultLoader.getUIHelper().showYesNoDialog(
            mainPanel, "Are you sure you want to dequeue the first message in the queue?", "Azure Explorer", null);
        if (isConfirm) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Dequeuing message", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        StorageClientSDKManager.getManager().dequeueFirstQueueMessage(storageAccount, queue);

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fillGrid();
                            }
                        });
                    } catch (AzureCmdException e) {
                        String msg = "An error occurred while attempting to dequeue messages." + "\n"
                            + String.format(AzureBundle.message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogAndLog(AzureBundle.message("errTtl"), msg, e);
                    }
                }
            });
        }
    }

    private QueueMessage getSelectedQueueMessage() {
        return (queueMessages != null && queueMessages.size() > 0)
                ? queueMessages.get(queueTable.getSelectedRow()) : null;
    }

    private void viewMessageText() {
        ViewMessageForm viewMessageForm = new ViewMessageForm(project);
        viewMessageForm.setMessage(queueMessages.get(queueTable.getSelectedRow()).getContent());
        viewMessageForm.show();
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return mainPanel;
    }

    @NotNull
    @Override
    public String getName() {
        return queue.getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
        try {
            unregisterSubscriptionsChanged();
        } catch (AzureCmdException ignored) {
        }
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return virtualFile;
    }

    //    private void registerSubscriptionsChanged()
    //            throws AzureCmdException {
    //        synchronized (subscriptionsChangedSync) {
    //            if (subscriptionsChanged == null) {
    //                subscriptionsChanged = AzureManagerImpl.getManager(project).registerSubscriptionsChanged();
    //            }
    //
    //            registeredSubscriptionsChanged = true;
    //
    //            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        subscriptionsChanged.waitEvent(new Runnable() {
    //                            @Override
    //                            public void run() {
    //                                if (registeredSubscriptionsChanged) {
    //                                    Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(project, storageAccount.getName(), queue);
    //
    //                                    if (openedFile != null) {
    //                                        DefaultLoader.getIdeHelper().closeFile(project, openedFile);
    //                                    }
    //                                }
    //                            }
    //                        });
    //                    } catch (AzureCmdException ignored) {
    //                    }
    //                }
    //            });
    //        }
    //    }

    private void unregisterSubscriptionsChanged()
            throws AzureCmdException {
    //        synchronized (subscriptionsChangedSync) {
    //            registeredSubscriptionsChanged = false;
    //
    //            if (subscriptionsChanged != null) {
    //                AzureManagerImpl.getManager(project).unregisterSubscriptionsChanged(subscriptionsChanged);
    //                subscriptionsChanged = null;
    //            }
    //        }
    }
}
