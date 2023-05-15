rootProject.name = "azure-toolkit-for-rider"

pluginManagement {
    repositories {
        maven { setUrl("https://cache-redirector.jetbrains.com/plugins.gradle.org") }
        gradlePluginPortal()
        // This is for snapshot version of 'org.jetbrains.intellij' plugin
        maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
    resolutionStrategy {
        eachPlugin {
            when(requested.id.name) {
                // This required to correctly rd-gen plugin resolution. May be we should switch our naming to match Gradle plugin naming convention.
                "rdgen" -> {
                    useModule("com.jetbrains.rd:rd-gen:${requested.version}")
                }
            }
        }
    }
}

include("protocol")
include(":azure-intellij-plugin-lib")
project(":azure-intellij-plugin-lib").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-lib")
include(":azure-intellij-plugin-guidance")
project(":azure-intellij-plugin-guidance").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-guidance")
include(":azure-intellij-resource-connector-lib")
project(":azure-intellij-resource-connector-lib").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-resource-connector-lib")
include(":azure-intellij-plugin-service-explorer")
project(":azure-intellij-plugin-service-explorer").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-service-explorer")
include(":azure-intellij-plugin-arm")
project(":azure-intellij-plugin-arm").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-arm")
include(":azure-intellij-plugin-containerservice")
project(":azure-intellij-plugin-containerservice").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-containerservice")
include(":azure-intellij-plugin-monitor")
project(":azure-intellij-plugin-monitor").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-monitor")
include(":azure-intellij-plugin-applicationinsights")
project(":azure-intellij-plugin-applicationinsights").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-applicationinsights")
include(":azure-intellij-plugin-containerregistry")
project(":azure-intellij-plugin-containerregistry").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-containerregistry")