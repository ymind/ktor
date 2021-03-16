/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlin.test.*

class LoggingMockedTests {
    @Test
    fun testLogRequestWithException() = testWithEngine(MockEngine) {
        val testLogger = TestLogger(
            "REQUEST: http://localhost/",
            "METHOD: HttpMethod(value=GET)",
            "COMMON HEADERS",
            "-> Accept: */*",
            "-> Accept-Charset: UTF-8",
            "CONTENT HEADERS",
            "-> Content-Length: 0",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END",
            "REQUEST http://localhost/ failed with exception: CustomError[BAD REQUEST]"
        )

        config {
            engine {
                addHandler {
                    throw CustomError("BAD REQUEST")
                }
            }
            install(Logging) {
                level = LogLevel.ALL
                logger = testLogger
            }
        }

        test { client ->
            var failed = false
            try {
                client.get {}.body<Unit>()
            } catch (_: Throwable) {
                failed = true
            }

            assertTrue(failed, "Exception is missing.")
        }

        after {
            testLogger.verify()
        }
    }

    @Test
    fun testLogResponseWithException() = testWithEngine(MockEngine) {
        val testLogger = TestLogger(
            "REQUEST: http://localhost/",
            "METHOD: HttpMethod(value=GET)",
            "COMMON HEADERS",
            "-> Accept: */*",
            "-> Accept-Charset: UTF-8",
            "CONTENT HEADERS",
            "-> Content-Length: 0",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END",
            "RESPONSE: 200 OK",
            "METHOD: HttpMethod(value=GET)",
            "FROM: http://localhost/",
            "COMMON HEADERS",
            "+++RESPONSE http://localhost/ failed with exception: CustomError[PARSE ERROR]",
            "BODY Content-Type: null",
            "BODY START",
            "Hello",
            "BODY END"
        )

        config {
            engine {
                addHandler { request ->
                    respondOk("Hello")
                }
            }
            install("BadInterceptor") {
                responsePipeline.intercept(HttpResponsePipeline.Parse) {
                    throw CustomError("PARSE ERROR")
                }
            }

            install(Logging) {
                level = LogLevel.ALL
                logger = testLogger
            }
        }

        test { client ->
            if (PlatformUtils.IS_NATIVE) return@test

            var failed = false
            client.prepareGet {}.execute {
                try {
                    it.body<String>()
                } catch (_: CustomError) {
                    failed = true
                }
            }

            assertTrue(failed, "Exception is missing.")
        }

        after {
            if (PlatformUtils.IS_NATIVE) return@after

            testLogger.verify()
        }
    }

    @Test
    fun testLogResponseWithExceptionSingle() = testWithEngine(MockEngine) {
        val testLogger = TestLogger(
            "REQUEST: http://localhost/",
            "METHOD: HttpMethod(value=GET)",
            "COMMON HEADERS",
            "-> Accept: */*",
            "-> Accept-Charset: UTF-8",
            "CONTENT HEADERS",
            "-> Content-Length: 0",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END",
            "RESPONSE: 200 OK",
            "METHOD: HttpMethod(value=GET)",
            "FROM: http://localhost/",
            "COMMON HEADERS",
            "RESPONSE http://localhost/ failed with exception: CustomError[PARSE ERROR]",
            "REQUEST http://localhost/ failed with exception: CustomError[PARSE ERROR]"
        )

        config {
            engine {
                addHandler { request ->
                    respondOk("Hello")
                }
            }
            install("BadInterceptor") {
                receivePipeline.intercept(HttpReceivePipeline.State) {
                    throw CustomError("PARSE ERROR")
                }
            }

            install(Logging) {
                level = LogLevel.ALL
                logger = testLogger
            }
        }

        test { client ->
            var failed = false
            try {
                client.get {}.body<String>()
            } catch (_: CustomError) {
                failed = true
            }

            assertTrue(failed, "Exception is missing.")
        }

        after {
            testLogger.verify()
        }
    }

    @Test
    fun testLoggingWithForm() = testWithEngine(MockEngine) {
        val testLogger = TestLogger(
            "REQUEST: http://localhost/",
            "METHOD: HttpMethod(value=POST)",
            "COMMON HEADERS",
            "-> Accept: */*",
            "-> Accept-Charset: UTF-8",
            "CONTENT HEADERS",
            "!!!-> Content-Type: multipart/form-data; " +
                "boundary=27e7dfaa-451f2057-3dabbd0c2b3cae572a4935af6a57b2d4bb335c34480373360863",
            "!!! BODY Content-Type: multipart/form-data; " +
                "boundary=41a55fb5-2ae7bc4b-5b124e524086ca1e-6879a99a75b8a0a028a6a7d7-63d38251-5",
            "BODY START",
            """!!!
                

                --41a55fb5-2ae7bc4b-5b124e524086ca1e-6879a99a75b8a0a028a6a7d7-63d38251-5
                Content-Disposition: form-data; name=file; file; name=""; filename=""

                Hello
                --41a55fb5-2ae7bc4b-5b124e524086ca1e-6879a99a75b8a0a028a6a7d7-63d38251-5--


            """.trimIndent(),
            "BODY END",
            "RESPONSE: 200 OK",
            "METHOD: HttpMethod(value=POST)",
            "FROM: http://localhost/",
            "COMMON HEADERS",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END"
        )

        config {
            engine {
                addHandler {
                    val body = it.body.toByteReadPacket().readText()
                    assertTrue { body.contains("Hello") }
                    respondOk()
                }
            }

            Logging {
                level = LogLevel.ALL
                logger = testLogger
            }
        }

        test { client ->
            val input = buildPacket { writeText("Hello") }
            client.submitFormWithBinaryData(
                formData {
                    appendInput(
                        "file",
                        headersOf(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.File.withParameter(ContentDisposition.Parameters.Name, "")
                                .withParameter(ContentDisposition.Parameters.FileName, "")
                                .toString()
                        )
                    ) { input }
                }
            ).body<String>()
        }

        after {
            testLogger.verify()
        }
    }

    @Test
    fun testFilterRequest() = testWithEngine(MockEngine) {
        val testLogger = TestLogger(
            "REQUEST: http://somewhere/filtered_path",
            "METHOD: HttpMethod(value=GET)",
            "COMMON HEADERS",
            "-> Accept: */*",
            "-> Accept-Charset: UTF-8",
            "CONTENT HEADERS",
            "-> Content-Length: 0",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END",
            "RESPONSE: 200 OK",
            "METHOD: HttpMethod(value=GET)",
            "FROM: http://somewhere/filtered_path",
            "COMMON HEADERS",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END",
            "RESPONSE: 200 OK",
            "METHOD: HttpMethod(value=GET)",
            "FROM: http://somewhere/not_filtered_path",
            "COMMON HEADERS",
            "BODY Content-Type: null",
            "BODY START",
            "",
            "BODY END"
        )

        config {
            engine {
                addHandler { respondOk() }
            }
            install(Logging) {
                level = LogLevel.ALL
                logger = testLogger
                filter { it.url.encodedPath == "/filtered_path" }
            }
        }

        test { client ->
            client.get(urlString = "http://somewhere/filtered_path").body<String>()
            client.get(urlString = "http://somewhere/not_filtered_path").body<String>()
        }

        after {
            testLogger.verify()
        }
    }
}
