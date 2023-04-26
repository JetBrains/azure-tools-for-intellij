plugins {
    kotlin("jvm") version "1.8.0"
    id("com.jetbrains.rdgen") version "2023.1.2"
    id("org.jetbrains.intellij") version "1.12.0"
    id("me.filippov.gradle.jvm.wrapper") version "0.11.0"
    id("io.freefair.aspectj.post-compile-weaving") version "6.5.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "com.jetbrains"
version = "1.0-SNAPSHOT"

val azureToolkitVersion = "0.33.0-SNAPSHOT"
val azureToolkitUtilsVersion = "3.77.0-SNAPSHOT"

extra.apply {
    set("azureToolkitVersion", azureToolkitVersion)
    set("azureToolkitUtilsVersion", azureToolkitUtilsVersion)
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.intellij")
        plugin("io.freefair.aspectj.post-compile-weaving")
        plugin("io.spring.dependency-management")
    }
    intellij {
        version.set("2022.3.2")
        type.set("RD")
        downloadSources.set(false)
    }
    dependencyManagement {
        imports {
            mavenBom("com.microsoft.azure:azure-toolkit-libs:$azureToolkitVersion")
            mavenBom("com.microsoft.azure:azure-toolkit-ide-libs:$azureToolkitVersion")
            mavenBom("com.microsoft.azuretools:utils:$azureToolkitUtilsVersion")
        }
    }
    configurations {
        implementation { exclude(module = "slf4j-api") }
        implementation { exclude(module = "log4j") }
        implementation { exclude(module = "stax-api") }
        implementation { exclude(module = "groovy-xml") }
        implementation { exclude(module = "groovy-templates") }
        implementation { exclude(module = "jna") }
    }
    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        implementation("com.microsoft.azure:azure-toolkit-common-lib")
        aspect("com.microsoft.azure:azure-toolkit-common-lib")
        compileOnly("org.jetbrains:annotations")
    }

    tasks {
        compileJava {
            sourceCompatibility = "17"
            targetCompatibility = "17"
        }

        compileKotlin {
            kotlinOptions.jvmTarget = "17"
        }

        buildSearchableOptions {
            enabled = false
        }
    }
}

// disable runIde tasks in subprojects to prevent starting-up multiple ide.
gradle.taskGraph.whenReady {
    val hasRootRunTask = this.hasTask(":runIde")

    if (hasRootRunTask) {
        val regex = ":.+:runIde".toRegex()
        this.allTasks.forEach { task ->
            val subRunTask = regex.containsMatchIn(task.path)
            if (subRunTask) {
                task.enabled = false
            }
        }
    }
}

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        resources.setSrcDirs(listOf("src/main/resources", "../azure-toolkit-for-intellij/src/main/resources/icons"))
    }
}

val resharperPluginPath = projectDir.resolve("ReSharper.Azure")
val rdLibDirectory: () -> File = { file("${tasks.setupDependencies.get().idea.get().classes}/lib/rd") }
extra["rdLibDirectory"] = rdLibDirectory

dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    implementation(project(":azure-intellij-plugin-guidance"))
    implementation(project(":azure-intellij-resource-connector-lib"))
    implementation(project(":azure-intellij-plugin-service-explorer"))
    implementation(project(":azure-intellij-plugin-arm"))
    implementation(project(":azure-intellij-plugin-containerservice"))

    implementation("com.microsoft.azuretools:azure-explorer-common:3.77.0-SNAPSHOT") {
        exclude(
            "javax.xml.bind",
            "jaxb-api"
        )
    }
    implementation("com.microsoft.azuretools:hdinsight-node-common:3.77.0-SNAPSHOT") {
        exclude(
            "javax.xml.bind",
            "jaxb-api"
        )
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("")
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