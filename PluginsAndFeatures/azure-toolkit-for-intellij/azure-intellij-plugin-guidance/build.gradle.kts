plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.modules.jcef")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.snakeyaml)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2") {
        exclude(group = "com.fasterxml.jackson", module = "jackson-bom")
    }
}
