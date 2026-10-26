plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.modules.jcef")
        bundledPlugins("org.jetbrains.plugins.terminal")
    }

    implementation(libs.azureToolkitAuthLib)
    implementation(libs.azureToolkitIdeCommonLib)

    implementation(libs.dom4j) {
        exclude(group = "javax.xml.stream", module = "stax-api")
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "pull-parser", module = "pull-parser")
        exclude(group = "net.java.dev.msv", module = "xsdlib")
    }
    implementation(libs.jacksonDataformatYaml) {
        exclude(group = "com.fasterxml.jackson", module = "jackson-bom")
    }
}
