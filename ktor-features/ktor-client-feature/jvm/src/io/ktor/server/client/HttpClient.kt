/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.client

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

public class HttpClient internal constructor(
    private val clients: Map<Id, ClientConfig<*>>
) : Closeable {
    public abstract class Id
    public object Default : Id()

    public suspend fun clientFor(id: Id): io.ktor.client.HttpClient =
        clients[id]?.instance?.await() ?: error("No client configured for id $id")

    public suspend fun client(): io.ktor.client.HttpClient =
        (clients[Default] ?: clients.values.singleOrNull())?.instance?.await() ?:
        error("No default client configured")

    internal class ClientConfig<C : HttpClientEngineConfig>(
        val id: Id,
        private val engine: HttpClientEngineFactory<C>?,
        private val configBlock: HttpClientConfig<C>.() -> Unit
    ) {
        internal lateinit var instance: Deferred<io.ktor.client.HttpClient>

        internal fun start(application: Application) {
            instance = application.async(start = CoroutineStart.LAZY) {
                when (engine) {
                    null -> HttpClient {
                        @Suppress("UNCHECKED_CAST")
                        configBlock(this as HttpClientConfig<C>)
                    }
                    else -> HttpClient(engine, configBlock)
                }
            }
        }
    }

    public class Config internal constructor() {
        internal val clients = HashMap<Id, ClientConfig<*>>()

        public fun <C : HttpClientEngineConfig> configure(
            name: Id = Default,
            engine: HttpClientEngineFactory<C>,
            config: HttpClientConfig<C>.() -> Unit = {}
        ) {
            register(name, engine, config)
        }

        public fun configure(
            name: Id = Default,
            config: HttpClientConfig<*>.() -> Unit = {}
        ) {
            register<HttpClientEngineConfig>(name, null, config)
        }

        private fun <C : HttpClientEngineConfig> register(
            name: Id,
            engine: HttpClientEngineFactory<C>?,
            config: HttpClientConfig<C>.() -> Unit
        ) {
            check(name !in clients) { "Client with id $name is already configured" }
            clients[name] = ClientConfig(name, engine, config)
        }
    }

    override fun close() {
        clients.values.forEach {
            it.instance.cancel()
        }

        runBlocking {
            clients.values.forEach {
                try {
                    it.instance.await().close()
                } catch (_: CancellationException) {
                }
            }
        }
    }

    public companion object Feature : ApplicationFeature<Application, Config, HttpClient> {
        override val key: AttributeKey<HttpClient> = AttributeKey("HttpClient")

        override fun install(pipeline: Application, configure: Config.() -> Unit): HttpClient {
            val config = Config().apply(configure)
            val instance = HttpClient(config.clients.toMap())
            instance.clients.values.forEach { it.start(pipeline) }
            return instance
        }
    }
}
