/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.CommonConst;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AppSettingsTable extends JBTable {

    protected final AppSettingModel appSettingModel = new AppSettingModel();
    protected AppServiceConfig config;
    @Getter
    protected boolean loading = false;

    public AppSettingsTable() {
        super();
        this.setModel(appSettingModel);
        this.setCellSelectionEnabled(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setAutoscrolls(true);
        this.setMinimumSize(new Dimension(-1, 150));
        this.setPreferredScrollableViewportSize(null);
        this.resetEmptyText();
    }

    public int addRequiredAppSettings(@Nonnull final String key, final String value) {
        return appSettingModel.addAppSettings(key, value);
    }

    public void addAppSettings() {
        final AddAppSettingsDialog dialog = new AddAppSettingsDialog(this);
        dialog.setOkActionListener(pair -> {
            this.addAppSettings(pair.getKey(), pair.getValue());
            dialog.close();
        });
        dialog.show();
    }

    public void addAppSettings(@Nonnull String key, String value) {
        final int index = appSettingModel.addAppSettings(key, value);
        this.refresh();
        scrollToRow(index);
    }

    public void addAppSettings(@Nullable final Map<String, String> appSettingMap) {
        if (MapUtils.isEmpty(appSettingMap)) {
            return;
        }
        appSettingMap.forEach((key, value) -> addAppSettings(key, value));
        this.refresh();
        scrollToRow(0);
    }

    public void removeAppSettings(int row) {
        appSettingModel.removeAppSettings(row);
        this.refresh();
    }

    public void removeAppSettings() {
        try {
            this.removeAppSettings(this.getSelectedRow());
        } catch (final IllegalArgumentException iae) {
            AzureMessager.getMessager().error(message("function.appSettings.remove.error.title"), iae.getMessage());
        }
    }

    public void removeAppSettings(@Nonnull String key) {
        final int row = appSettingModel.getAppSettingsRow(key);
        this.removeAppSettings(row);
    }

    public void loadAppSettings(@Nonnull Supplier<Map<String, String>> supplier) {
        this.setLoading(true);
        AzureTaskManager.getInstance().runInBackground("Loading application settings", () -> {
            try {
                final Map<String, String> appSettings = supplier.get();
                AzureTaskManager.getInstance().runLater(() -> AppSettingsTable.this.setAppSettings(appSettings), AzureTask.Modality.ANY);
            } finally {
                AzureTaskManager.getInstance().runLater(() -> AppSettingsTable.this.setLoading(false), AzureTask.Modality.ANY);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        this.loading = isLoading;
        this.setEnabled(!isLoading);
        if (isLoading) {
            this.clear();
            this.getEmptyText().setText(CommonConst.LOADING_TEXT);
        } else {
            this.resetEmptyText();
        }
    }

    private void resetEmptyText() {
        this.getEmptyText().setText("No app setting configured");
        this.getEmptyText().appendLine("New app setting", SimpleTextAttributes.LINK_ATTRIBUTES, ignore -> addAppSettings());
    }

    public void setAppSettings(@Nullable final Map<String, String> appSettingMap) {
        clear();
        if (MapUtils.isNotEmpty(appSettingMap)) {
            addAppSettings(appSettingMap);
        }
    }

    public void clear() {
        appSettingModel.clear();
        this.refresh();
    }

    public String getSelectedKey() {
        return appSettingModel.getAppSettingsKey(getSelectedRow());
    }

    @Nonnull
    public Map<String, String> getAppSettings() {
        return appSettingModel.getAppSettings();
    }

    public boolean isEmpty() {
        return appSettingModel.getRowCount() == 0;
    }

    private void scrollToRow(int target) {
        scrollRectToVisible(getCellRect(target, 0, true));
    }

    private void refresh() {
        this.setSize(-1, getRowHeight() * getRowCount());
        this.repaint();
    }
}