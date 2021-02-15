/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.resources

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.util.*
import io.ktor.resources.common.Resources as ResourcesCore

/**
 * Installable feature for [ResourcesCore].
 */
public object Resources : HttpClientFeature<ResourcesCore.Configuration, ResourcesCore> {

    override val key: AttributeKey<ResourcesCore> = AttributeKey("Resources")

    override fun prepare(block: ResourcesCore.Configuration.() -> Unit): ResourcesCore {
        val config = ResourcesCore.Configuration().apply(block)
        return ResourcesCore(config)
    }

    override fun install(feature: ResourcesCore, scope: HttpClient) {
        // no op
    }
}
