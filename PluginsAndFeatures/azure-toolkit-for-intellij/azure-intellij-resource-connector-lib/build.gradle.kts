plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties", "com.intellij.xml")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdentityLib)
    implementation(libs.dotenvJava)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
