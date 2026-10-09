plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(libs.azureToolkitIdeCommonLib)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
