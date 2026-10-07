<div align="center">
  <img alt="Logo" src="./PluginsAndFeatures/azure-toolkit-for-rider/src/main/resources/META-INF/pluginIcon.svg#gh-light-mode-only" width="100">
  <img alt="Logo" src="./PluginsAndFeatures/azure-toolkit-for-rider/src/main/resources/META-INF/pluginIcon_dark.svg#gh-dark-mode-only" width="100">
  <h2>Azure Toolkit for Rider</h2>

**This is the JetBrains Rider Plugin for Azure Cloud Services Integration.**

The plugin streamlines the development, deployment, and management of .NET applications on Azure, directly from Rider and across all supported platforms.

  <a href="https://github.com/JetBrains"><img src="http://jb.gg/badges/official-flat-square.svg" alt="JetBrains Official"></a>
  <a href="https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider"><img src="https://img.shields.io/jetbrains/plugin/v/11220-azure-toolkit-for-rider.svg?label=plugin&logo=rider" alt="Version"></a>
  <a href="https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider"><img src="https://img.shields.io/jetbrains/plugin/d/11220-azure-toolkit-for-rider.svg" alt="Downloads"></a>
</div>

---

## Key features

- **Azure Explorer**: View and manage your Azure resources, including Web Apps, Function Apps, databases, Redis caches, virtual machines, storage accounts, and more.
- **Azure App Services**: Easily create, deploy, and manage .NET Web Apps and Function Apps on any platform.
- **Databases**: Create and manage SQL Server, PostgreSQL, and MySQL databases.
- **Azure Cloud Shell**: Use the built-in Cloud Shell terminal to run commands in your Azure subscription, upload files, intercept downloads, and open files from Cloud Shell in a browser.
- **Bicep support**: Automatically download and connect to the Bicep language server.

The plugin can be downloaded and installed in JetBrains Rider and is
available [from the JetBrains plugins repository](https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider).

Feature requests can be logged in our [issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues), we
also welcome contributions.

## Resources

* [Issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues)
* [Plugin page](https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider)

## History and differences with Microsoft Azure Toolkit for IntelliJ

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is a fork of
the [Azure Toolkit for IntelliJ](https://docs.microsoft.com/en-us/java/azure/intellij/azure-toolkit-for-intellij-installation),
available [on GitHub](https://github.com/Microsoft/azure-tools-for-java).

Microsoft's Azure Toolkit for IntelliJ provides similar functionality to the Azure plugin
for [JetBrains Rider](https://www.jetbrains.com/rider), however, focus on the Java/JVM ecosystem and development flows.
JetBrains decided to fork the original plugin, and split base functionality (such as browsing Azure resources) from
Java/JVM-specific features (such as deploying a `.war` file to the HDInsight service).

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is released with several notable differences:

* No telemetry or usage data is collected and sent to Microsoft
* Java/JVM-specific functionality was removed
* .NET-specific functionalities, such as deploying an ASP.NET web application and more, have been added

## Contributing

Please see the [contribution instructions](CONTRIBUTING.md) if you wish to build the plugin from source.