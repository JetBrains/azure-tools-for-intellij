plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledModule("intellij.rider.rdclient.dotnet")
        bundledPlugin("org.jetbrains.plugins.textmate")
    }

    implementation(libs.serializationJson)
}
