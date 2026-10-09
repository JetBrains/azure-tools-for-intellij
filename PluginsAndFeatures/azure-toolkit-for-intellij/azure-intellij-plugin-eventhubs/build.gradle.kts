plugins {
    id("azure.java-module")
}

dependencies {
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeEventHubsLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
