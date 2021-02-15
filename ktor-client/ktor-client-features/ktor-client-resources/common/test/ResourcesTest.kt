/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.client.engine.mock.*
import io.ktor.client.features.resources.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.resources.*
import kotlinx.serialization.*
import kotlin.test.*

class ResourcesTest {

    @Serializable
    @Resource("path/{id}/{method}")
    class Path(val id: Long, val method: String) {
        @Serializable
        @Resource("child/{path?}")
        data class Child(val parent: Path, val path: String, val query: List<Int>)
    }

    @Test
    fun testRequest() = testWithEngine(MockEngine) {
        config {
            engine {
                addHandler { request ->
                    val uri = request.url.fullPath
                    val method = request.method.value
                    assertEquals(method, uri.split('/')[3])
                    assertEquals("/path/123/$method/child/value?query=1&query=2&query=3&query=4", uri)
                    respondOk(uri)
                }
            }
            install(Resources)
        }

        test { client ->
            val response1: String = client.get(Path.Child(Path(123, "GET"), "value", listOf(1, 2, 3, 4)))
            val response2: String = client.post(Path.Child(Path(123, "POST"), "value", listOf(1, 2, 3, 4)))
            val response3: String = client.put(Path.Child(Path(123, "PUT"), "value", listOf(1, 2, 3, 4)))
            val response4: String = client.delete(Path.Child(Path(123, "DELETE"), "value", listOf(1, 2, 3, 4)))
            val response5: String = client.options(Path.Child(Path(123, "OPTIONS"), "value", listOf(1, 2, 3, 4)))
        }
    }

    @Serializable
    @Resource("path/{id}/{value?}")
    class PathWithDefault(val id: Boolean = true, val value: String? = null, val query1: Int?, val query2: Int? = 5)

    @Test
    fun testRequestWithDefaults() = testWithEngine(MockEngine) {
        config {
            engine {
                addHandler { request ->
                    val uri = request.url.fullPath
                    assertEquals("/path/true?query2=5", uri)
                    respondOk(uri)
                }
            }
            install(Resources)
        }

        test { client ->
            val response = client.get<PathWithDefault, String>(PathWithDefault(query1 = null))
        }
    }
}
