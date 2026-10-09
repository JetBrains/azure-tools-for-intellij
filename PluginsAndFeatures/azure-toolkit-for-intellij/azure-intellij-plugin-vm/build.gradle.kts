plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("org.jetbrains.plugins.remote-run", "com.jetbrains.plugins.webDeployment" ,"org.jetbrains.plugins.terminal")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-storage"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeVmLib)
    implementation(libs.azureToolkitComputeLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
