/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.json

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

@Serializable
internal data class User(val id: Long, val login: String)

@Serializable
internal data class Photo(val id: Long, val path: String)

@Serializable
data class GithubProfile(
    val login: String,
    val id: Int,
    val name: String
)

class KotlinxSerializerTest : ClientLoader() {
    @Test
    fun testRegisterCustom() {
        val serializer = KotlinxSerializer()

        val user = User(1, "vasya")
        val actual = serializer.testWrite(user)
        assertEquals("{\"id\":1,\"login\":\"vasya\"}", actual)
    }

    @Test
    fun testRegisterCustomList() {
        val serializer = KotlinxSerializer()

        val user = User(2, "petya")
        val photo = Photo(3, "petya.jpg")

        assertEquals("[{\"id\":2,\"login\":\"petya\"}]", serializer.testWrite(listOf(user)))
        assertEquals("[{\"id\":3,\"path\":\"petya.jpg\"}]", serializer.testWrite(listOf(photo)))
    }

    @Test
    fun testCustomFormBody() = clientTests {
        config {
            install(JsonFeature)
        }

        val data = {
            formData {
                append("name", "hello")
                append("content") {
                    writeText("123456789")
                }
                append("file", "urlencoded_name.jpg") {
                    for (i in 1..4096) {
                        writeByte(i.toByte())
                    }
                }
                append("hello", 5)
            }
        }

        test { client ->
            var throwed = false
            try {
                client.submitFormWithBinaryData(url = "upload", formData = data()).body<String>()
            } catch (cause: Throwable) {
                throwed = true
            }

            assertTrue(throwed, "Connection exception expected.")
        }
    }

    @Test
    fun testStringWithJsonFeature() = clientTests {
        config {
            install(JsonFeature)
            defaultRequest {
                val contentType = ContentType.parse("application/vnd.string+json")
                accept(contentType)
                contentType(contentType)
            }
        }

        test { client ->
            val response = client.post("$TEST_SERVER/echo-with-content-type") {
                setBody("Hello")
            }.body<String>()
            assertEquals("\"Hello\"", response)

            val textResponse = client.post("$TEST_SERVER/echo") {
                setBody("Hello")
            }.body<String>()
            assertEquals("\"Hello\"", textResponse)

            val emptyResponse = client.post("$TEST_SERVER/echo").body<String>()
            assertEquals("", emptyResponse)
        }
    }

    @Test
    fun testMultipleListSerializersWithClient() = clientTests {
        val testSerializer = KotlinxSerializer()

        config {
            install(JsonFeature) {
                serializer = testSerializer
            }
            defaultRequest {
                accept(ContentType.Application.Json)
            }
        }

        test { client ->
            val users = client.get("$TEST_SERVER/json/users").body<List<User>>()
            val photos = client.get("$TEST_SERVER/json/photos").body<List<Photo>>()

            assertEquals(listOf(User(42, "TestLogin")), users)
            assertEquals(listOf(Photo(4242, "cat.jpg")), photos)
        }
    }

    private fun JsonSerializer.testWrite(data: Any): String =
        (write(data, ContentType.Application.Json) as? TextContent)?.text ?: error("Failed to get serialized $data")
}
