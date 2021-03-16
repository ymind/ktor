/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.request

import io.ktor.client.utils.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlin.reflect.*

internal val BodyTypeAttributeKey: AttributeKey<KType> = AttributeKey("BodyTypeAttributeKey")

public data class TypedBody<T : Any> @PublishedApi internal constructor(
    val body: T,
    val type: KType?
) {

    public companion object {
        public inline operator fun <reified T : Any> invoke(value: T): TypedBody<T> = bodyOf(value)
    }
}

public inline fun <reified T : Any> bodyOf(value: T): TypedBody<T> = TypedBody(
    body = value,
    type = tryGetType(value)
)

@PublishedApi
@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T : Any> tryGetType(ignored: T): KType? = try {
    // We need to wrap getting type in try catch because of KT-42913
    typeOf<T>()
} catch (_: Throwable) {
    null
}

public inline fun <reified T> HttpRequestBuilder.body(body: T) {
    when (body) {
        null -> {
            this.body = EmptyContent
        }
        is TypedBody<*> -> {
            this.body = body.body
            this.bodyType = body.type
        }
        is String,
        is OutgoingContent,
        is ByteArray,
        is ByteReadChannel -> {
            this.body = body
        }
        else -> {
            this.body = body
            bodyType = tryGetType(body)
        }
    }
}
