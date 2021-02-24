/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.benchmarks

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import kotlinx.benchmark.*

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

@State(Scope.Benchmark)
internal class ClientBenchmark {
    lateinit var client: HttpClient

    @Param("Apache", "OkHttp", "Android", "CIO") // "Jetty")
    var zengineName: String = ""

    @Param("0", "1", "16", "32", "64", "256", "1024")
    var size: Int = 0

    @Setup
    public fun start() {
        client = HttpClient(findEngine(zengineName))
    }

    @Benchmark
    public fun download() = runBenchmark {
        val data = client.get("$TEST_BENCHMARKS_SERVER/bytes?size=$size").body<ByteArray>()
        check(data.size == size * 1024)
    }

    @Benchmark
    public fun upload() = runBenchmark {
        val uploaded = client.post("$TEST_BENCHMARKS_SERVER/bytes") {
            setBody(testData[size]!!)
        }.body<String>()
        check(uploaded.toInt() == size * 1024) { "Expected ${size * 1024}, got $uploaded" }
    }

    @Benchmark
    public fun echoStream() = runBenchmark {
        val uploaded = client.post("$TEST_BENCHMARKS_SERVER/echo") {
            setBody(testData[size]!!)
        }.body<ByteArray>()

        check(uploaded.size == size * 1024) { "Expected ${size * 1024}, got ${uploaded.size}" }
    }

    @Benchmark
    public fun echoStreamChain() = runBenchmark {
        val stream = client.post("$TEST_BENCHMARKS_SERVER/echo") {
            setBody(testData[size]!!)
        }.body<ByteReadChannel>()

        val result = client.post("$TEST_BENCHMARKS_SERVER/echo") {
            setBody(stream)
        }.body<ByteArray>()

        check(result.size == size * 1024) { "Expected ${size * 1024}, got ${result.size}" }
    }

    @TearDown
    public fun stop() {
        client.close()
    }
}
