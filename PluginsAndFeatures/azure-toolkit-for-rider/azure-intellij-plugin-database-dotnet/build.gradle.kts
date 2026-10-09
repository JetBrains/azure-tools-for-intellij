plugins {
    id("azure.kotlin-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.database")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-plugin-database"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitDatabaseLib)
    implementation(libs.azureToolkitMysqlLib)
    implementation(libs.azureToolkitSqlserverLib)
    implementation(libs.azureToolkitPostgreLib)
}
