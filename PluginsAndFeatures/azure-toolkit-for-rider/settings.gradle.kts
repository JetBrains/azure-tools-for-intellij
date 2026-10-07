rootProject.name = "azure-toolkit-for-rider"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

fun intellijModule(name: String) {
    include(":$name")
    project(":$name").projectDir = file("../azure-toolkit-for-intellij/$name")
}
fun riderModule(name: String) {
    include(":$name")
}

intellijModule("azure-intellij-plugin-lib")
riderModule("azure-intellij-plugin-core")
riderModule("azure-intellij-plugin-lib-dotnet")
intellijModule("azure-intellij-plugin-guidance")
intellijModule("azure-intellij-resource-connector-lib")
intellijModule("azure-intellij-plugin-service-explorer")
intellijModule("azure-intellij-plugin-arm")
intellijModule("azure-intellij-plugin-monitor")
intellijModule("azure-intellij-plugin-appservice")
riderModule("azure-intellij-plugin-appservice-dotnet")
intellijModule("azure-intellij-plugin-database")
riderModule("azure-intellij-plugin-database-dotnet")
intellijModule("azure-intellij-plugin-redis")
riderModule("azure-intellij-plugin-redis-dotnet")
intellijModule("azure-intellij-plugin-storage")
riderModule("azure-intellij-plugin-storage-dotnet")
intellijModule("azure-intellij-plugin-keyvault")
riderModule("azure-intellij-plugin-keyvault-dotnet")
intellijModule("azure-intellij-plugin-cloud-shell")
intellijModule("azure-intellij-plugin-servicebus")
intellijModule("azure-intellij-plugin-eventhubs")
intellijModule("azure-intellij-plugin-vm")
riderModule("azure-intellij-plugin-cosmos")
riderModule("azure-intellij-plugin-cosmos-dotnet")
riderModule("azure-intellij-plugin-bicep")
