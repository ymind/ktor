/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.resources.serialisation

import io.ktor.http.*
import io.ktor.resources.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@OptIn(ExperimentalSerializationApi::class)
internal class ParametersEncoder(
    override val serializersModule: SerializersModule
) : AbstractEncoder() {

    private val parametersBuilder = ParametersBuilder()

    val parameters: Parameters
        get() = parametersBuilder.build()

    private lateinit var nextElementName: String

    override fun encodeValue(value: Any) {
        parametersBuilder.append(nextElementName, value.toString())
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (descriptor.kind != StructureKind.LIST) {
            nextElementName = descriptor.getElementName(index)
        }
        return true
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeValue(enumDescriptor.getElementName(index))
    }

    override fun encodeNull() {
        // no op
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class PathPatternEncoder(
    override val serializersModule: SerializersModule,
) : AbstractEncoder() {

    private var nestingLevel: Int = -1
    private val nestingLevelsWithParents = mutableSetOf<Int>()
    private var currentResourceName: String? = null
    private var previousResourceName: String? = null

    private val pathBuilder = StringBuilder()

    val pathPattern
        get() = pathBuilder.toString()

    override fun encodeValue(value: Any) {
        // no op
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val annotation = descriptor.annotations.filterIsInstance<Resource>().firstOrNull() ?: return this

        nestingLevel++
        if (nestingLevelsWithParents.contains(nestingLevel)) {
            throw IllegalArgumentException("There are multiple parents for resource $currentResourceName")
        }
        nestingLevelsWithParents.add(nestingLevel)

        val addSlash = pathBuilder.isNotEmpty() && !pathBuilder.startsWith('/') && !annotation.path.endsWith('/')
        if (addSlash) {
            pathBuilder.insert(0, '/')
        }
        pathBuilder.insert(0, annotation.path)

        previousResourceName = currentResourceName
        currentResourceName = descriptor.serialName

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        super.endStructure(descriptor)
        nestingLevel--
        currentResourceName = previousResourceName
    }

    override fun encodeNull() {
        // no op
    }
}
