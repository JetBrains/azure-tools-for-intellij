import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    // https://search.maven.org/artifact/com.jetbrains.rd/rd-gen
    id("com.jetbrains.rdgen") version "2023.3.0"
    id("org.jetbrains.intellij") version "1.15.0"
    id("me.filippov.gradle.jvm.wrapper") version "0.11.0"
    id("io.freefair.aspectj.post-compile-weaving") version "6.5.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.changelog") version "2.1.2"
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
        plugin("org.jetbrains.intellij")
        plugin("io.freefair.aspectj.post-compile-weaving")
        plugin("io.spring.dependency-management")
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

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
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

    intellij {
        version = properties("platformVersion")
        type = properties("platformType")
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

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
}

val rdLibDirectory: () -> File = { file("${tasks.setupDependencies.get().idea.get().classes}/lib/rd") }
extra["rdLibDirectory"] = rdLibDirectory

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    configure<com.jetbrains.rd.generator.gradle.RdGenExtension> {
        val modelDir = projectDir.resolve("protocol").resolve("src").resolve("main")
            .resolve("kotlin")
        val csDaemonGeneratedOutput = projectDir.resolve("ReSharper.Azure").resolve("src")
            .resolve("Azure.Daemon").resolve("Protocol")
        val ktGeneratedOutput = projectDir.resolve("src").resolve("main").resolve("kotlin")
            .resolve("org").resolve("jetbrains").resolve("protocol")

        verbose = true
        hashFolder = "build/rdgen"
        packages = "model.daemon"
        classpath({
            logger.info("Calculating classpath for rdgen, intellij.ideaDependency is ${rdLibDirectory().canonicalPath}")
            rdLibDirectory().resolve("rider-model.jar").canonicalPath
        })
        sources(modelDir)

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

    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    buildSearchableOptions {
        enabled = false
    }
}
