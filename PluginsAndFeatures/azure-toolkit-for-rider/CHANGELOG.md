<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Azure Toolkit for Rider Changelog

## [Unreleased]

### Changed

- Update domain for Azure Functions CDN

## [4.3.8] - 2024-12-17

### Fixed

- Do not update App Services during the deployment ([RIDER-119626](https://youtrack.jetbrains.com/issue/RIDER-119626))
- Search for the `in-proc` folders for the in-process worker model ([RIDER-120960](https://youtrack.jetbrains.com/issue/RIDER-120960))

## [4.3.7] - 2024-12-09

### Fixed

- Use core tools `v4` path from the settings for the `v0` `AzureFunctionsVersion` property ([RIDER-120285](https://youtrack.jetbrains.com/issue/RIDER-120285))

## [4.3.6] - 2024-12-09

### Fixed

- Use the local settings file to calculate the proper version of the core tools

## [4.3.5] - 2024-11-21

### Changed

- Reimplement Function core tools management

## [4.3.4] - 2024-11-13

### Fixed

- Remove the locked 4.99.0 release of the core tools as the archive can be unzipped without problems

## [4.2.6] - 2024-11-13

### Fixed

- Remove the locked 4.99.0 release of the core tools as the archive can be unzipped without problems

## [4.3.3] - 2024-11-12

### Fixed

- Fix DEXP-832012: Error while extracting a file

## [4.2.5] - 2024-11-11

### Fixed

- Fix DEXP-832012: Error while extracting a file

## [4.3.2] - 2024-10-28

### Fixed

- Fix the 4.99.0 release of the core tools ([#944](https://github.com/JetBrains/azure-tools-for-intellij/issues/944), [RIDER-119093](https://youtrack.jetbrains.com/issue/RIDER-119093))

## [4.2.4] - 2024-10-28

### Fixed

- Fix the 4.99.0 release of the core tools ([#944](https://github.com/JetBrains/azure-tools-for-intellij/issues/944), [RIDER-119093](https://youtrack.jetbrains.com/issue/RIDER-119093))

## [4.3.1] - 2024-10-25

### Fixed

- Cannot find Azure Core Functions Tool ([#944](https://github.com/JetBrains/azure-tools-for-intellij/issues/944), [RIDER-119093](https://youtrack.jetbrains.com/issue/RIDER-119093))

## [4.2.3] - 2024-10-25

### Fixed

- Cannot find Azure Core Functions Tool ([#944](https://github.com/JetBrains/azure-tools-for-intellij/issues/944), [RIDER-119093](https://youtrack.jetbrains.com/issue/RIDER-119093))

## [4.3.0] - 2024-10-24

### Changed

- Support for Rider 2024.3

## [4.2.2] - 2024-10-09

### Fixed

- Allow comments in the local.settings.json file ([#900](https://github.com/JetBrains/azure-tools-for-intellij/issues/900))
- Show a more accurate description of errors if the deployment fails ([RIDER-113475](https://youtrack.jetbrains.com/issue/RIDER-113475))

## [4.2.1] - 2024-10-04

### Fixed

- Error: unable to find valid certification path to requested target ([#788](https://github.com/JetBrains/azure-tools-for-intellij/issues/788))

## [4.2.0] - 2024-09-19

### Fixed

- Improve error notification if unable to get Azure Function worker PID ([RIDER-116398](https://youtrack.jetbrains.com/issue/RIDER-116398))

### Added

- Setting to disable Azurite executable check before running a configuration ([RIDER-106668](https://youtrack.jetbrains.com/issue/RIDER-106668))

## [4.1.3] - 2024-09-06

### Fixed

- Detect the Function worker runtime from the installed packages and show notifications otherwise ([RIDER-116722](https://youtrack.jetbrains.com/issue/RIDER-116722))
- Use zipdeploy for the App Service deployments ([#898](https://github.com/JetBrains/azure-tools-for-intellij/issues/898))

## [4.1.2] - 2024-09-04

### Fixed

- Unable to sign in to Azure using OAuth 2.0: Unable to locate JNA native support library ([RIDER-116013](https://youtrack.jetbrains.com/issue/RIDER-116013), [#884](https://github.com/JetBrains/azure-tools-for-intellij/issues/884))

## [4.1.1] - 2024-09-02

### Fixed

- Azurite configuration is reset with each plugin major update ([#895](https://github.com/JetBrains/azure-tools-for-intellij/issues/895))
- Publish to Azure App Service deletes user files ([#902](https://github.com/JetBrains/azure-tools-for-intellij/issues/902))

## [4.1.0] - 2024-08-21

### Added

- Support for Azure Service Bus
- Support for Azure Event Hub
- Support for Azure VMs

### Fixed

- Resolve MSBuild properties in the Function run configuration
- Function classes are shown as never instantiated ([#891](https://github.com/JetBrains/azure-tools-for-intellij/issues/891))

## [4.0.2] - 2024-08-16

### Added

- Option to disable authentication cache

### Fixed

- Plugin uses `v4` func cli even if the value of the MSBuild property is `v0`

## [4.0.1] - 2024-08-16

### Changed

- Do not require `launchSettings.json` to run a Function project ([#881](https://github.com/JetBrains/azure-tools-for-intellij/issues/881))

## [4.0.0] - 2024-08-13

### Added

- Support for Azure Redis
- Support for Azure KeyVault
- Support for Azure Storage accounts
- Edit and Continue for Function run configuration

### Fixed

- Read run configurations from `launchSettings.json` file ([RIDER-92674](https://youtrack.jetbrains.com/issue/RIDER-92674))
- Fixed Function nuget package suggestion on project opening
- Use AzureToolsForIntelliJ/Azurite folder for Azurite workspace
- Unable to deploy function app from standalone project ([#862](https://github.com/JetBrains/azure-tools-for-intellij/issues/862))

### Removed

- App Settings table from the deployment configurations

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

[Unreleased]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.8...HEAD
[4.3.8]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.7...v4.3.8
[4.3.7]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.6...v4.3.7
[4.3.6]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.5...v4.3.6
[4.3.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.4...v4.3.5
[4.3.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.6...v4.3.4
[4.3.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.5...v4.3.3
[4.3.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.4...v4.3.2
[4.3.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.3...v4.3.1
[4.3.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.2...v4.3.0
[4.2.6]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.3...v4.2.6
[4.2.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.2...v4.2.5
[4.2.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.1...v4.2.4
[4.2.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.0...v4.2.3
[4.2.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.1...v4.2.2
[4.2.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.2.0...v4.2.1
[4.2.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.1.3...v4.2.0
[4.1.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.1.2...v4.1.3
[4.1.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.1.1...v4.1.2
[4.1.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.1.0...v4.1.1
[4.1.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.2...v4.1.0
[4.0.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.1...v4.0.2
[4.0.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0...v4.0.1
[4.0.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.7...v4.0.0
[4.0.0-preview.7]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.6...v4.0.0-preview.7
[4.0.0-preview.6]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.5...v4.0.0-preview.6
[4.0.0-preview.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.4...v4.0.0-preview.5
[4.0.0-preview.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.3...v4.0.0-preview.4
[4.0.0-preview.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.2...v4.0.0-preview.3
[4.0.0-preview.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.0.0-preview.1...v4.0.0-preview.2
[4.0.0-preview.1]: https://github.com/JetBrains/azure-tools-for-intellij/commits/v4.0.0-preview.1
