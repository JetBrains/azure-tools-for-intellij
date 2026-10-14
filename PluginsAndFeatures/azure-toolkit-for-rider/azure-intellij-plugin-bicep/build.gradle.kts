plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("org.jetbrains.plugins.textmate")
        bundledModule("intellij.platform.lsp")
        bundledModule("intellij.rider.rdclient.dotnet")
    }

    implementation(libs.serializationJson)
}
