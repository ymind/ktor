/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.resources.common

import io.ktor.resources.serialisation.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*

public class Resources(configuration: Configuration) {

    public val resourcesFormat: ResourcesFormat = ResourcesFormat(configuration.serializersModule)

    @OptIn(ExperimentalSerializationApi::class)
    public class Configuration {
        public var serializersModule: SerializersModule = EmptySerializersModule
    }
}
