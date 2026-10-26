plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties", "org.jetbrains.plugins.terminal")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
//    implementation(project(path = ":azure-intellij-plugin-guidance"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
//    implementation(project(path = ":azure-intellij-plugin-monitor"))

    implementation(libs.azureToolkitAppserviceLib)
    implementation(libs.azureToolkitIdeAppserviceLib)
    implementation(libs.azureToolkitIdeContainerregistryLib)
    implementation(libs.jsch)
    implementation(libs.plexusArchiver)
    implementation(libs.plexusContainerDefault)
    implementation(libs.nvWebsocketClient)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
