/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.client.tests.utils.assertFailsWith
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.test.*

private const val TEST_URL = "$TEST_SERVER/timeout"

class HttpTimeoutTest : ClientLoader() {
    @Test
    fun testGet() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 1000 }
        }

        test { client ->
            val response = client.get("$TEST_URL/with-delay") {
                parameter("delay", 20)
            }.body<String>()
            assertEquals("Text", response)
        }
    }

    @Test
    fun testGetWithExceptionAndTryAgain() = clientTests {
        test { client ->
            val requestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("$TEST_URL/404")
                parameter("delay", 20)
            }

            val job = requestBuilder.executionContext
            assertTrue { job.isActive }

            assertFails { client.request(requestBuilder).body<String>() }
            assertTrue { job.isActive }

            requestBuilder.url("$TEST_URL/with-delay")

            val response = client.request(requestBuilder).body<String>()

            assertEquals("Text", response)
            assertTrue { job.isActive }
        }
    }

    @Test
    fun testWithExternalTimeout() = clientTests(listOf("Android")) {
        config {
            install(HttpTimeout)
        }

        test { client ->
            val requestBuilder = HttpRequestBuilder().apply {
                method = HttpMethod.Get
                url("$TEST_URL/with-delay")
                parameter("delay", 60 * 1000)
            }

            val exception = assertFails {
                withTimeout(500) {
                    client.request(requestBuilder).body<String>()
                }
            }

            assertTrue { exception is TimeoutCancellationException }
            assertTrue { requestBuilder.executionContext.getActiveChildren().none() }
        }
    }

    @Test
    fun testHead() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            val response = client.head("$TEST_URL/with-delay?delay=10")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun testHeadWithTimeout() = clientTests {
        config {
            install(HttpTimeout) {
                requestTimeoutMillis = 500
            }
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.head("$TEST_URL/with-delay?delay=1000")
            }
        }
    }

    @Test
    fun testGetWithCancellation() = clientTests(listOf("Curl")) {
        config {
            install(HttpTimeout) {
                requestTimeoutMillis = 500
            }

            test { client ->
                val requestBuilder = HttpRequestBuilder().apply {
                    method = HttpMethod.Get
                    url("$TEST_URL/with-stream")
                    parameter("delay", 2000)
                }

                client.prepareRequest(requestBuilder).body<ByteReadChannel>().cancel()

                delay(2000) // Channel is closing asynchronously.
                assertTrue { requestBuilder.executionContext.getActiveChildren().none() }
            }
        }
    }

    @Test
    fun testGetRequestTimeout() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 10 }
        }

        test { client ->
            assertFails {
                client.get("$TEST_URL/with-delay") {
                    parameter("delay", 5000)
                }.body<String>()
            }
        }
    }

    @Test
    fun testGetRequestTimeoutPerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFails {
                client.get("$TEST_URL/with-delay") {
                    parameter("delay", 5000)

                    timeout { requestTimeoutMillis = 10 }
                }.body<String>()
            }
        }
    }

    @Test
    fun testGetWithSeparateReceive() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 1000 }
        }

        test { client ->
            val response = client.request("$TEST_URL/with-delay") {
                method = HttpMethod.Get
                parameter("delay", 10)
            }
            val result: String = response.body()

            assertEquals("Text", result)
        }
    }

    @Test
    fun testGetWithSeparateReceivePerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            val response = client.request("$TEST_URL/with-delay") {
                method = HttpMethod.Get
                parameter("delay", 10)

                timeout { requestTimeoutMillis = 1000 }
            }
            val result: String = response.body()

            assertEquals("Text", result)
        }
    }

    @Test
    fun testGetRequestTimeoutWithSeparateReceive() = clientTests(listOf("Curl", "Js")) {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 1000 }
        }

        test { client ->
            val response = client.prepareRequest("$TEST_URL/with-stream") {
                method = HttpMethod.Get
                parameter("delay", 500)
            }.body<ByteReadChannel>()

            assertFailsWith<HttpRequestTimeoutException> {
                response.readUTF8Line()
            }
        }
    }

    @Test
    fun testGetRequestTimeoutWithSeparateReceivePerRequestAttributes() = clientTests(listOf("Curl", "Js")) {
        config {
            install(HttpTimeout)
        }

        test { client ->
            val response = client.prepareRequest("$TEST_URL/with-stream") {
                method = HttpMethod.Get
                parameter("delay", 10000)

                timeout { requestTimeoutMillis = 1000 }
            }.body<ByteReadChannel>()
            assertFailsWith<HttpRequestTimeoutException> {
                response.readUTF8Line()
            }
        }
    }

    @Test
    fun testGetStream() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 1000 }
        }

        test { client ->
            val response = client.get("$TEST_URL/with-stream") {
                parameter("delay", 10)
            }.body<ByteArray>()

            assertEquals("Text", String(response))
        }
    }

    @Test
    fun testGetStreamPerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            val response = client.get("$TEST_URL/with-stream") {
                parameter("delay", 10)

                timeout { requestTimeoutMillis = 1000 }
            }.body<ByteArray>()

            assertEquals("Text", String(response))
        }
    }

    @Test
    fun testGetStreamRequestTimeout() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 1000 }
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.get("$TEST_URL/with-stream") {
                    parameter("delay", 400)
                }.body<ByteArray>()
            }
        }
    }

    @Test
    fun testGetStreamRequestTimeoutPerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.get("$TEST_URL/with-stream") {
                    parameter("delay", 400)

                    timeout { requestTimeoutMillis = 1000 }
                }.body<ByteArray>()
            }
        }
    }

    @Test
    fun testRedirect() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 1000 }
        }

        test { client ->
            val response = client.get("$TEST_URL/with-redirect") {
                parameter("delay", 20)
                parameter("count", 2)
            }.body<String>()

            assertEquals("Text", response)
        }
    }

    @Test
    fun testRedirectPerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            val response = client.get("$TEST_URL/with-redirect") {
                parameter("delay", 20)
                parameter("count", 2)

                timeout { requestTimeoutMillis = 1000 }
            }.body<String>()
            assertEquals("Text", response)
        }
    }

    @Test
    fun testRedirectRequestTimeoutOnFirstStep() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 20 }
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.get("$TEST_URL/with-redirect") {
                    parameter("delay", 1000)
                    parameter("count", 5)
                }.body<String>()
            }
        }
    }

    @Test
    fun testRedirectRequestTimeoutOnFirstStepPerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.get("$TEST_URL/with-redirect") {
                    parameter("delay", 1000)
                    parameter("count", 5)

                    timeout { requestTimeoutMillis = 20 }
                }.body<String>()
            }
        }
    }

    @Test
    fun testRedirectRequestTimeoutOnSecondStep() = clientTests {
        config {
            install(HttpTimeout) { requestTimeoutMillis = 400 }
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.get("$TEST_URL/with-redirect") {
                    parameter("delay", 500)
                    parameter("count", 5)
                }.body<String>()
            }
        }
    }

    @Test
    fun testRedirectRequestTimeoutOnSecondStepPerRequestAttributes() = clientTests {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFailsWith<HttpRequestTimeoutException> {
                client.get("$TEST_URL/with-redirect") {
                    parameter("delay", 500)
                    parameter("count", 5)

                    timeout { requestTimeoutMillis = 400 }
                }.body<String>()
            }
        }
    }

    @Test
    fun testConnectTimeout() = clientTests(listOf("Js", "iOS", "CIO")) {
        config {
            install(HttpTimeout) { connectTimeoutMillis = 1000 }
        }

        test { client ->
            assertFailsWith<ConnectTimeoutException> {
                client.get("http://www.google.com:81").body<String>()
            }
        }
    }

    @Test
    fun testConnectionRefusedException() = clientTests(listOf("Js", "native:*", "win:*")) {
        config {
            install(HttpTimeout) { connectTimeoutMillis = 1000 }
        }

        test { client ->
            assertFails {
                try {
                    client.get("http://localhost:11").body<String>()
                } catch (_: ConnectTimeoutException) {
                    /* Ignore. */
                }
            }
        }
    }

    @Test
    fun testConnectTimeoutPerRequestAttributes() = clientTests(listOf("Js", "iOS", "CIO")) {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFailsWith<ConnectTimeoutException> {
                client.get("http://www.google.com:81") {
                    timeout { connectTimeoutMillis = 1000 }
                }.body<String>()
            }
        }
    }

    @Test
    fun testSocketTimeoutRead() = clientTests(listOf("Js", "Curl", "native:CIO", "Java")) {
        config {
            install(HttpTimeout) { socketTimeoutMillis = 1000 }
        }

        test { client ->
            assertFailsWith<SocketTimeoutException> {
                client.get("$TEST_URL/with-stream") {
                    parameter("delay", 5000)
                }.body<String>()
            }
        }
    }

    @Test
    fun testSocketTimeoutReadPerRequestAttributes() = clientTests(listOf("Js", "Curl", "native:CIO", "Java")) {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFailsWith<SocketTimeoutException> {
                client.get("$TEST_URL/with-stream") {
                    parameter("delay", 5000)

                    timeout { socketTimeoutMillis = 1000 }
                }.body<String>()
            }
        }
    }

    @Test
    fun testSocketTimeoutWriteFailOnWrite() = clientTests(listOf("Js", "Curl", "Android", "native:CIO")) {
        config {
            install(HttpTimeout) { socketTimeoutMillis = 500 }
        }

        test { client ->
            assertFailsWith<SocketTimeoutException> {
                client.post("$TEST_URL/slow-read") { setBody(makeString(4 * 1024 * 1024)) }
            }
        }
    }

    @Test
    fun testSocketTimeoutWriteFailOnWritePerRequestAttributes() = clientTests(
        listOf("Js", "Curl", "Android", "native:CIO")
    ) {
        config {
            install(HttpTimeout)
        }

        test { client ->
            assertFailsWith<SocketTimeoutException> {
                client.post("$TEST_URL/slow-read") {
                    setBody(makeString(4 * 1024 * 1024))
                    timeout { socketTimeoutMillis = 500 }
                }
            }
        }
    }

    @Test
    fun testNonPositiveTimeout() {
        assertFailsWith<IllegalArgumentException> {
            HttpTimeout.HttpTimeoutCapabilityConfiguration(
                requestTimeoutMillis = -1
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HttpTimeout.HttpTimeoutCapabilityConfiguration(
                requestTimeoutMillis = 0
            )
        }

        assertFailsWith<IllegalArgumentException> {
            HttpTimeout.HttpTimeoutCapabilityConfiguration(
                socketTimeoutMillis = -1
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HttpTimeout.HttpTimeoutCapabilityConfiguration(
                socketTimeoutMillis = 0
            )
        }

        assertFailsWith<IllegalArgumentException> {
            HttpTimeout.HttpTimeoutCapabilityConfiguration(
                connectTimeoutMillis = -1
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HttpTimeout.HttpTimeoutCapabilityConfiguration(
                connectTimeoutMillis = 0
            )
        }
    }

    @Test
    fun testNotInstalledFeatures() = clientTests {
        test { client ->
            assertFailsWith<IllegalArgumentException> {
                client.get("https://www.google.com") {
                    timeout { requestTimeoutMillis = 1000 }
                }.body<String>()
            }
        }
    }
}
