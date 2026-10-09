plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugins("com.intellij.properties")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdentityLib)
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
