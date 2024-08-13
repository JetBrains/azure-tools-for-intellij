<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Azure Toolkit for Rider Changelog

## [Unreleased]

## [4.0.0] - 2024-08-13

### Added

- Support for Azure Redis
- Support for Azure KeyVault
- Support for Azure Storage accounts
- Edit and Continue for Function run configuration
- Support for MySQL databases
- Support for PostgreSQL databases
- Support for SQL Server databases
- WebApp and Function property views ([#767](https://github.com/JetBrains/azure-tools-for-intellij/issues/767))
- Azure Environment and Azure CLI path settings
- Azure Identity settings ([#787](https://github.com/JetBrains/azure-tools-for-intellij/issues/787))
- Option to choose Storage Account during the Function publishing ([#764](https://github.com/JetBrains/azure-tools-for-intellij/issues/764))
- Swap with Production action ([#806](https://github.com/JetBrains/azure-tools-for-intellij/issues/806))

### Fixed

- Read run configurations from `launchSettings.json` file ([RIDER-92674](https://youtrack.jetbrains.com/issue/RIDER-92674))
- Fixed Function nuget package suggestion on project opening
- Use AzureToolsForIntelliJ/Azurite folder for Azurite workspace
- Unable to deploy function app from standalone project ([#862](https://github.com/JetBrains/azure-tools-for-intellij/issues/862))
- Properly remove Azure Function project templates ([#844](https://github.com/JetBrains/azure-tools-for-intellij/issues/844))
- Unknown JSON token error in local.settings.json file preventing running/debugging ([#811](https://github.com/JetBrains/azure-tools-for-intellij/issues/811))
- Unable to deploy to the existing WebApp ([#782](https://github.com/JetBrains/azure-tools-for-intellij/issues/782))

### Removed

- App Settings table from the deployment configurations

### Changed

- Support for Rider 2024.2
- Reimplement Azure Cloud Shell support
- Improve Azure Function nuget suggestion
- Improve "Trigger HTTP function" action
- Support for Rider 2024.1
- Update tool window icon
- Reimplement Azurite support
- Reimplement Azure account functionality
- Reimplement Azure Explorer tool window
- Reimplement Azure Web Apps and Azure Web Apps for Containers deployment
- Reimplement Azure Functions deployment
- Reimplement Azure Functions local running
- Reimplement Azure Functions Core Tools integration
- Reimplement Azure Functions templates

## [4.0.0-preview.7] - 2024-07-10

### Changed

- Support for Rider 2024.2
- Reimplement Azure Cloud Shell support

### Fixed

- Properly remove Azure Function project templates ([#844](https://github.com/JetBrains/azure-tools-for-intellij/issues/844))

## [4.0.0-preview.6] - 2024-06-05

### Changed

- Improve Azure Function nuget suggestion
- Improve "Trigger HTTP function" action

## [4.0.0-preview.5] - 2024-05-13

### Added

- Support for MySQL databases
- Support for PostgreSQL databases
- Support for SQL Server databases

## [4.0.0-preview.4] - 2024-04-15

### Added

- WebApp and Function property views ([#767](https://github.com/JetBrains/azure-tools-for-intellij/issues/767))
- Azure Environment and Azure CLI path settings
- Azure Identity settings ([#787](https://github.com/JetBrains/azure-tools-for-intellij/issues/787))
- Option to choose Storage Account during the Function publishing ([#764](https://github.com/JetBrains/azure-tools-for-intellij/issues/764))
- Swap with Production action ([#806](https://github.com/JetBrains/azure-tools-for-intellij/issues/806))

### Fixed

- Unknown JSON token error in local.settings.json file preventing running/debugging ([#811](https://github.com/JetBrains/azure-tools-for-intellij/issues/811))

## [4.0.0-preview.3] - 2024-03-22

### Changed

- Support for Rider 2024.1

## [4.0.0-preview.2] - 2024-02-05

### Changed

- Update tool window icon
- Reimplement Azurite support

### Fixed

- Unable to deploy to the existing WebApp ([#782](https://github.com/JetBrains/azure-tools-for-intellij/issues/782))

## [4.0.0-preview.1] - 2024-01-24

### Changed

- Reimplement Azure account functionality
- Reimplement Azure Explorer tool window
- Reimplement Azure Web Apps and Azure Web Apps for Containers deployment
- Reimplement Azure Functions deployment
- Reimplement Azure Functions local running
- Reimplement Azure Functions Core Tools integration
- Reimplement Azure Functions templates

[Unreleased]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0...HEAD
[4.0.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.7...v4.0.0
[4.0.0-preview.7]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.6...v4.0.0-preview.7
[4.0.0-preview.6]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.5...v4.0.0-preview.6
[4.0.0-preview.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.4...v4.0.0-preview.5
[4.0.0-preview.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.3...v4.0.0-preview.4
[4.0.0-preview.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.2...v4.0.0-preview.3
[4.0.0-preview.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.1...v4.0.0-preview.2
[4.0.0-preview.1]: https://github.com/JetBrains/azure-tools-for-intellij/commits/v4.0.0-preview.1
