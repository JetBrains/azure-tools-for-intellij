plugins {
    id("azure.java-module")
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.database")
    }

    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitDatabaseLib)
    implementation(libs.azureToolkitMysqlLib)
    implementation(libs.azureToolkitSqlserverLib)
    implementation(libs.azureToolkitPostgreLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeDatabaseLib)
    implementation(libs.mysqlConnectorJava)
    implementation(libs.mssqlJdbc)
    implementation(libs.postgresql)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
