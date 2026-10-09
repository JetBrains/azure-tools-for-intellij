plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.modules.json")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeArmLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
