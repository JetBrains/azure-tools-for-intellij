plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
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

dependencies {
    intellijPlatform {
        rider(platformVersion) {
            useInstaller = false
            useCache = true
        }
        jetbrainsRuntime()
        bundledPlugin("com.intellij.modules.jcef")
        bundledPlugins("org.jetbrains.plugins.terminal")
    }

    implementation(libs.azureToolkitLibs)
    implementation(libs.azureToolkitIdeLibs)
    implementation(libs.azureToolkitHdinsightLibs)

    implementation(libs.azureToolkitAuthLib)
    implementation(libs.azureToolkitIdeCommonLib)

    compileOnly(libs.lombok)
    compileOnly("org.jetbrains:annotations:24.0.0")
    annotationProcessor(libs.lombok)
    implementation(libs.azureToolkitCommonLib)
    aspect(libs.azureToolkitCommonLib)
    implementation("org.aspectj:aspectjrt:1.9.25")

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

configurations {
    implementation { exclude(module = "slf4j-api") }
    implementation { exclude(module = "log4j") }
    implementation { exclude(module = "stax-api") }
    implementation { exclude(module = "groovy-xml") }
    implementation { exclude(module = "jna") }
    implementation { exclude(module = "xpp3") }
    implementation { exclude(module = "pull-parser") }
    implementation { exclude(module = "xsdlib") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    compileJava {
        options.release.set(25)
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
