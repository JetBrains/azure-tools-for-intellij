package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.AnActionButton
import com.intellij.ui.HideableDecorator
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.nodes.isProject
import com.jetbrains.rider.projectView.nodes.isUnloadedProject
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

/**
 * The setting panel for web app deployment run configuration.
 *
 * TODO: refactor this, almost 1k lines as of 2018-09-06
 */
class RiderWebAppSettingPanel(project: Project,
                              private val configuration: RiderWebAppConfiguration)
    : AzureRiderSettingPanel<RiderWebAppConfiguration>(project), DotNetWebAppDeployMvpView {

    companion object {

        // const
        private const val WEB_APP_SETTINGS_PANEL_NAME = "Run On Web App"

        private const val RUN_CONFIG_PREFIX = "Azure Web App"

        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_APP_SERVICE_PLAN = "App Service Plan"
        private const val HEADER_OPERATING_SYSTEM = "Operating System"
        private const val HEADER_SQL_SERVER = "SQL Server"

        private const val WEB_APP_TABLE_COLUMN_SUBSCRIPTION = "Subscription"
        private const val WEB_APP_TABLE_COLUMN_NAME = "Name"
        private const val WEB_APP_TABLE_COLUMN_RESOURCE_GROUP = "Resource group"
        private const val WEB_APP_TABLE_COLUMN_LOCATION = "Location"
        private const val WEB_APP_TABLE_COLUMN_OS = "OS"
        private const val WEB_APP_TABLE_COLUMN_DOTNET_VERSION = ".Net Version"
        private const val WEB_APP_RUNTIME_MISMATCH_WARNING =
                "Selected Azure WebApp runtime '%s' mismatch with Project .Net Core Framework '%s'"

        private const val BUTTON_REFRESH_NAME = "Refresh"

        private const val NOT_APPLICABLE = "N/A"
        private const val TABLE_LOADING_MESSAGE = "Loading ... "
        private const val TABLE_EMPTY_MESSAGE = "No available Web Apps."
        private const val PROJECTS_EMPTY_MESSAGE = "No projects to publish"

        private const val DATABASES_EMPTY_MESSAGE = "No existing Azure SQL database"
        private const val SQL_SERVER_EMPTY_MESSAGE = "No existing Azure SQL server"

        private const val DEFAULT_APP_NAME = "webapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RGP_NAME = "rg-webapp-"

        private const val DEFAULT_SQL_DATABASE_NAME = "sql_%s_db"
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-db-"
        private const val DEFAULT_SQL_SERVER_NAME = "sql-server-"

        private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
        private val netAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

        private val warningIcon = com.intellij.icons.AllIcons.General.BalloonWarning
    }

    // presenter
    private val myView = DotNetWebAppDeployViewPresenter<RiderWebAppSettingPanel>()

    // cache variable
    private var lastSelectedProject: PublishableProjectModel? = null

    private var lastSelectedWebApp: ResourceEx<WebApp>? = null
    private var cachedWebAppList = listOf<ResourceEx<WebApp>>()

    private var lastSelectedSubscriptionId = ""
    private var lastSelectedResourceGroupName = ""
    private var lastSelectedOperatingSystem = AzureDotNetWebAppSettingModel.defaultOperatingSystem

    private var cachedAppServicePlan = listOf<AppServicePlan>()
    private var lastSelectedLocation = ""
    private var cachedPricingTier = listOf<PricingTier>()
    private var lastSelectedPriceTier = AzureDotNetWebAppSettingModel.defaultPricingTier

    private var lastSelectedDatabase: SqlDatabase? = null
    private var cachedResourceGroups = listOf<ResourceGroup>()
    private var lastSelectedDbResourceGroupName = ""
    private var lastSelectedSqlServer: SqlServer? = null
    private var lastSelectedDbLocation = ""
    private var lastSelectedDatabaseEdition = AzureDotNetWebAppSettingModel.defaultDatabaseEditions

    // Panels
    override var mainPanel: JPanel = pnlRoot
    private lateinit var pnlRoot: JPanel
    private lateinit var tpRoot: JBTabbedPane
    private lateinit var pnlWebAppConfigTab: JPanel
    private lateinit var pnlDbConnectionTab: JPanel

    private lateinit var pnlWebApp: JPanel
    private lateinit var pnlWebAppSelector: JPanel

    // Existing Web App
    private lateinit var rdoUseExistingWebApp: JRadioButton
    private lateinit var pnlExistingWebApp: JPanel

    private lateinit var table: JBTable
    private lateinit var btnRefresh: AnActionButton

    private lateinit var lblRuntimeMismatchWarning: JLabel

    // New Web App
    private lateinit var rdoCreateNewWebApp: JRadioButton
    private lateinit var pnlCreateWebApp: JPanel

    private lateinit var pnlWebAppTable: JPanel
    private lateinit var txtSelectedWebApp: JTextField

    private lateinit var txtWebAppName: JTextField
    private lateinit var cbSubscription: JComboBox<Subscription>

    // Resource Group
    private lateinit var pnlResourceGroupHolder: JPanel
    private lateinit var pnlResourceGroup: JPanel

    private lateinit var rdoUseExistResGrp: JRadioButton
    private lateinit var cbResourceGroup: JComboBox<ResourceGroup>

    private lateinit var rdoCreateResGrp: JRadioButton
    private lateinit var txtResourceGroupName: JTextField

    // Operating System
    private lateinit var pnlOperatingSystemHolder: JPanel
    private lateinit var pnlOperatingSystem: JPanel
    private lateinit var rdoOperatingSystemWindows: JRadioButton
    private lateinit var rdoOperatingSystemLinux: JRadioButton

    // App Service Plan
    private lateinit var pnlAppServicePlanHolder: JPanel
    private lateinit var pnlAppServicePlan: JPanel

    private lateinit var rdoCreateAppServicePlan: JRadioButton
    private lateinit var txtAppServicePlanName: JTextField
    private lateinit var cbLocation: JComboBox<Location>
    private lateinit var cbPricingTier: JComboBox<PricingTier>

    private lateinit var rdoUseExistAppServicePlan: JRadioButton
    private lateinit var cbAppServicePlan: JComboBox<AppServicePlan>
    private lateinit var lblLocation: JLabel
    private lateinit var lblPricingTier: JLabel

    // Project
    private lateinit var pnlProject: JPanel
    private lateinit var cbProject: JComboBox<PublishableProjectModel>

    // SQL Database
    private lateinit var pnlDbConnection: JPanel

    private lateinit var checkBoxEnableDbConnection: JCheckBox

    private lateinit var pnlDbConnectionSelector: JPanel
    private lateinit var rdoExistingDb: JRadioButton
    private lateinit var rdoNewDb: JRadioButton

    private lateinit var pnlDbConnectionString: JPanel
    private lateinit var txtConnectionStringName: JTextField

    // Existing Database
    private lateinit var pnlExistingDb: JPanel
    private lateinit var cbDatabase: JComboBox<SqlDatabase>
    private lateinit var lblSqlDbAdminLogin: JLabel
    private lateinit var passExistingDbAdminPassword: JBPasswordField

    // New Database
    private lateinit var pnlCreateNewDb: JPanel
    private lateinit var txtDbName: JTextField

    // Database Resource Group
    private lateinit var pnlDbResourceGroupHolder: JPanel
    private lateinit var pnlDbResourceGroup: JPanel
    private lateinit var rdoDbExistingResourceGroup: JRadioButton
    private lateinit var cbDbResourceGroup: JComboBox<ResourceGroup>
    private lateinit var rdoDbCreateResourceGroup: JRadioButton
    private lateinit var txtDbNewResourceGroup: JTextField

    // Database SQL Server
    private lateinit var pnlSqlServerHolder: JPanel
    private lateinit var pnlSqlServer: JPanel
    private lateinit var rdoUseExistSqlServer: JRadioButton
    private lateinit var cbExistSqlServer: JComboBox<SqlServer>
    private lateinit var lblExistingSqlServerAdminLogin: JLabel
    private lateinit var passExistingSqlServerAdminPassword: JBPasswordField
    private lateinit var lblExistingSqlServerLocation: JLabel

    private lateinit var rdoCreateSqlServer: JRadioButton
    private lateinit var txtNewSqlServerName: JTextField
    private lateinit var cbSqlServerLocation: JComboBox<Location>
    private lateinit var txtNewSqlServerAdminLogin: JTextField
    private lateinit var passNewSqlServerAdminPass: JBPasswordField
    private lateinit var passNewSqlServerAdminPassConfirm: JBPasswordField

    // Database Edition
    private lateinit var pnlDbEdition: JPanel
    private lateinit var cbDatabaseEdition: JComboBox<DatabaseEditions>

    // Database Collation
    private lateinit var pnlCollation: JPanel
    private lateinit var txtCollationValue: JTextField

    override val panelName: String
        get() = WEB_APP_SETTINGS_PANEL_NAME

    init {
        myView.onAttachView(this)

        updateAzureModelInBackground()

        initButtonGroupsState()
        initUIComponents(project)
    }

    //region Read From Config

    /**
     * Reset all controls from configuration.
     * Function is triggered while constructing the panel.
     *
     * @param configuration - Web App Configuration instance
     */
    public override fun resetFromConfig(configuration: RiderWebAppConfiguration) {
        val dateString = SimpleDateFormat("yyMMddHHmmss").format(Date())
        val model = configuration.model

        resetWebAppFromConfig(model, dateString)
        resetDatabaseFromConfig(model, dateString)

        myView.onLoadPublishableProjects(project)
        myView.onLoadSubscription()
        myView.onLoadWebApps()
        myView.onLoadPricingTier()
        myView.onLoadDatabaseEdition()
    }

    private fun resetWebAppFromConfig(model: AzureDotNetWebAppSettingModel, dateString: String) {
        cbProject.model.selectedItem = model.publishableProject

        txtWebAppName.text = if (model.webAppName.isEmpty()) "$DEFAULT_APP_NAME$dateString" else model.webAppName
        txtAppServicePlanName.text = if (model.appServicePlanName.isEmpty()) "$DEFAULT_PLAN_NAME$dateString" else model.appServicePlanName
        txtResourceGroupName.text = if (model.resourceGroupName.isEmpty()) "$DEFAULT_RGP_NAME$dateString" else model.resourceGroupName

        if (model.isCreatingWebApp) {
            rdoCreateNewWebApp.doClick()

            if (model.isCreatingResourceGroup) {
                rdoCreateResGrp.doClick()
            } else {
                rdoUseExistResGrp.doClick()
            }

            if (model.isCreatingAppServicePlan) {
                rdoCreateAppServicePlan.doClick()
            } else {
                rdoUseExistAppServicePlan.doClick()
            }

            if (model.operatingSystem == OperatingSystem.WINDOWS) {
                rdoOperatingSystemWindows.doClick()
            } else {
                rdoOperatingSystemLinux.doClick()
            }
        } else {
            rdoUseExistingWebApp.doClick()
        }

        btnRefresh.isEnabled = false
    }

    private fun resetDatabaseFromConfig(model: AzureDotNetWebAppSettingModel, dateString: String) {
        checkBoxEnableDbConnection.isSelected = model.isDatabaseConnectionEnabled
        cbDatabase.model.selectedItem = model.database

        txtDbName.text = if (model.databaseName.isEmpty()) String.format(DEFAULT_SQL_DATABASE_NAME, dateString) else model.databaseName
        txtDbNewResourceGroup.text = if (model.dbResourceGroupName.isEmpty()) "$DEFAULT_RESOURCE_GROUP_NAME$dateString" else model.dbResourceGroupName
        txtNewSqlServerName.text = if (model.sqlServerName.isEmpty()) "$DEFAULT_SQL_SERVER_NAME$dateString" else model.sqlServerName

        if (model.isCreatingDbResourceGroup) {
            rdoDbCreateResourceGroup.doClick()
        } else {
            rdoDbExistingResourceGroup.doClick()
        }

        if (model.isCreatingSqlServer) {
            rdoCreateSqlServer.doClick()
        } else {
            rdoUseExistSqlServer.doClick()
        }

        txtCollationValue.text = model.collation
        txtConnectionStringName.text = model.connectionStringName
    }

    /**
     * Function is triggered by any content change events.
     *
     * @param configuration configuration instance
     */
    override fun apply(configuration: RiderWebAppConfiguration) {
        applyWebAppConfig(configuration.model)
        applyDatabaseConfig(configuration.model)
    }

    private fun applyWebAppConfig(model: AzureDotNetWebAppSettingModel) {
        model.publishableProject = getSelectedItem(cbProject) ?: model.publishableProject

        model.isCreatingWebApp = rdoCreateNewWebApp.isSelected
        if (rdoCreateNewWebApp.isSelected) {
            model.webAppName = txtWebAppName.text
            model.subscriptionId = getSelectedItem(cbSubscription)?.subscriptionId() ?: ""

            model.isCreatingResourceGroup = rdoCreateResGrp.isSelected
            if (rdoCreateResGrp.isSelected) {
                model.resourceGroupName = txtResourceGroupName.text
            } else {
                model.resourceGroupName = lastSelectedResourceGroupName
            }

            if (rdoOperatingSystemWindows.isSelected) {
                model.operatingSystem = OperatingSystem.WINDOWS
                // Set .Net Framework version based on project config
                val publishableProject = getSelectedItem(cbProject) ?: model.publishableProject
                if (publishableProject != null && !publishableProject.isDotNetCore) {
                    model.netFrameworkVersion =
                            if (getProjectNetFrameworkVersion(publishableProject).startsWith("4")) NetFrameworkVersion.fromString("4.7")
                            else NetFrameworkVersion.fromString("3.5")
                }
            } else {
                model.operatingSystem = OperatingSystem.LINUX
                // Set Net Core runtime based on project config
                val publishableProject = lastSelectedProject
                if (publishableProject != null && publishableProject.isDotNetCore) {
                    val netCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
                    model.netCoreRuntime = RuntimeStack("DOTNETCORE", netCoreVersion)
                }
            }

            model.isCreatingAppServicePlan = rdoCreateAppServicePlan.isSelected
            if (rdoCreateAppServicePlan.isSelected) {
                model.appServicePlanName = txtAppServicePlanName.text
                model.location = getSelectedItem(cbLocation)?.name() ?: model.location
                model.pricingTier = getSelectedItem(cbPricingTier) ?: model.pricingTier
            } else {
                model.appServicePlanId = getSelectedItem(cbAppServicePlan)?.id() ?: model.appServicePlanId
            }
        } else {
            model.subscriptionId = getSelectedItem(cbSubscription)?.subscriptionId() ?: ""

            val selectedWebApp = lastSelectedWebApp?.resource
            model.webAppId = selectedWebApp?.id() ?: ""
            model.operatingSystem = selectedWebApp?.operatingSystem() ?: OperatingSystem.WINDOWS
            if (model.operatingSystem == OperatingSystem.LINUX) {
                val dotNetCoreVersionArray = selectedWebApp?.linuxFxVersion()?.split('|')
                val netCoreRuntime =
                        if (dotNetCoreVersionArray != null && dotNetCoreVersionArray.size == 2) RuntimeStack(dotNetCoreVersionArray[0], dotNetCoreVersionArray[1])
                        else AzureDotNetWebAppSettingModel.defaultRuntime
                model.netCoreRuntime = netCoreRuntime
            }
        }
    }

    private fun applyDatabaseConfig(model: AzureDotNetWebAppSettingModel) {
        model.isDatabaseConnectionEnabled = checkBoxEnableDbConnection.isSelected
        if (checkBoxEnableDbConnection.isSelected) {
            model.connectionStringName = txtConnectionStringName.text

            model.isCreatingSqlDatabase = rdoNewDb.isSelected
            if (rdoNewDb.isSelected) {
                model.databaseName = txtDbName.text

                model.isCreatingDbResourceGroup = rdoDbCreateResourceGroup.isSelected
                if (rdoDbCreateResourceGroup.isSelected) {
                    model.dbResourceGroupName = txtDbNewResourceGroup.text
                } else {
                    model.dbResourceGroupName = getSelectedItem(cbDbResourceGroup)?.name() ?: ""
                }

                model.isCreatingSqlServer = rdoCreateSqlServer.isSelected
                if (rdoCreateSqlServer.isSelected) {
                    model.sqlServerName = txtNewSqlServerName.text
                    model.sqlServerLocation = getSelectedItem(cbSqlServerLocation)?.name() ?: model.sqlServerLocation
                    model.sqlServerAdminLogin = txtNewSqlServerAdminLogin.text
                    model.sqlServerAdminPassword = passNewSqlServerAdminPass.password
                    model.sqlServerAdminPasswordConfirm = passNewSqlServerAdminPassConfirm.password
                } else {
                    model.sqlServerId = getSelectedItem(cbExistSqlServer)?.id() ?: ""
                    model.sqlServerAdminLogin = lblExistingSqlServerAdminLogin.text
                    model.sqlServerAdminPassword = passExistingSqlServerAdminPassword.password
                }

                model.databaseEdition = getSelectedItem(cbDatabaseEdition) ?: model.databaseEdition
                model.collation = txtCollationValue.text
            } else {
                model.database = getSelectedItem(cbDatabase) ?: model.database
                model.sqlServerAdminLogin = lblSqlDbAdminLogin.text
                model.sqlServerAdminPassword = passExistingDbAdminPassword.password
            }
        }
    }

    //endregion Read From Config

    //region MVP View

    /**
     * Render table with existing Web apps to publish
     *
     * @param webAppLists - list of web apps to render
     */
    override fun renderWebAppsTable(webAppLists: List<ResourceEx<WebApp>>) {
        btnRefresh.isEnabled = true
        table.emptyText.text = TABLE_EMPTY_MESSAGE

        val sortedWebApps = webAppLists.sortedWith(compareBy (
                { it.resource.operatingSystem() },
                { it.subscriptionId },
                { it.resource.resourceGroupName() }))

        cachedWebAppList = sortedWebApps

        if (sortedWebApps.isEmpty()) return

        collectConnectionStringsInBackground(webAppLists.map { it.resource })

        val model = table.model as DefaultTableModel
        model.dataVector.clear()

        val subscriptionManager = AuthMethodManager.getInstance().azureManager.subscriptionManager

        for (i in sortedWebApps.indices) {
            val webApp = sortedWebApps[i].resource
            val subscription = subscriptionManager.subscriptionIdToSubscriptionMap[webApp.manager().subscriptionId()] ?: continue

            model.addRow(arrayOf(
                    webApp.name(),
                    webApp.resourceGroupName(),
                    webApp.region().label(),
                    webApp.operatingSystem().name.toLowerCase().capitalize(),
                    mapWebAppFrameworkVersion(webApp),
                    subscription.displayName())
            )

            if (webApp.id() == configuration.model.webAppId ||
                    (lastSelectedWebApp != null && lastSelectedWebApp?.resource?.id() == webApp.id())) {
                table.setRowSelectionInterval(i, i)
            }
        }
    }

    override fun fillSubscription(subscriptions: List<Subscription>) {
        cbSubscription.removeAllItems()
        subscriptions.forEach {
            cbSubscription.addItem(it)
            if (it.subscriptionId() == configuration.subscriptionId) {
                cbSubscription.selectedItem = it
            }
        }

        if (subscriptions.isEmpty()) { lastSelectedSubscriptionId = "" }
        setComponentsEnabled(checkCbSubscriptionRule(), cbSubscription)
    }

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        cachedResourceGroups = resourceGroups

        cbResourceGroup.removeAllItems()
        resourceGroups.sortedWith(compareBy { it.name() })
                .forEach {
                    cbResourceGroup.addItem(it)
                    if (it.name() == configuration.model.resourceGroupName) {
                        cbResourceGroup.selectedItem = it
                    }

                    cbDbResourceGroup.addItem(it)
                    if (it.name() == configuration.model.dbResourceGroupName) {
                        cbDbResourceGroup.selectedItem = it
                    }
                }

        if (resourceGroups.isEmpty()) {
            toggleResourceGroupPanel(true)
            toggleDbResourceGroupPanel(true)
            lastSelectedResourceGroupName = ""
        }

        setComponentsEnabled(checkCbResourceGroupRule(), cbResourceGroup)
        setComponentsEnabled(checkCbDbResourceGroupRule(), cbDbResourceGroup)
    }

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) {
        cachedAppServicePlan = appServicePlans

        val filteredPlans = filterAppServicePlans(lastSelectedOperatingSystem, appServicePlans)
        setAppServicePlanContent(filteredPlans)

        if (appServicePlans.isEmpty()) {
            toggleAppServicePlanPanel(true)
            lblLocation.text = NOT_APPLICABLE
            lblPricingTier.text = NOT_APPLICABLE
        }

        setComponentsEnabled(checkCbAppServicePlanRule(), cbAppServicePlan)
    }

    override fun fillLocation(locations: List<Location>) {
        cbLocation.removeAllItems()

        locations.sortedWith(compareBy { it.displayName() })
                .forEach {
                    cbLocation.addItem(it)
                    if (it.name() == configuration.model.location) {
                        cbLocation.selectedItem = it
                    }

                    cbSqlServerLocation.addItem(it)
                    if (it.name() == configuration.model.sqlServerLocation) {
                        cbSqlServerLocation.selectedItem = it
                    }
                }

        if (locations.isEmpty()) {
            lastSelectedLocation = ""
        }
    }

    override fun fillPricingTier(prices: List<PricingTier>) {
        cachedPricingTier = prices
        val filteredPrices = filterPricingTiers(lastSelectedOperatingSystem, prices)
        setPricingTierContent(filteredPrices)
    }

    override fun fillSqlDatabase(databases: List<SqlDatabase>) {
        cbDatabase.removeAllItems()
        databases.sortedBy { it.name() }
                .forEach {
                    cbDatabase.addItem(it)
                    if (it == configuration.model.database) {
                        cbDatabase.selectedItem = it
                    }
                }

        if (databases.isEmpty()) {
            lastSelectedDatabase = null
        }

        setComponentsEnabled(checkCbDatabaseRule(), cbDatabase, passExistingDbAdminPassword)
    }

    override fun fillDatabaseEdition(prices: List<DatabaseEditions>) {
        cbDatabaseEdition.removeAllItems()
        prices.forEach {
            cbDatabaseEdition.addItem(it)
            if (it == configuration.model.databaseEdition) {
                cbDatabaseEdition.selectedItem = it
            }
        }
    }

    override fun fillSqlServer(sqlServers: List<SqlServer>) {
        cbExistSqlServer.removeAllItems()
        sqlServers.sortedWith(compareBy { it.name() })
                .forEach {
                    cbExistSqlServer.addItem(it)
                    if (it.name() == configuration.model.sqlServerName) {
                        cbExistSqlServer.selectedItem = it
                        lastSelectedSqlServer = it
                    }
                }

        if (sqlServers.isEmpty()) {
            toggleSqlServerPanel(true)
            lastSelectedSqlServer = null
        }

        setComponentsEnabled(checkCbExistSqlServerRule(), cbExistSqlServer)
    }

    override fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        cbProject.removeAllItems()

        val filteredProjects = filterProjects(publishableProjects)
        filteredProjects.sortedBy { it.projectName }
                .forEach {
                    cbProject.addItem(it)
                    if (it == configuration.model.publishableProject) {
                        cbProject.selectedItem = it
                        lastSelectedProject = it
                        setOperatingSystemRadioButtons(it)
                    }
                }
    }

    /**
     * Set the App Service Plan Combo Box content
     *
     * @param appServicePlans list of App Service Plans to display
     */
    private fun setAppServicePlanContent(appServicePlans: List<AppServicePlan>) {
        cbAppServicePlan.removeAllItems()

        appServicePlans
                .sortedWith(compareBy({ it.operatingSystem() }, { it.name() }))
                .forEach {
                    cbAppServicePlan.addItem(it)
                    if (it.id() == configuration.model.appServicePlanId) {
                        cbAppServicePlan.selectedItem = it
                    }
                }
    }

    /**
     * Set the Pricing Tier Combo Box content
     *
     * @param pricingTiers list of Pricing Tiers to display
     */
    private fun setPricingTierContent(pricingTiers: List<PricingTier>) {
        cbPricingTier.removeAllItems()
        pricingTiers.forEach {
            cbPricingTier.addItem(it)
            if (it == configuration.model.pricingTier) {
                cbPricingTier.selectedItem = it
            }
        }
    }

    /**
     * Let the presenter release the view. Will be called by:
     * [com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppSettingEditor.disposeEditor].
     */
    override fun disposeEditor() {
        myView.onDetachView()
    }

    //endregion MVP View

    //region Filters

    /**
     * Filter all non-core apps or non-web apps on full .Net framework
     *
     * @param publishableProjects list of all available publishable projects
     */
    private fun filterProjects(publishableProjects: Collection<PublishableProjectModel>): List<PublishableProjectModel> {
        return publishableProjects.filter { it.isWeb && (it.isDotNetCore || SystemInfo.isWindows) }
    }

    /**
     * Filter App Service Plans to Operating System related values
     */
    private fun filterAppServicePlans(operatingSystem: OperatingSystem,
                                      appServicePlans: List<AppServicePlan>): List<AppServicePlan> {
        return appServicePlans.filter { it.operatingSystem() == operatingSystem }
    }

    /**
     * Filter Pricing Tier based on Operating System
     *
     * Note: We should hide "Free" and "Shared" Pricing Tiers for Linux OS
     */
    private fun filterPricingTiers(operatingSystem: OperatingSystem,
                                   prices: List<PricingTier>): List<PricingTier> {
        if (operatingSystem == OperatingSystem.WINDOWS) return prices
        return prices.filter { it != PricingTier.FREE_F1 && it != PricingTier.SHARED_D1 }
    }

    /**
     * Filter Web App table content based on selected project.
     * Here we should filter all Linux instances for selected project with full .Net Target Framework
     *
     * @param publishableProject a selected project
     */
    private fun filterWebAppTableContent(publishableProject: PublishableProjectModel) {
        val sorter = table.rowSorter as? TableRowSorter<*> ?: return

        if (publishableProject.isDotNetCore) {
            sorter.rowFilter = null
            return
        }

        val osColumnIndex = (table.model as DefaultTableModel).findColumn(WEB_APP_TABLE_COLUMN_OS)
        assert(osColumnIndex >= 0)
        sorter.rowFilter = javax.swing.RowFilter.regexFilter(OperatingSystem.WINDOWS.name.toLowerCase().capitalize(), osColumnIndex)
    }

    //endregion Filters

    //region Editing Web App Table

    private fun createUIComponents() {
        initWebAppTable()
    }

    private fun initWebAppTable() {
        initWebAppTableModel()
        initRefreshButton()

        val tableToolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraActions(btnRefresh).setToolbarPosition(ActionToolbarPosition.TOP)

        pnlWebAppTable = tableToolbarDecorator.createPanel()
    }

    private fun initWebAppTableModel() {
        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int) = false
        }

        tableModel.addColumn(WEB_APP_TABLE_COLUMN_NAME)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_RESOURCE_GROUP)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_LOCATION)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_OS)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_DOTNET_VERSION)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_SUBSCRIPTION)

        table = JBTable(tableModel)
        table.emptyText.text = TABLE_LOADING_MESSAGE
        table.rowSelectionAllowed = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.rowSorter = TableRowSorter<TableModel>(table.model)

        table.selectionModel.addListSelectionListener { event ->
            if (event.valueIsAdjusting) {
                return@addListSelectionListener
            }

            val selectedRow = table.selectedRow
            if (cachedWebAppList.isEmpty() || selectedRow < 0 || selectedRow >= cachedWebAppList.size) {
                lastSelectedWebApp = null
                txtSelectedWebApp.text = ""
                return@addListSelectionListener
            }

            val webApp = cachedWebAppList[selectedRow]
            lastSelectedWebApp = webApp

            // This is not visible on the UI, but is used to preform a re-validation over selected web app from the table
            txtSelectedWebApp.text = webApp.resource.name()

            val publishableProject = getSelectedItem(cbProject) ?: return@addListSelectionListener
            checkSelectedProjectAgainstWebAppRuntime(webApp.resource, publishableProject)
        }
    }

    private fun initRefreshButton() {
        btnRefresh = object : AnActionButton(BUTTON_REFRESH_NAME, AllIcons.Actions.Refresh) {
            override fun actionPerformed(anActionEvent: AnActionEvent) {
                resetWidget()
                myView.onRefresh()
            }
        }
    }

    private fun resetWidget() {
        btnRefresh.isEnabled = false
        val model = table.model as DefaultTableModel
        model.dataVector.clear()
        model.fireTableDataChanged()
        table.emptyText.text = TABLE_LOADING_MESSAGE
        txtSelectedWebApp.text = ""
        setRuntimeMismatchWarning(false)
    }

    private fun setRuntimeMismatchWarning(show: Boolean, message: String = "") {
        lblRuntimeMismatchWarning.text = message
        lblRuntimeMismatchWarning.isVisible = show
    }

    private fun checkSelectedProjectAgainstWebAppRuntime(webApp: WebApp, publishableProject: PublishableProjectModel) {
        if (webApp.operatingSystem() == OperatingSystem.WINDOWS) {
            setRuntimeMismatchWarning(false)
            return
        }

        // DOTNETCORE|2.0 -> 2.0
        val webAppFrameworkVersion = webApp.linuxFxVersion().split('|').getOrNull(1)

        // .NETCoreApp,Version=v2.0 -> 2.0
        val projectNetCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
        setRuntimeMismatchWarning(
                webAppFrameworkVersion != projectNetCoreVersion,
                String.format(WEB_APP_RUNTIME_MISMATCH_WARNING, webAppFrameworkVersion, projectNetCoreVersion))
    }

    //endregion Editing Web App Table

    //region Behavior

    private fun initButtonGroupsState() {
        initWebAppDeployButtonGroup()
        initResourceGroupButtonGroup()
        initOperatingSystemButtonGroup()
        initAppServicePlanButtonsGroup()

        initDatabaseDeployButtonGroup()
        initDbResourceGroupButtonGroup()
        initSqlServerButtonsGroup()
    }

    private fun initWebAppDeployButtonGroup() {
        val webAppDeployButtons = ButtonGroup()
        webAppDeployButtons.add(rdoUseExistingWebApp)
        webAppDeployButtons.add(rdoCreateNewWebApp)
        rdoCreateNewWebApp.addActionListener { toggleWebAppPanel(true) }
        rdoUseExistingWebApp.addActionListener { toggleWebAppPanel(false) }
        toggleWebAppPanel(configuration.model.isCreatingWebApp)
    }

    private fun initResourceGroupButtonGroup() {
        val resourceGroupButtons = ButtonGroup()
        resourceGroupButtons.add(rdoUseExistResGrp)
        resourceGroupButtons.add(rdoCreateResGrp)
        rdoCreateResGrp.addActionListener { toggleResourceGroupPanel(true) }
        rdoUseExistResGrp.addActionListener { toggleResourceGroupPanel(false) }
        toggleResourceGroupPanel(configuration.model.isCreatingResourceGroup)
    }

    private fun initOperatingSystemButtonGroup() {
        val operatingSystemButtons = ButtonGroup()
        operatingSystemButtons.add(rdoOperatingSystemWindows)
        operatingSystemButtons.add(rdoOperatingSystemLinux)
        rdoOperatingSystemWindows.addActionListener { toggleOperatingSystem(OperatingSystem.WINDOWS) }
        rdoOperatingSystemLinux.addActionListener { toggleOperatingSystem(OperatingSystem.LINUX) }
        toggleOperatingSystem(configuration.model.operatingSystem)
    }

    private fun initAppServicePlanButtonsGroup() {
        val appServicePlanButtons = ButtonGroup()
        appServicePlanButtons.add(rdoUseExistAppServicePlan)
        appServicePlanButtons.add(rdoCreateAppServicePlan)
        rdoCreateAppServicePlan.addActionListener { toggleAppServicePlanPanel(true) }
        rdoUseExistAppServicePlan.addActionListener { toggleAppServicePlanPanel(false) }
        toggleAppServicePlanPanel(configuration.model.isCreatingAppServicePlan)
    }

    private fun initDatabaseDeployButtonGroup() {
        val databaseDeployButtons = ButtonGroup()
        databaseDeployButtons.add(rdoNewDb)
        databaseDeployButtons.add(rdoExistingDb)
        rdoNewDb.addActionListener { toggleDatabasePanel(true) }
        rdoExistingDb.addActionListener { toggleDatabasePanel(false) }
        toggleDatabasePanel(configuration.model.isCreatingSqlDatabase)
    }

    private fun initDbResourceGroupButtonGroup() {
        val btnGroupResourceGroup = ButtonGroup()
        btnGroupResourceGroup.add(rdoDbCreateResourceGroup)
        btnGroupResourceGroup.add(rdoDbExistingResourceGroup)

        rdoDbCreateResourceGroup.addActionListener {
            toggleDbResourceGroupPanel(true)
            rdoCreateSqlServer.doClick()
        }

        rdoDbExistingResourceGroup.addActionListener {
            if (rdoUseExistSqlServer.isSelected) return@addActionListener
            toggleDbResourceGroupPanel(false)
        }

        toggleDbResourceGroupPanel(configuration.model.isCreatingDbResourceGroup)
    }

    /**
     * Button groups - SQL Server
     *
     * Note: Existing SQL Server is already defined in some Resource Group. User has no option to choose
     *       a Resource Group if he selecting from existing SQL Servers.
     */
    private fun initSqlServerButtonsGroup() {
        val btnGroupSqlServer = ButtonGroup()
        btnGroupSqlServer.add(rdoCreateSqlServer)
        btnGroupSqlServer.add(rdoUseExistSqlServer)

        rdoCreateSqlServer.addActionListener {
            toggleSqlServerPanel(true)
            toggleDbResourceGroupPanel(rdoDbCreateResourceGroup.isSelected)
        }

        rdoUseExistSqlServer.addActionListener {
            toggleSqlServerPanel(false)

            val sqlServer = lastSelectedSqlServer
            if (sqlServer != null) toggleSqlServerComboBox(sqlServer)

            rdoDbExistingResourceGroup.doClick()
            toggleDbResourceGroupPanel(false)
            cbDbResourceGroup.isEnabled = false // Disable ability to select resource group - show related to SQL Server instead
        }

        toggleSqlServerPanel(configuration.model.isCreatingSqlServer)
    }

    /**
     * Set SQL Server elements visibility when using existing or creating new SQL Server
     *
     * @param isCreatingNew - flag indicating whether we create new SQL Server or use and existing one
     */
    private fun toggleSqlServerPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtNewSqlServerName, txtNewSqlServerAdminLogin,
                passNewSqlServerAdminPass, passNewSqlServerAdminPassConfirm, cbSqlServerLocation)

        setComponentsEnabled(!isCreatingNew, cbExistSqlServer,
                lblExistingSqlServerLocation, lblExistingSqlServerAdminLogin, passExistingSqlServerAdminPassword)
    }

    private fun toggleSqlServerComboBox(selectedSqlServer: SqlServer) {
        val resourceGroupToSet =
                cachedResourceGroups.find { it.name() == selectedSqlServer.resourceGroupName() } ?: return

        cbDbResourceGroup.selectedItem = resourceGroupToSet
    }

    private fun initDbConnectionEnableCheckbox() {
        checkBoxEnableDbConnection.addActionListener { setDbConnectionPanel(checkBoxEnableDbConnection.isSelected) }
        setDbConnectionPanel(configuration.model.isDatabaseConnectionEnabled)
    }

    private fun toggleWebAppPanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateWebApp)
        setComponentsVisible(!isCreatingNew, pnlExistingWebApp)
    }

    private fun toggleDatabasePanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateNewDb)
        setComponentsVisible(!isCreatingNew, pnlExistingDb)
    }

    private fun toggleResourceGroupPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtResourceGroupName)
        setComponentsEnabled(!isCreatingNew, cbResourceGroup)
    }

    private fun toggleDbResourceGroupPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtDbNewResourceGroup)
        setComponentsEnabled(!isCreatingNew, cbDbResourceGroup)
    }

    private fun toggleAppServicePlanPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtAppServicePlanName, cbLocation, cbPricingTier)
        setComponentsEnabled(!isCreatingNew, cbAppServicePlan, lblLocation, lblPricingTier)
    }

    private fun toggleOperatingSystem(operatingSystem: OperatingSystem) {
        setAppServicePlanContent(filterAppServicePlans(operatingSystem, cachedAppServicePlan))
        setPricingTierContent(filterPricingTiers(operatingSystem, cachedPricingTier))
    }

    private fun setDbConnectionPanel(isEnabled: Boolean) {
        setDbConnectionSelectorPanel(isEnabled)
        setDbConnectionStringPanel(isEnabled)
        setExistingDbPanel(isEnabled)
        setCreateNewDbPanel(isEnabled)
    }

    private fun setDbConnectionSelectorPanel(isEnabled: Boolean) {
        setComponentsEnabled(isEnabled, rdoExistingDb, rdoNewDb)
    }

    private fun setDbConnectionStringPanel(isEnabled: Boolean) {
        setComponentsEnabled(isEnabled, txtConnectionStringName)
    }

    private fun setExistingDbPanel(isEnabled: Boolean) {
        setComponentsEnabled(isEnabled && checkCbDatabaseRule(), cbDatabase, passExistingDbAdminPassword)
    }

    private fun setCreateNewDbPanel(isEnabled: Boolean) {
        setComponentsEnabled(isEnabled,
                txtDbName, rdoDbExistingResourceGroup, cbDbResourceGroup, rdoDbCreateResourceGroup,
                txtDbNewResourceGroup, rdoUseExistSqlServer, cbExistSqlServer, rdoCreateSqlServer, txtNewSqlServerName,
                cbSqlServerLocation, txtNewSqlServerAdminLogin, passNewSqlServerAdminPass, passNewSqlServerAdminPassConfirm,
                cbDatabaseEdition, txtCollationValue)

        if (isEnabled) {
            toggleDbResourceGroupPanel(rdoDbCreateResourceGroup.isSelected)
            toggleSqlServerPanel(rdoCreateSqlServer.isSelected)
            if (rdoUseExistSqlServer.isSelected) rdoUseExistSqlServer.doClick() // Perform a click to force resource group setup logic
        }
    }

    private fun setOperatingSystemRadioButtons(publishableProject: PublishableProjectModel) {
        val isDotNetCore = publishableProject.isDotNetCore
        setComponentsEnabled(isDotNetCore, rdoOperatingSystemLinux)

        if (!isDotNetCore) {
            rdoOperatingSystemWindows.doClick()
            toggleOperatingSystem(OperatingSystem.WINDOWS)
        }
    }

    private fun checkCbSubscriptionRule(): Boolean {
        if (cbSubscription.model.size == 0) return false
        return true
    }

    private fun checkCbResourceGroupRule(): Boolean {
        if (rdoCreateResGrp.isSelected ||
                cbResourceGroup.model.size == 0) return false

        return true
    }

    private fun checkCbAppServicePlanRule(): Boolean {
        if (rdoCreateAppServicePlan.isSelected ||
                cbAppServicePlan.model.size == 0) return false

        return true
    }

    private fun checkCbDatabaseRule(): Boolean {
        if (!checkBoxEnableDbConnection.isSelected ||
                cbDatabase.model.size == 0) return false

        return true
    }

    private fun checkCbDbResourceGroupRule(): Boolean {
        if (!checkBoxEnableDbConnection.isSelected ||
                rdoDbCreateResourceGroup.isSelected ||
                rdoUseExistSqlServer.isSelected ||
                cbDbResourceGroup.model.size == 0) return false

        return true
    }

    private fun checkCbExistSqlServerRule(): Boolean {
        if (!checkBoxEnableDbConnection.isSelected ||
                rdoCreateSqlServer.isSelected ||
                rdoDbCreateResourceGroup.isSelected ||
                cbExistSqlServer.model.size == 0) return false

        return true
    }

    //endregion Behavior

    //region Initialize UI Components

    /**
     * Configure renderer and listeners for all UI Components
     */
    private fun initUIComponents(project: Project) {
        initRuntimeMismatchWarningLabel()

        initSubscriptionComboBox()
        initResourceGroupComboBox()

        initAppServicePlanComboBox()
        initLocationComboBox()
        initPricingTierComboBox()

        initProjectsComboBox(project)

        initDbConnectionEnableCheckbox()
        initSqlDatabaseComboBox()
        initSqlServerComboBox()
        initDatabaseEditionComboBox()

        setHeaderDecorators()
    }

    private fun initSubscriptionComboBox() {
        cbSubscription.renderer = object : ListCellRendererWrapper<Subscription>() {
            override fun customize(list: JList<*>, subscription: Subscription?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                subscription ?: return
                setText(subscription.displayName())
            }
        }

        cbSubscription.addActionListener {
            val subscription = getSelectedItem(cbSubscription) ?: return@addActionListener
            val selectedSid = subscription.subscriptionId()
            if (lastSelectedSubscriptionId != selectedSid) {
                resetSubscriptionComboBoxValues()

                myView.onLoadResourceGroups(selectedSid)
                myView.onLoadLocation(selectedSid)
                myView.onLoadAppServicePlan(selectedSid)
                myView.onLoadSqlServers(selectedSid)
                myView.onLoadSqlDatabase(selectedSid)

                lastSelectedSubscriptionId = selectedSid
            }
        }
    }

    private fun initResourceGroupComboBox() {

        val renderer = object : ListCellRendererWrapper<ResourceGroup>() {
            override fun customize(list: JList<*>, resourceGroup: ResourceGroup?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                resourceGroup ?: return
                setText(resourceGroup.name())
            }
        }

        cbResourceGroup.renderer = renderer

        cbResourceGroup.addActionListener {
            lastSelectedResourceGroupName = getSelectedItem(cbResourceGroup)?.name() ?: return@addActionListener
        }

        cbDbResourceGroup.renderer = renderer

        cbDbResourceGroup.addActionListener {
            lastSelectedDbResourceGroupName = getSelectedItem(cbDbResourceGroup)?.name() ?: return@addActionListener
        }
    }

    private fun initAppServicePlanComboBox() {
        cbAppServicePlan.renderer = object : ListCellRendererWrapper<AppServicePlan>() {
            override fun customize(list: JList<*>, appServicePlan: AppServicePlan?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                appServicePlan ?: return
                setText(appServicePlan.name())
            }
        }

        cbAppServicePlan.addActionListener {
            val plan = getSelectedItem(cbAppServicePlan) ?: return@addActionListener
            lblLocation.text = plan.regionName()
            val pricingTier = plan.pricingTier()
            val skuDescription = pricingTier.toSkuDescription()
            lblPricingTier.text = "${skuDescription.name()} (${skuDescription.tier()})"
        }
    }

    private fun initLocationComboBox() {
        val renderer = object : ListCellRendererWrapper<Location>() {
            override fun customize(list: JList<*>, location: Location?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                if (location != null) {
                    setText(location.displayName())
                }
            }
        }

        cbLocation.renderer = renderer

        cbLocation.addActionListener {
            lastSelectedLocation = getSelectedItem(cbLocation)?.name() ?: return@addActionListener
        }

        cbSqlServerLocation.renderer = renderer

        cbSqlServerLocation.addActionListener {
            lastSelectedDbLocation = getSelectedItem(cbSqlServerLocation)?.name() ?: return@addActionListener
        }
    }

    private fun initPricingTierComboBox() {
        cbPricingTier.renderer = object : ListCellRendererWrapper<PricingTier>() {
            override fun customize(list: JList<*>?, pricingTier: PricingTier?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                pricingTier ?: return
                val skuDescription = pricingTier.toSkuDescription()
                setText("${skuDescription.name()} (${skuDescription.tier()})")
            }
        }

        cbPricingTier.addActionListener {
            val pricingTier = getSelectedItem(cbPricingTier) ?: return@addActionListener
            lastSelectedPriceTier = pricingTier
        }
    }

    private fun initProjectsComboBox(project: Project) {

        cbProject.renderer = object : ListCellRendererWrapper<PublishableProjectModel>() {
            override fun customize(list: JList<*>,
                                   publishableProject: PublishableProjectModel?,
                                   index: Int,
                                   isSelected: Boolean,
                                   cellHasFocus: Boolean) {
                if (project.isDisposed) return

                // Text
                if (publishableProject == null) {
                    setText(PROJECTS_EMPTY_MESSAGE)
                    return
                }

                setText(publishableProject.projectName)

                // Icon
                val projectVf = VfsUtil.findFileByIoFile(File(publishableProject.projectFilePath), false) ?: return

                val projectArray = ProjectModelViewHost.getInstance(project).getItemsByVirtualFile(projectVf)
                val projectNodes = projectArray.filter { it.isProject() || it.isUnloadedProject() }

                if (!projectNodes.isEmpty()) {
                    setIcon(projectNodes[0].getIcon())
                }
            }
        }

        cbProject.addActionListener {
            val publishableProject = getSelectedItem(cbProject) ?: return@addActionListener
            if (publishableProject == lastSelectedProject) return@addActionListener

            setOperatingSystemRadioButtons(publishableProject)

            filterWebAppTableContent(publishableProject)

            val webApp = lastSelectedWebApp?.resource
            if (webApp != null) checkSelectedProjectAgainstWebAppRuntime(webApp, publishableProject)

            lastSelectedProject = publishableProject
            setConfigurationName("$RUN_CONFIG_PREFIX: ${publishableProject.projectName}")
        }
    }

    private fun initSqlDatabaseComboBox() {
        cbDatabase.renderer = object : ListCellRendererWrapper<SqlDatabase>() {
            override fun customize(list: JList<*>?, sqlDatabase: SqlDatabase?, index: Int, selected: Boolean, hasFocus: Boolean) {
                if (sqlDatabase == null) {
                    setText(DATABASES_EMPTY_MESSAGE)
                    return
                }

                setText("${sqlDatabase.name()} (${sqlDatabase.resourceGroupName()})")
                setIcon(IconLoader.getIcon("icons/Database.svg"))
            }
        }

        cbDatabase.addActionListener {
            val database = getSelectedItem(cbDatabase) ?: return@addActionListener
            if (lastSelectedDatabase != database) {
                AzureDatabaseMvpModel.getSqlServerAdminLoginAsync(lastSelectedSubscriptionId, database).subscribe { lblSqlDbAdminLogin.text = it }
                lastSelectedDatabase = database
            }
        }
    }

    private fun initSqlServerComboBox() {
        cbExistSqlServer.renderer = object : ListCellRendererWrapper<SqlServer>() {
            override fun customize(list: JList<*>, sqlServer: SqlServer?, index: Int, selected: Boolean, hasFocus: Boolean) {
                if (sqlServer == null) {
                    setText(SQL_SERVER_EMPTY_MESSAGE)
                    return
                }
                setText("${sqlServer.name()} (${sqlServer.resourceGroupName()})")
            }
        }

        cbExistSqlServer.addActionListener {
            val sqlServer = cbExistSqlServer.getItemAt(cbExistSqlServer.selectedIndex) ?: return@addActionListener

            if (sqlServer != lastSelectedSqlServer) {
                lblExistingSqlServerLocation.text = sqlServer.region().label()
                lblExistingSqlServerAdminLogin.text = sqlServer.administratorLogin()
                lastSelectedSqlServer = sqlServer

                toggleSqlServerComboBox(sqlServer)
            }
        }
    }

    private fun initDatabaseEditionComboBox() {
        cbDatabaseEdition.addActionListener {
            lastSelectedDatabaseEdition = cbDatabaseEdition.getItemAt(cbDatabaseEdition.selectedIndex) ?: return@addActionListener
        }
    }

    private fun initRuntimeMismatchWarningLabel() {
        lblRuntimeMismatchWarning.icon = warningIcon
    }

    private fun resetSubscriptionComboBoxValues() {
        resetResourceGroupComboBoxValues()
        resetLocationComboBoxValues()
        resetAppServicePlanComboBoxValues()
        resetSqlDatabaseComboBoxValues()
        resetSqlServerComboBoxValues()
    }

    private fun resetResourceGroupComboBoxValues() {
        cbResourceGroup.removeAllItems()
        cbDbResourceGroup.removeAllItems()
    }

    /**
     * Reset Location combo box values
     */
    private fun resetLocationComboBoxValues() {
        cbLocation.removeAllItems()
        cbSqlServerLocation.removeAllItems()
    }

    /**
     * Reset SQL Server combo box values from selected item
     */
    private fun resetSqlServerComboBoxValues() {
        cbExistSqlServer.removeAllItems()
        lblExistingSqlServerLocation.text = NOT_APPLICABLE
        lblExistingSqlServerAdminLogin.text = NOT_APPLICABLE
    }

    private fun resetAppServicePlanComboBoxValues() {
        cbAppServicePlan.removeAllItems()
        lblLocation.text = NOT_APPLICABLE
        lblPricingTier.text = NOT_APPLICABLE
    }

    private fun resetSqlDatabaseComboBoxValues() {
        cbDatabase.removeAllItems()
        lblSqlDbAdminLogin.text = NOT_APPLICABLE
        passExistingDbAdminPassword.text = ""
    }

    //endregion Initialize UI Components

    //region Header Decorators

    private fun setHeaderDecorators() {
        setResourceGroupDecorator()
        setAppServicePlanDecorator()
        setOperatingSystemDecorator()

        setDbResourceGroupDecorator()
        setSqlServerDecorator()
    }

    private fun setResourceGroupDecorator() {
        setDecorator(HEADER_RESOURCE_GROUP, pnlResourceGroupHolder, pnlResourceGroup)
    }

    private fun setAppServicePlanDecorator() {
        setDecorator(HEADER_APP_SERVICE_PLAN, pnlAppServicePlanHolder, pnlAppServicePlan)
    }

    private fun setOperatingSystemDecorator() {
        setDecorator(HEADER_OPERATING_SYSTEM, pnlOperatingSystemHolder, pnlOperatingSystem)
    }

    private fun setDbResourceGroupDecorator() {
        setDecorator(HEADER_RESOURCE_GROUP, pnlDbResourceGroupHolder, pnlDbResourceGroup)
    }

    private fun setSqlServerDecorator() {
        setDecorator(HEADER_SQL_SERVER, pnlSqlServerHolder, pnlSqlServer)
    }

    /**
     * Set header decorator for a panel with a specified name
     *
     * @param name - decorator name
     * @param panelHolder - main panel holder for a decorator
     * @param content - panel content
     */
    private fun setDecorator(name: String, panelHolder: JPanel?, content: JComponent?) {
        val decorator = HideableDecorator(panelHolder, name, true)
        decorator.setContentComponent(content)
        decorator.setOn(true)
    }

    //endregion Header Decorators

    //region Azure Model

    /**
     * Update cached values in [com.microsoft.azuretools.utils.AzureModel] and [AzureDatabaseMvpModel]
     * in the background to use for fields validation on client side
     */
    private fun updateAzureModelInBackground() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureModelController.updateResourceGroupMaps(UpdateProgressIndicator(indicator))
                AzureDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
            }
        })
    }

    private fun collectConnectionStringsInBackground(webApps: List<WebApp>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Web App connection strings map", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                webApps.forEach {
                    AzureDotNetWebAppMvpModel.getConnectionStrings(it, true)
                }
            }
        })
    }

    //endregion Azure Model

    //region Private Methods and Operators

    private fun setConfigurationName(name: String) {
        configuration.name = name
    }

    /**
     * Get a user friendly text for net framework for an existing Azure web app instance
     *
     * Note: For .NET Framework there are two valid values for version (v4.7 and v3.5). Azure SDK
     *       does not set the right values. They set the "v4.0" for "v4.7" and "v2.0" for "v3.5" instances
     *       For .Net Core Framework we get the correct runtime value from the linux fx version that store
     *       a instance name representation
     *
     * Note: 2.0 and 4.0 are CLR versions most likely
     *
     * @param webApp web app instance
     * @return [String] with a .Net Framework version for a web app instance
     */
    private fun mapWebAppFrameworkVersion(webApp: WebApp): String {
        return if (webApp.operatingSystem() == OperatingSystem.LINUX) {
            "Core v${webApp.linuxFxVersion().split('|')[1]}"
        } else {
            val version = webApp.netFrameworkVersion().toString()
            if (version.startsWith("v4")) "v4.7"
            else "v3.5"
        }
    }

    private fun getProjectNetFrameworkVersion(publishableProject: PublishableProjectModel): String {
        val defaultVersion = "4.7"
        val currentFramework = getCurrentFrameworkId(publishableProject) ?: return defaultVersion
        return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value ?: defaultVersion
    }

    /**
     * Get a Target .Net Core Framework value for a publishable .net core project
     *
     * @param publishableProject a publishable project instance
     * @return [String] a version for a Target Framework in format:
     *                  for a target framework ".NETCoreApp,Version=v2.0", the method returns "2.0"
     */
    private fun getProjectNetCoreFrameworkVersion(publishableProject: PublishableProjectModel): String {
        val defaultVersion = "2.1"
        val currentFramework = getCurrentFrameworkId(publishableProject) ?: return defaultVersion
        return netCoreAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value ?: defaultVersion
    }

    private fun getCurrentFrameworkId(publishableProject: PublishableProjectModel): String? {
        val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
        return targetFramework?.currentTargetFrameworkId?.valueOrNull
    }

    /**
     * Wrapper to get a typed Combo Box object
     */
    private fun <T>getSelectedItem(comboBox: JComboBox<T>): T? = comboBox.getItemAt(comboBox.selectedIndex)

    /**
     * Set all provided components to a specified enabled state
     */
    private fun setComponentsEnabled(isEnabled: Boolean, vararg components: JComponent) {
        components.forEach { it.isEnabled = isEnabled }
    }

    /**
     * Set all provided components to a specified visibility state
     */
    private fun setComponentsVisible(isVisible: Boolean, vararg components: JComponent) {
        components.forEach { it.isVisible = isVisible }
    }

    //endregion Private Methods and Operators
}
