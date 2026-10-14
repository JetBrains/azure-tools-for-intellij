plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledModules("intellij.platform.remoteServers", "intellij.platform.remoteServers.impl")
        bundledPlugins("Docker")
    }

    implementation(libs.azureToolkitAuthLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(project(path = ":azure-intellij-plugin-lib"))
}
