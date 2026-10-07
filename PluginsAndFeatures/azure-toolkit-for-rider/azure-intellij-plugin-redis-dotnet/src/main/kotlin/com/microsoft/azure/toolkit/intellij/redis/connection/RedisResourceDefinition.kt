/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.redis.connection

import com.intellij.openapi.project.Project

class RedisResourceDefinition : BaseRedisResourceDefinition() {
    companion object {
        val INSTANCE = RedisResourceDefinition()
    }

    override fun getResourcePanel(project: Project?) = RedisResourcePanel()
}