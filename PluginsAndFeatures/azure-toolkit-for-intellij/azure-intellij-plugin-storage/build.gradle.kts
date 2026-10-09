plugins {
    id("azure.java-module")
}

dependencies {
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitStorageLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeStorageLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
