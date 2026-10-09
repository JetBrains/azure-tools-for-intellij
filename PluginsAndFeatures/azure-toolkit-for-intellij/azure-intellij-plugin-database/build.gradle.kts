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
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.3.1.jre8-preview")
    implementation("org.postgresql:postgresql:42.4.1")
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
