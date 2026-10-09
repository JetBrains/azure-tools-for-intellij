/*
 * Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

rootProject.name = "azure-build-logic"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
