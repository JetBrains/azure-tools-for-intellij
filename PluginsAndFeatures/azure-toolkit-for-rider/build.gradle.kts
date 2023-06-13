fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.jetbrains.rdgen") version "2023.1.2"
    id("org.jetbrains.intellij") version "1.13.3"
    id("me.filippov.gradle.jvm.wrapper") version "0.11.0"
    id("io.freefair.aspectj.post-compile-weaving") version "6.5.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

val azureToolkitVersion = properties("azureToolkitVersion").get()
val azureToolkitUtilsVersion = properties("azureToolkitUtilsVersion").get()

extra.apply {
    set("azureToolkitVersion", azureToolkitVersion)
    set("azureToolkitUtilsVersion", azureToolkitUtilsVersion)
}

allprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("io.freefair.aspectj.post-compile-weaving")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }

    intellij {
        version.set(properties("platformVersion").get())
        type.set(properties("platformType").get())
        downloadSources.set(false)
        plugins.set(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
    }

    dependencyManagement {
        imports {
            mavenBom("com.microsoft.azure:azure-toolkit-libs:$azureToolkitVersion")
            mavenBom("com.microsoft.azure:azure-toolkit-ide-libs:$azureToolkitVersion")
            mavenBom("com.microsoft.azuretools:utils:$azureToolkitUtilsVersion")
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        implementation("com.microsoft.azure:azure-toolkit-common-lib")
        aspect("com.microsoft.azure:azure-toolkit-common-lib") {
            exclude("com.squareup.okhttp3", "okhttp")
            exclude("com.squareup.okhttp3", "okhttp-urlconnection")
            exclude("com.squareup.okhttp3", "logging-interceptor")
        }
        compileOnly("org.jetbrains:annotations")
    }

    configurations {
        implementation { exclude(module = "slf4j-api") }
        implementation { exclude(module = "log4j") }
        implementation { exclude(module = "stax-api") }
        implementation { exclude(module = "groovy-xml") }
        implementation { exclude(module = "groovy-templates") }
        implementation { exclude(module = "jna") }
    }

    tasks {
        compileJava {
            sourceCompatibility = "17"
            targetCompatibility = "17"
        }

        compileKotlin {
            kotlinOptions.jvmTarget = "17"
        }
    }
}

subprojects {
    tasks {
        buildPlugin { enabled = false }
        runIde { enabled = false }
        prepareSandbox { enabled = false }
        prepareTestingSandbox { enabled = false }
        buildSearchableOptions { enabled = false }
        patchPluginXml { enabled = false }
        publishPlugin { enabled = false }
        verifyPlugin { enabled = false }
    }
}

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
}

val resharperPluginPath = projectDir.resolve("ReSharper.Azure")
val rdLibDirectory: () -> File = { file("${tasks.setupDependencies.get().idea.get().classes}/lib/rd") }
extra["rdLibDirectory"] = rdLibDirectory

dependencies {
    implementation(project(path = ":azure-intellij-plugin-lib", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-guidance", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-resource-connector-lib", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-service-explorer", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-arm", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-containerservice", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-monitor", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-applicationinsights", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-containerregistry", configuration = "instrumentedJar"))

    aspect("com.microsoft.azure:azure-toolkit-common-lib") {
        exclude("com.squareup.okhttp3", "okhttp")
        exclude("com.squareup.okhttp3", "okhttp-urlconnection")
        exclude("com.squareup.okhttp3", "logging-interceptor")
    }

    implementation("com.microsoft.azuretools:azure-explorer-common") {
        exclude("javax.xml.bind", "jaxb-api")
    }
    implementation("com.microsoft.azuretools:hdinsight-node-common") {
        exclude("javax.xml.bind", "jaxb-api")
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        version.set(properties("pluginVersion").get())
        sinceBuild.set(properties("pluginSinceBuild").get())
        untilBuild.set(properties("pluginUntilBuild").get())
    }

    runIde {
        maxHeapSize = "8g"
    }

    rdgen {
        verbose = true
        hashFolder = "build/rdgen"
        logger.info("Configuring rdgen params")
        classpath({
            logger.info("Calculating classpath for rdgen, intellij.ideaDependency is ${rdLibDirectory().canonicalPath}")
            rdLibDirectory().resolve("rider-model.jar").canonicalPath
        })
        val resharperPluginPath = projectDir.resolve("ReSharper.Azure")
        val csDaemonGeneratedOutput = resharperPluginPath.resolve("src")
            .resolve("Azure.Daemon").resolve("Protocol")
        val ktGeneratedOutput = projectDir.resolve("src").resolve("main").resolve("kotlin")
            .resolve("org").resolve("jetbrains").resolve("protocol")
        sources(
            projectDir.resolve("protocol").resolve("src")
                .resolve("main").resolve("kotlin")
        )
        packages = "model.daemon"

        generator {
            language = "kotlin"
            transform = "asis"
            root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
            namespace = "com.jetbrains.rider.azure.model"
            directory = ktGeneratedOutput.canonicalPath
        }

        generator {
            language = "csharp"
            transform = "reversed"
            root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
            namespace = "JetBrains.Rider.Azure.Model"
            directory = csDaemonGeneratedOutput.canonicalPath
        }
    }

    compileKotlin {
        dependsOn(rdgen)
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
