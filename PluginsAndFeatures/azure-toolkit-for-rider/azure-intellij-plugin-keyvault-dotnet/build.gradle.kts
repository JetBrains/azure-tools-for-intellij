plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties", "org.jetbrains.plugins.yaml")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-keyvault"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitKeyvaultLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeKeyvaultLib)
}
