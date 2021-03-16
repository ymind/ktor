/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.cio

import io.ktor.application.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.junit4.*
import org.junit.*
import java.nio.channels.*
import kotlin.test.*
import kotlin.test.Test

class CIORequestTest : TestWithKtor() {
    private val testSize = 2 * 1024

    @get:Rule
    override val timeout = CoroutinesTimeout.seconds(10)

    override val server: ApplicationEngine = embeddedServer(Netty, serverPort) {
        routing {
            get("/") {
                val longHeader = call.request.headers["LongHeader"]!!
                call.respond(
                    object : OutgoingContent.NoContent() {
                        override val headers: Headers = headersOf("LongHeader", longHeader)
                    }
                )
            }
            get("/echo") {
                call.respond("OK")
            }

            get("/delay") {
                delay(1000)
                call.respond("OK")
            }
        }
    }

    @Test
    fun engineUsesRequestTimeoutFromItsConfiguration() {
        testWithEngine(CIO) {
            config {
                engine {
                    requestTimeout = 10
                }
            }

            test { client ->
                assertFailsWith<TimeoutCancellationException> {
                    client.get<HttpStatement>(path = "/delay", port = serverPort).execute()
                }
            }
        }
    }

    @Test
    fun requestTimeoutFromHttpTimeoutFeatureIsUsedAndHasHigherPriorityThanFromConfiguration() {
        testWithEngine(CIO) {
            config {
                engine {
                    requestTimeout = 10
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 200
                }
            }

            test { client ->
                assertFailsWith<HttpRequestTimeoutException> {
                    client.get<HttpStatement>(path = "/delay", port = serverPort).execute()
                }
            }
        }
    }

    @Test
    fun longHeadersTest() = testWithEngine(CIO) {
        test { client ->
            val headerValue = "x".repeat(testSize)

            client.prepareGet {
                url(port = serverPort)
                header("LongHeader", headerValue)
            }.execute { response ->
                assertEquals(headerValue, response.headers["LongHeader"])
            }
        }
    }

    @Test
    fun testHangingTimeoutWithWrongUrl() = testWithEngine(CIO) {
        config {
            engine {
                endpoint {
                    connectTimeout = 1
                }
            }
        }

        test { client ->
            for (i in 0..1000) {
                try {
                    client.get("http://something.wrong").body<String>()
                } catch (cause: UnresolvedAddressException) {
                    // ignore
                }
            }
        }
    }
}
