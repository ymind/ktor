/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.lincheck

import io.ktor.utils.io.*
import org.jetbrains.kotlinx.lincheck.*
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.*
import org.jetbrains.kotlinx.lincheck.strategy.stress.*
import org.jetbrains.kotlinx.lincheck.verifier.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

@ModelCheckingCTest
@StressCTest
class ByteBufferChannelReadRemainingCancelStressTest : VerifierState() {
    val channel = ByteChannel()

    @Operation(runOnce = true)
    suspend fun writeByteAndCancel() {
        channel.writeByte(10)
        channel.cancel()
    }

    @Operation(runOnce = true, handleExceptionsAsResult = arrayOf(CancellationException::class, java.util.concurrent.CancellationException::class ))
    suspend fun readRemaining() {
        channel.readRemaining()
    }

    @Test
    fun runTest() {
        LinChecker.check(this::class.java)
    }

    override fun extractState(): Any = Unit
}
