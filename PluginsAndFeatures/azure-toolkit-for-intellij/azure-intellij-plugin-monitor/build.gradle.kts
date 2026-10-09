plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledModules("intellij.libraries.microba")
        bundledPlugins("com.intellij.properties", "com.intellij.modules.json")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeApplicationinsightsLib)
    implementation("com.azure:azure-monitor-query:1.0.10")
    implementation("org.apache.commons:commons-csv:1.9.0")
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
