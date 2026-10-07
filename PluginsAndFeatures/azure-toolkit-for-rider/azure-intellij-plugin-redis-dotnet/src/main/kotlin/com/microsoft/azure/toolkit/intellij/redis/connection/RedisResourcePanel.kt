/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.redis.connection

import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.connector.Resource
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager
import com.microsoft.azure.toolkit.redis.AzureRedis
import com.microsoft.azure.toolkit.redis.RedisCache
import java.awt.event.ItemEvent
import javax.swing.JPanel


class RedisResourcePanel : AzureFormJPanel<Resource<RedisCache>> {
    private val subscriptionComboBox = SubscriptionComboBox()
    private val redisComboBox: AzureComboBox<RedisCache>

    init {
        val loader = {
            val subscriptionId = subscriptionComboBox.value?.id
            if (subscriptionId != null) Azure.az(AzureRedis::class.java).caches(subscriptionId).list()
            else emptyList()
        }
        redisComboBox = object : AzureComboBox<RedisCache>(loader) {
            override fun doGetDefaultValue(): RedisCache? {
                return CacheManager.getUsageHistory(RedisCache::class.java).peek {
                    val subscription = subscriptionComboBox.value
                    subscription == it.subscription
                }
            }

            override fun getItemText(item: Any?) = (item as? RedisCache)?.name ?: ""

            override fun refreshItems() {
                val subscriptionId = subscriptionComboBox.value?.id
                if (subscriptionId != null) {
                    Azure.az(AzureRedis::class.java).caches(subscriptionId).refresh()
                }
                super.refreshItems()
            }
        }
        redisComboBox.isRequired = true

        subscriptionComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                redisComboBox.reloadItems()
            } else if (it.stateChange == ItemEvent.DESELECTED) {
                redisComboBox.clear()
            }
        }
    }

    private val panel: JPanel = panel {
        row("Subscription:") {
            cell(subscriptionComboBox)
        }
        row("Redis:") {
            cell(redisComboBox)
        }
    }

    override fun setValue(data: Resource<RedisCache>?) {
        val cache = data?.data
        if (cache != null) {
            subscriptionComboBox.value = cache.subscription
            redisComboBox.value = cache
        }
    }

    override fun getValue(): Resource<RedisCache>? {
        val info = getValidationInfo(true)
        if (!info.isValid) return null

        val cache = redisComboBox.value
        return RedisResourceDefinition.INSTANCE.define(cache)
    }

    override fun getContentPanel() = panel

    override fun getInputs() = listOf(subscriptionComboBox, redisComboBox)
}