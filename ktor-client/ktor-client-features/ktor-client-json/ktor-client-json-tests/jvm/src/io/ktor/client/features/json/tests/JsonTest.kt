/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")

package io.ktor.client.features.json.tests

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.client.utils.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import kotlinx.serialization.*
import kotlin.test.*

/** Base class for JSON tests. */
@Suppress("KDocMissingDocumentation")
public abstract class JsonTest : TestWithKtor() {
    private val widget = Widget("Foo", 1000, listOf("bar", "baz", "qux"))
    private val users = listOf(
        User("vasya", 10),
        User("foo", 45)
    )

    override val server: ApplicationEngine = embeddedServer(io.ktor.server.cio.CIO, serverPort) {
        install(ContentNegotiation) {
            gson()
            gson(customContentType)
        }
        routing {
            createRoutes(this)
        }
    }

    private val customContentType = ContentType.parse("application/x-json")

    protected open fun createRoutes(routing: Routing): Unit = with(routing) {
        post("/widget") {
            val received = call.receive<Widget>()
            assertEquals(widget, received)
            call.respond(widget)
        }
        get("/users") {
            call.respond(Response(true, users))
        }
        get("/users-x") { // route for testing custom content type, namely "application/x-json"
            call.respond(Response(true, users))
        }
        post("/post-x") {
            require(call.request.contentType().withoutParameters() == customContentType) {
                "Request body content type should be $customContentType"
            }

            val requestPayload = call.receive<User>()
            call.respondText(requestPayload.toString())
        }
    }

    protected abstract val serializerImpl: JsonSerializer?

    protected fun TestClientBuilder<*>.configClient() {
        config {
            configJsonFeature()
        }
    }

    private fun HttpClientConfig<*>.configJsonFeature(block: JsonFeature.Config.() -> Unit = {}) {
        install(JsonFeature) {
            serializerImpl?.let {
                serializer = it
            }
            block()
        }
    }

    private fun TestClientBuilder<*>.configCustomContentTypeClient(block: JsonFeature.Config.() -> Unit) {
        config {
            configJsonFeature(block)
        }
    }

    @org.junit.Test
    public fun testEmptyBody() = testWithEngine(MockEngine) {
        config {
            engine {
                addHandler { request ->
                    respond(
                        request.body.toByteReadPacket().readText(),
                        headers = buildHeaders {
                            append("X-ContentType", request.body.contentType.toString())
                        }
                    )
                }
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
            configJsonFeature()
        }

        test { client ->
            val response: HttpResponse = client.get("https://test.com")
            assertEquals("", response.readText())
            assertEquals("null", response.headers["X-ContentType"])
        }
    }

    @Test
    public fun testSerializeSimple() = testWithEngine(CIO) {
        configClient()

        test { client ->
            val result = client.post {
                setBody(widget)
                url(path = "/widget", port = serverPort)
                contentType(ContentType.Application.Json)
            }.body<Widget>()

            assertEquals(widget, result)
        }
    }

    @Test
    public fun testSerializeNested() = testWithEngine(CIO) {
        configClient()

        test { client ->
            val result = client.get { url(path = "/users", port = serverPort) }.body<Response<List<User>>>()

            assertTrue(result.ok)
            assertNotNull(result.result)
            assertEquals(users, result.result)
        }
    }

    @Test
    public fun testCustomContentTypes() = testWithEngine(CIO) {
        configCustomContentTypeClient {
            acceptContentTypes = listOf(customContentType)
        }

        test { client ->
            val result = client.get { url(path = "/users-x", port = serverPort) }.body<Response<List<User>>>()

            assertTrue(result.ok)
            assertNotNull(result.result)
            assertEquals(users, result.result)
        }

        test { client ->
            client.prepareGet { url(path = "/users-x", port = serverPort) }.execute { response ->
                val result = response.body<Response<List<User>>>()

                assertTrue(result.ok)
                assertNotNull(result.result)
                assertEquals(users, result.result)

                assertEquals(customContentType, response.contentType()?.withoutParameters())
            }
        }

        test { client ->
            val payload = User("name1", 99)

            val result = client.post {
                url(path = "/post-x", port = serverPort)
                setBody(payload)
                contentType(customContentType)
            }.body<String>()

            assertEquals(payload.toString(), result)
        }
    }

    @Test
    public fun testCustomContentTypesMultiple() = testWithEngine(CIO) {
        configCustomContentTypeClient {
            acceptContentTypes = listOf(ContentType.Application.Json, customContentType)
        }

        test { client ->
            val payload = User("name2", 98)

            val result = client.post {
                url(path = "/post-x", port = serverPort)
                setBody(payload)
                contentType(customContentType)
            }.body<String>()

            assertEquals(payload.toString(), result)
        }
    }

    @Test
    public fun testCustomContentTypesWildcard() = testWithEngine(CIO) {
        configCustomContentTypeClient {
            acceptContentTypes = listOf(ContentType.Application.Any)
        }

        test { client ->
            client.prepareGet { url(path = "/users-x", port = serverPort) }.execute { response ->
                val result = response.body<Response<List<User>>>()

                assertTrue(result.ok)
                assertNotNull(result.result)
                assertEquals(users, result.result)

                // json is registered first on server so it should win
                // since Accept header consist of the wildcard
                assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            }
        }

        test { client ->
            val payload = User("name3", 97)

            val result = client.post {
                url(path = "/post-x", port = serverPort)
                setBody(payload)
                contentType(customContentType) // custom content type should match the wildcard
            }.body<String>()

            assertEquals(payload.toString(), result)
        }
    }

    @Serializable
    public data class Response<T>(
        val ok: Boolean,
        val result: T?
    )

    @Serializable
    public data class Widget(
        val name: String,
        val value: Int,
        val tags: List<String> = emptyList()
    )

    @Serializable
    public data class User(
        val name: String,
        val age: Int
    )
}
