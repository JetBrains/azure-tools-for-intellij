plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.database")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-cosmos"))
    implementation(libs.azureToolkitCosmosLib)
    implementation(libs.azureToolkitIdeCosmosLib)
    implementation(libs.azureToolkitIdentityLib)
    implementation(libs.azureToolkitIdeCommonLib)
}
