plugins {
    id("azure.java-module")
}

dependencies {
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeServiceBusLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
