plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.database")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitCosmosLib)
    implementation(libs.azureToolkitIdeCosmosLib)
    implementation(libs.azureToolkitIdentityLib)
    implementation(libs.azureToolkitIdeCommonLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
