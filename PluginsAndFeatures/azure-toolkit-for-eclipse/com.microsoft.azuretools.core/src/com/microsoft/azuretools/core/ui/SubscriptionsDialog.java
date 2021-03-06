/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SELECT_SUBSCRIPTIONS;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.ProgressDialog;

public class SubscriptionsDialog extends AzureTitleAreaDialogWrapper {
    private static final String SUBSCRIPTION_MESSAGE = "Select subscription(s) you want to use.";
    private static final String SUBSCRIPTION_TITLE = "Your Subscriptions";
    private static final String DIALOG_TITLE = "Select Subscriptions";
    private static ILog LOG = Activator.getDefault().getLog();

    private Table table;

    private SubscriptionManager subscriptionManager;
    private List<SubscriptionDetail> sdl;

    /**
     * Create the dialog.
     * @param parentShell
     */
    private SubscriptionsDialog(Shell parentShell, SubscriptionManager subscriptionManage) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.subscriptionManager = subscriptionManage;
    }

    public static SubscriptionsDialog go(Shell parentShell, SubscriptionManager subscriptionManager) {
        SubscriptionsDialog d = new SubscriptionsDialog(parentShell, subscriptionManager);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(SUBSCRIPTION_MESSAGE);
        setTitle(SUBSCRIPTION_TITLE);
        getShell().setText(DIALOG_TITLE);
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        GridData gdTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdTable.heightHint = 300;
        table.setLayoutData(gdTable);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn.setWidth(300);
        tblclmnNewColumn.setText("Subscription Name");

        TableColumn tblclmnNewColumn1 = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn1.setWidth(270);
        tblclmnNewColumn1.setText("Subscription ID");

        Button btnRefresh = new Button(container, SWT.NONE);
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshSubscriptions();
            }
        });
        GridData gdBtnRefresh = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
        gdBtnRefresh.widthHint = 78;
        btnRefresh.setLayoutData(gdBtnRefresh);
        btnRefresh.setText("Refresh");

        return area;
    }

    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                System.out.println("refreshSubscriptionsAsync");
                refreshSubscriptionsAsync();
                setSubscriptionDetails();
            }
        });
    }

    public void refreshSubscriptionsAsync() {
        try {
            ProgressDialog.get(getShell(), "Update Azure Local Cache Progress").run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Reading subscriptions...", IProgressMonitor.UNKNOWN);
                    EventUtil.executeWithLog(TelemetryConstants.ACCOUNT, TelemetryConstants.GET_SUBSCRIPTIONS, (operation) -> {
                        subscriptionManager.getSubscriptionDetails();
                    }, (ex) -> {
                            ex.printStackTrace();
                            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@efreshSubscriptionsAsync@SubscriptionDialog", ex));
                        });
                    monitor.done();
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            //LOGGER.log(LogService.LOG_ERROR, "run@refreshSubscriptionsAsync@SubscriptionDialog", e);
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@refreshSubscriptionsAsync@SubscriptionDialog", ex));
        }
    }

    private void setSubscriptionDetails() {
        sdl = subscriptionManager.getSubscriptionDetails();
        for (SubscriptionDetail sd : sdl) {
            TableItem item = new TableItem(table, SWT.NULL);
            item.setText(new String[] {sd.getSubscriptionName(), sd.getSubscriptionId()});
            item.setChecked(sd.isSelected());
        }
    }

    private void refreshSubscriptions() {
        System.out.println("refreshSubscriptions");
        table.removeAll();
        subscriptionManager.cleanSubscriptions();
        refreshSubscriptionsAsync();
        setSubscriptionDetails();
        subscriptionManager.setSubscriptionDetails(sdl);
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Select");
    }

    @Override
    public void okPressed() {
        EventUtil.logEvent(EventType.info, ACCOUNT, SELECT_SUBSCRIPTIONS, null);
        TableItem[] tia = table.getItems();
        int chekedCount = 0;
        for (TableItem ti : tia) {
            if (ti.getChecked()) {
                chekedCount++;
            }
        }

        if (chekedCount == 0) {
            this.setErrorMessage("Select at least one subscription");
            return;
        }

        for (int i = 0; i < tia.length; ++i) {
            this.sdl.get(i).setSelected(tia[i].getChecked());
        }

        try {
            subscriptionManager.setSubscriptionDetails(sdl);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@SubscriptionDialog", ex));
        }

        super.okPressed();
    }
}
