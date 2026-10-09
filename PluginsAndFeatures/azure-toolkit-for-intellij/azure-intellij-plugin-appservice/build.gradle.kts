plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties", "org.jetbrains.plugins.terminal")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
//    implementation(project(path = ":azure-intellij-plugin-guidance"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(project(path = ":azure-intellij-plugin-monitor"))

    implementation(libs.azureToolkitAppserviceLib)
    implementation(libs.azureToolkitIdeAppserviceLib)
    implementation(libs.azureToolkitIdeContainerregistryLib)
    implementation("com.jcraft:jsch:0.1.55")
    implementation(libs.plexusArchiver)
    implementation("org.codehaus.plexus:plexus-container-default:2.1.1")
    implementation("com.neovisionaries:nv-websocket-client:2.14")
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
