<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Azure Toolkit for Rider Changelog

## [Unreleased]

### Changed

- Sync plugin with upstream `release-3.97.6`.
- Sync plugin with upstream `release-v3.97.5`.
- Sync plugin with upstream `release-v3.97.4`.
- Sync plugin with upstream `release-v3.97.3`.
- Sync plugin with upstream `release-v3.97.2`.
- Sync plugin with upstream `release-v3.97.1`.

## [4.8.4] - 2026-08-03

### Changed

- Update platform version to RD-2026.2
- Update Bicep LSP to v0.46.1

## [4.8.3] - 2026-06-29

### Changed

- Update platform version to RD-2026.2-EAP8-SNAPSHOT

## [4.8.2] - 2026-06-02

### Fixed

- Fix Azure Explorer tool window ([RIDER-139406](https://youtrack.jetbrains.com/issue/RIDER-139406))

## [4.8.1] - 2026-05-28

### Removed

- ReSharper part for Azure Functions. The Azure Functions features were moved to Rider itself. 

## [4.8.0] - 2026-05-06

### Changed

- Update platform version to RD-2026.2-SNAPSHOT

## [4.7.4] - 2026-03-19

### Fixed

- `Function App name is not provided` error doesn't disappear after selecting Function App name in the tree ([RIDER-136950](https://youtrack.jetbrains.com/issue/RIDER-136950))
- Can't select an Azure project to be published and its configuration ([RIDER-136947](https://youtrack.jetbrains.com/issue/RIDER-136947))
- Function apps tree has a lot of redundant empty space ([RIDER-136948](https://youtrack.jetbrains.com/issue/RIDER-136948))

## [4.7.3] - 2026-03-11

### Changed

- Update the base plugin to the `endgame-202511` version
- Update the platform version to `2026.1-EAP5-SNAPSHOT`
- Rework deployment configuration app combo boxes ([RIDER-136305](https://youtrack.jetbrains.com/issue/RIDER-136305), [RIDER-132995](https://youtrack.jetbrains.com/issue/RIDER-132995))

### Fixed

- Catch an exception while trying to register rx manager for the second time ([#1143](https://github.com/JetBrains/azure-tools-for-intellij/issues/1143))
- Azure favorites disappear every time new Rider updated ([RIDER-129134](https://youtrack.jetbrains.com/issue/RIDER-129134))

## [4.7.2] - 2026-02-13

### Changed

- Update platform version to RD-2026.1-EAP3 +

## [4.7.1] - 2026-01-20

### Changed

- Internal changes

## [4.7.0] - 2026-01-08

### Changed

- Support for Rider 2026.1

## [4.6.5] - 2025-11-24

### Added

- Setting to specify `--skipApiVersionCheck` option for Azurite executable ([RIDER-129584](https://youtrack.jetbrains.com/issue/RIDER-129584))

### Fixed

- Rider fails to find a proper deployment slot ([RIDER-131444](https://youtrack.jetbrains.com/issue/RIDER-131444))

## [4.6.4] - 2025-11-20

### Changed

- Select the first tfm or launch profile when the specified value is missing or invalid ([RIDER-132763](https://youtrack.jetbrains.com/issue/RIDER-132763))

### Fixed

- Attach to Process → All Processes list has an empty value ([RIDER-131554](https://youtrack.jetbrains.com/issue/RIDER-131554))

## [4.6.3] - 2025-11-06

### Fixed

- Unable to install Bicep LSP ([RIDER-131961](https://youtrack.jetbrains.com/issue/RIDER-131961/Unable-to-install-Bicep-LSP))

## [4.6.2] - 2025-11-03

### Changed

- Support for Rider 2025.3 EAP 8

## [4.6.1] - 2025-10-22

### Changed

- Unify logic to detect Azure Function projects by using `AzureFunctionsVersion` MSBuild property ([RIDER-131333](https://youtrack.jetbrains.com/issue/RIDER-131333))

## [4.6.0] - 2025-10-21

### Changed

- Support for Rider 2025.3 EAP 6

## [4.5.5] - 2025-10-09

### Fixed

- Problems with launching and debugging function app ([#1116](https://github.com/JetBrains/azure-tools-for-intellij/issues/1116))
- Failed to start .net 8 isolated Azure Functions App ([RIDER-126110](https://youtrack.jetbrains.com/issue/RIDER-126110))

## [4.5.4] - 2025-08-21

### Fixed

- Fix .NET Aspire integration

## [4.5.3] - 2025-08-14

### Fixed

- Unable to start SSH session to Azure Virtual Machine ([RIDER-127487](https://youtrack.jetbrains.com/issue/RIDER-127487))

## [4.5.2] - 2025-07-10

### Fixed

- Endless loading in the Web App and Function App publish dialogs ([#1066](https://github.com/JetBrains/azure-tools-for-intellij/issues/1066))

## [4.5.1] - 2025-06-23

### Changed

- Support for Rider 2025.2 EAP 6
- Add Bicep support
- Show Azure Functions in the Endpoints tool window ([#515](https://github.com/JetBrains/azure-tools-for-intellij/issues/515))

## [4.5.0] - 2025-06-12

### Changed

- Support for Rider 2025.2 EAP 4

## [4.4.8] - 2025-04-16

### Added

- Support for Flex Consumption service plans ([RIDER-117297](https://youtrack.jetbrains.com/issue/RIDER-117297))
- Support for CosmosDB

### Fixed

- Blocking requests for run configuration validation ([RIDER-124123](https://youtrack.jetbrains.com/issue/RIDER-124123))
- No TODO window via View | Tool Windows ([RIDER-124351](https://youtrack.jetbrains.com/issue/RIDER-124351))

### Changed

- Support for Rider 2025.1

## [4.4.7] - 2025-04-02

### Fixed

- Unable to open the Resource Group and Service Plan dialog ([#1021](https://github.com/JetBrains/azure-tools-for-intellij/issues/1021))

## [4.4.6] - 2025-03-27

### Added

- Run configuration to deploy Function App as a container

### Fixed

- Unable to download Azure Functions Core Tools because of the `NoTransformationFoundException` ([#1042](https://github.com/JetBrains/azure-tools-for-intellij/issues/1042))

## [4.4.5] - 2025-03-06

### Changed

- Support for Rider 2025.1 EAP 7

## [4.4.4] - 2025-02-28

### Changed

- Support for Rider 2025.1 EAP 6

## [4.4.3] - 2025-02-07

### Added

- Initial .NET Aspire support

## [4.4.2] - 2025-01-31

### Changed

- Support for Rider 2025.1 EAP 2

## [4.4.1] - 2025-01-22

### Added

- Support for debugging Azure App Services remotely
- Ability to generate a Dockerfile for a Function project

### Changed

- Improve startup for the plugin

## [4.4.0] - 2025-01-17

### Changed

- Support for Rider 2025.1

### Fixed

- AppService ComboBox render logic
- Cannot Open Files of Deployed Apps from Explorer ([RIDER-120305](https://youtrack.jetbrains.com/issue/RIDER-120305))

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

[Unreleased]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.8.4...HEAD
[4.8.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.8.3...4.8.4
[4.8.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.8.2...4.8.3
[4.8.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.8.1...4.8.2
[4.8.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.8.0...4.8.1
[4.8.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.7.4...4.8.0
[4.7.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.7.3...4.7.4
[4.7.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.7.2...4.7.3
[4.7.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.7.1...4.7.2
[4.7.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.7.0...4.7.1
[4.7.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.6.5...4.7.0
[4.6.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.6.4...4.6.5
[4.6.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.6.3...4.6.4
[4.6.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.6.2...4.6.3
[4.6.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.6.1...4.6.2
[4.6.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.6.0...4.6.1
[4.6.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.5.5...4.6.0
[4.5.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.5.4...4.5.5
[4.5.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/4.5.3...4.5.4
[4.5.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.5.2...v4.5.3
[4.5.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.5.1...v4.5.2
[4.5.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.5.0...v4.5.1
[4.5.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.8...v4.5.0
[4.4.8]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.7...v4.4.8
[4.4.7]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.6...v4.4.7
[4.4.6]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.5...v4.4.6
[4.4.5]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.4...v4.4.5
[4.4.4]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.3...v4.4.4
[4.4.3]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.2...v4.4.3
[4.4.2]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.1...v4.4.2
[4.4.1]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.4.0...v4.4.1
[4.4.0]: https://github.com/JetBrains/azure-tools-for-intellij/compare/v4.3.8...v4.4.0
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
