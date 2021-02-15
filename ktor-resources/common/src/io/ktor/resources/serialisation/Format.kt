/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.resources.serialisation

import io.ktor.http.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*

@OptIn(ExperimentalSerializationApi::class)
public class ResourcesFormat(
    override val serializersModule: SerializersModule = EmptySerializersModule
) : SerialFormat {

    public fun <T> encodeToPathPattern(serializer: SerializationStrategy<T>, value: T): String {
        val encoder = PathPatternEncoder(serializersModule)
        encoder.encodeSerializableValue(serializer, value)
        return encoder.pathPattern
    }

    public fun <T> encodeToParameters(serializer: SerializationStrategy<T>, value: T): Parameters {
        val encoder = ParametersEncoder(serializersModule)
        encoder.encodeSerializableValue(serializer, value)
        return encoder.parameters
    }

    public fun <T> decodeFromParameters(deserializer: DeserializationStrategy<T>, parameters: Parameters): T {
        val input = ParametersDecoder(serializersModule, parameters, emptyList())
        return input.decodeSerializableValue(deserializer)
    }
}
