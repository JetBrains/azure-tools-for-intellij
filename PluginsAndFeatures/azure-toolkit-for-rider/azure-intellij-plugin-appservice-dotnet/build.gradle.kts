dependencies {
    implementation(project(path = ":azure-intellij-plugin-lib-dotnet", configuration = "instrumentedJar"))
    implementation(project(path = ":azure-intellij-plugin-appservice", configuration = "instrumentedJar"))
    implementation(project(mapOf("path" to ":azure-intellij-plugin-lib-dotnet")))
}