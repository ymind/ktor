import io.ktor.application.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.client.*
import io.ktor.server.testing.*
import kotlin.test.*

/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

class HttpClientFeatureTest {
    @Test
    fun smokeTest() {
        withTestApplication {
            application.install(HttpClient) {
                configure(engine = factoryFor(engine))
            }

            application.routing {
                get("/") {
                    call.respondText(client().get("http://localhost/c?p1=7"))
                }

                get("/c") {
                    call.respondText("OK, ${call.request.queryParameters["p1"]}")
                }
            }

            handleRequest(HttpMethod.Get, "/").let { call ->
                assertEquals("OK, 7", call.response.content)
            }
        }
    }

    private fun <C : HttpClientEngineConfig> factoryFor(
        engine: HttpClientEngine
    ): HttpClientEngineFactory<C> {
        return object : HttpClientEngineFactory<C> {
            override fun create(block: C.() -> Unit): HttpClientEngine {
                @Suppress("UNCHECKED_CAST")
                (engine.config as C).apply(block)
                return engine
            }
        }
    }
}
