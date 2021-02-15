/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.resources

import io.ktor.application.*
import io.ktor.util.*
import io.ktor.resources.common.Resources as ResourcesCore

/**
 * Installable feature for [ResourcesCore].
 */
public object Resources : ApplicationFeature<Application, ResourcesCore.Configuration, ResourcesCore> {

    override val key: AttributeKey<ResourcesCore> = AttributeKey("Resources")

    override fun install(pipeline: Application, configure: ResourcesCore.Configuration.() -> Unit): ResourcesCore {
        val configuration = ResourcesCore.Configuration().apply(configure)
        return ResourcesCore(configuration)
    }
}

public inline fun <reified T : Any> Application.href(resource: T): String {
    return href(feature(Resources).resourcesFormat, resource)
}
