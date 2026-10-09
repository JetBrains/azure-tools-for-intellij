plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("Docker")
    }

    implementation(libs.azureToolkitAuthLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(project(path = ":azure-intellij-plugin-lib"))
}
