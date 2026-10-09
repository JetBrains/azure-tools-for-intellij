plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledModule("intellij.rider.rdclient.dotnet")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-storage"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitStorageLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeStorageLib)
}
