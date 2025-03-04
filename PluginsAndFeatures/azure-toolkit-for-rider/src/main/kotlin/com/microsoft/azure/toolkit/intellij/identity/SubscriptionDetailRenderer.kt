/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.identity

import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import java.awt.Component
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class SubscriptionDetailRenderer: ListCellRenderer<Subscription> {
    private val titleLabel = SimpleColoredComponent()
    private val subscriptionIdLabel = SimpleColoredComponent()
    private val tenantIdLabel = SimpleColoredComponent()

    private val component = JPanel()
        .apply {
            background = UIUtil.getListBackground()
            border = BorderFactory.createEmptyBorder(0, JBUIScale.scale(UIUtil.getListCellHPadding()), 0, JBUIScale.scale(UIUtil.getListCellHPadding()))

            layout = GridLayout(3, 1)
            add(titleLabel)
            add(subscriptionIdLabel)
            add(tenantIdLabel)
        }

    override fun getListCellRendererComponent(
        list: JList<out Subscription?>?,
        value: Subscription?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        titleLabel.clear()
        subscriptionIdLabel.clear()
        tenantIdLabel.clear()

        if (value != null) {
            titleLabel.append(value.name)
            subscriptionIdLabel.append("Subscription id: ${value.id}", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
            tenantIdLabel.append("Tenant id: ${value.tenantId}", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
        }

        return component
    }
}