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

    implementation("org.dom4j:dom4j:2.1.3") {
        exclude(group = "javax.xml.stream", module = "stax-api")
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "pull-parser", module = "pull-parser")
        exclude(group = "net.java.dev.msv", module = "xsdlib")
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2") {
        exclude(group = "com.fasterxml.jackson", module = "jackson-bom")
    }
}
