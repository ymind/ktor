/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.utils.io.*
import kotlin.test.*

internal const val TEST_BENCHMARKS_SERVER = "http://127.0.0.1:8080/benchmarks"

private val testData = mutableMapOf(
    0 to ByteArray(0),
    1 to ByteArray(1 * 1024),
    16 to ByteArray(16 * 1024),
    32 to ByteArray(32 * 1024),
    32 to ByteArray(32 * 1024),
    64 to ByteArray(64 * 1024),
    256 to ByteArray(256 * 1024),
    1024 to ByteArray(1024 * 1024)
)

class BenchmarkTest : ClientLoader() {
    private val size = 1 // 1024

    @Test
    fun testDownload() = clientTests {
        test { client ->
            val data = client.get("$TEST_BENCHMARKS_SERVER/bytes?size=$size").body<ByteArray>()
            check(data.size == size * 1024)
        }
    }

    @Test
    fun testUpload() = clientTests {
        test { client ->
            val uploaded = client.post("$TEST_BENCHMARKS_SERVER/bytes") {
                setBody(testData[size]!!)
            }.body<String>()
            check(uploaded.toInt() == size * 1024) { "Expected ${size * 1024}, got $uploaded" }
        }
    }

    @Test
    fun testEchoStream() = clientTests {
        test { client ->
            val uploaded = client.post("$TEST_BENCHMARKS_SERVER/echo") {
                setBody(testData[size]!!)
            }.body<ByteArray>()

            check(uploaded.size == size * 1024) { "Expected ${size * 1024}, got ${uploaded.size}" }
        }
    }

    @Test
    fun testEchoStreamChain() = clientTests {
        test { client ->
            val stream = client.post("$TEST_BENCHMARKS_SERVER/echo") {
                setBody(testData[size]!!)
            }.body<ByteReadChannel>()

            val result = client.post("$TEST_BENCHMARKS_SERVER/echo") {
                setBody(stream)
            }.body<ByteArray>()

            check(result.size == size * 1024) { "Expected ${size * 1024}, got ${result.size}" }
        }
    }
}
