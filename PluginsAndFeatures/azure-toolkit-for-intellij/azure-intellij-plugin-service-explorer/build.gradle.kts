plugins {
    id("java")
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.aspectj)
}

repositories {
    mavenCentral()
    mavenLocal()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

val platformVersion: String by extra
val azureToolkitVersion: String by extra

dependencies {
    intellijPlatform {
        rider(platformVersion)
        jetbrainsRuntime()
        bundledPlugins(listOf("com.intellij.properties"))
        instrumentationTools()
    }

    implementation(platform("com.microsoft.azure:azure-toolkit-libs:$azureToolkitVersion"))
    implementation(platform("com.microsoft.azure:azure-toolkit-ide-libs:$azureToolkitVersion"))
    implementation(platform("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-libs:0.1.1"))

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib:$azureToolkitVersion")

    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.jetbrains:annotations:24.0.0")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("com.microsoft.azure:azure-toolkit-common-lib:$azureToolkitVersion")
    aspect("com.microsoft.azure:azure-toolkit-common-lib:$azureToolkitVersion")
}

configurations {
    implementation { exclude(module = "slf4j-api") }
    implementation { exclude(module = "log4j") }
    implementation { exclude(module = "stax-api") }
    implementation { exclude(module = "groovy-xml") }
    implementation { exclude(module = "groovy-templates") }
    implementation { exclude(module = "jna") }
    implementation { exclude(module = "xpp3") }
    implementation { exclude(module = "pull-parser") }
    implementation { exclude(module = "xsdlib") }
}

tasks {
    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    buildPlugin { enabled = false }
    patchPluginXml { enabled = false }
    prepareSandbox { enabled = false }
    publishPlugin { enabled = false }
    runIde { enabled = false }
    signPlugin { enabled = false }
    buildSearchableOptions { enabled = false }
    verifyPlugin { enabled = false }
}
