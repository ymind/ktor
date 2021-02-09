/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.resources

import io.ktor.http.*
import io.ktor.resources.serialisation.*
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*

@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> href(
    resource: T,
    serializersModule: SerializersModule = EmptySerializersModule
): String {
    val locationFormat = ResourcesFormat(serializersModule)
    val serializer = serializer<T>()
    val parameters = locationFormat.encodeToParameters(serializer, resource)
    val pathPattern = locationFormat.encodeToPathPattern(serializer, resource)

    val usedForPathParameterNames = mutableSetOf<String>()
    val pathParts = pathPattern.split("/")

    val urlBuilder = URLBuilder()

    val updatedParts = pathParts.flatMap {
        if (!it.startsWith('{') || !it.endsWith('}')) return@flatMap listOf(it)

        val part = it.substring(1, it.lastIndex)
        when {
            part.endsWith('?') -> {
                val values = parameters.getAll(part.dropLast(1)) ?: return@flatMap emptyList()
                if (values.size > 1) {
                    throw IllegalStateException(
                        "Expect zero or one parameter with name: ${part.dropLast(1)}, but found ${values.size}"
                    )
                }
                usedForPathParameterNames += part.dropLast(1)
                values
            }
            part.endsWith("...") -> {
                usedForPathParameterNames += part.dropLast(3)
                parameters.getAll(part.dropLast(3)) ?: emptyList()
            }
            else -> {
                val values = parameters.getAll(part)
                if (values == null || values.size != 1) {
                    throw IllegalStateException(
                        "Expect exactly one parameter with name: $part, but found ${values?.size ?: 0}"
                    )
                }
                usedForPathParameterNames += part
                values
            }
        }
    }

    urlBuilder.path(updatedParts)

    val queryArgs = parameters.filter { key, _ -> !usedForPathParameterNames.contains(key) }
    urlBuilder.parameters.appendAll(queryArgs)

    return urlBuilder.build().fullPath
}
