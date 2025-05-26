package com.microsoft.azure.toolkit.intellij.devops.auth.ui

import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import org.jetbrains.annotations.Nls
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel

@Suppress("UnstableApiUsage")
internal class AzureDevOpsChooseAccountDialog @JvmOverloads constructor(
    project: Project?,
    parentComponent: Component?,
    accounts: Collection<AzureDevOpsAccount>,
    showHosts: Boolean, allowDefault: Boolean,
    title: @Nls(capitalization = Nls.Capitalization.Title) String
    = "Choose Azure DevOps Organization",
    description: @Nls(capitalization = Nls.Capitalization.Sentence) String?
    = null,
    okText: @Nls(capitalization = Nls.Capitalization.Title) String
    = "Choose"
) : DialogWrapper(project, parentComponent, false, IdeModalityType.IDE) {

    private val description: JComponent? = description?.let { SimpleHtmlPane(it) }
    private val accountsList: JBList<AzureDevOpsAccount> = JBList(accounts).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : ColoredListCellRenderer<AzureDevOpsAccount>() {
            override fun customizeCellRenderer(
                list: JList<out AzureDevOpsAccount>,
                value: AzureDevOpsAccount,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                append(value.name)
                if (showHosts) {
                    append(" ")
                    append(value.server.toString(), SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                border = JBUI.Borders.empty(0, UIUtil.DEFAULT_HGAP)
            }
        }
    }
    private val setDefaultCheckBox: JBCheckBox? = if (allowDefault) JBCheckBox("Set as default") else null

    init {
        this.title = title
        setOKButtonText(okText)
        init()
        accountsList.selectedIndex = 0
    }

    override fun doValidate(): ValidationInfo? {
        return if (accountsList.selectedValue == null) ValidationInfo("Account not selected", accountsList)
        else null
    }

    val account: AzureDevOpsAccount get() = accountsList.selectedValue
    val setDefault: Boolean get() = setDefaultCheckBox?.isSelected ?: false

    override fun getPreferredFocusedComponent() = accountsList
    override fun createCenterPanel(): JComponent {
        return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
            .addToCenter(JBScrollPane(accountsList).apply {
                preferredSize = JBDimension(150, 20 * (accountsList.itemsCount + 1))
            })
            .apply { description?.run(::addToTop) }
            .apply { setDefaultCheckBox?.run(::addToBottom) }
    }
}
