plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties", "org.jetbrains.plugins.yaml")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitKeyvaultLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeKeyvaultLib)
    implementation(libs.azureToolkitIdentityLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
