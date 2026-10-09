plugins {
    id("azure.java-module")
}

dependencies {
    implementation(project(path = ":azure-intellij-plugin-lib"))
    implementation(project(path = ":azure-intellij-resource-connector-lib"))
    implementation(libs.azureToolkitRedisLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(libs.azureToolkitIdeRedisLib)
    implementation(libs.jedis)
}

configurations {
    implementation { exclude(module = "groovy-templates") }
}
