/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.websocket

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*

/**
 * Create raw [ClientWebSocketSession]: no ping-pong and other service messages are used.
 */
public suspend fun HttpClient.webSocketRawSession(
    method: HttpMethod = HttpMethod.Get,
    host: String = "localhost",
    port: Int = DEFAULT_PORT,
    path: String = "/",
    block: HttpRequestBuilder.() -> Unit = {}
): ClientWebSocketSession = request {
    this.method = method
    url("ws", host, port, path)
    block()
}.body()

/**
 * Create raw [ClientWebSocketSession]: no ping-pong and other service messages are used.
 */
public suspend fun HttpClient.webSocketRaw(
    method: HttpMethod = HttpMethod.Get,
    host: String = "localhost",
    port: Int = DEFAULT_PORT,
    path: String = "/",
    request: HttpRequestBuilder.() -> Unit = {},
    block: suspend ClientWebSocketSession.() -> Unit
): Unit { // ktlint-disable filename no-unit-return
    val session = webSocketRawSession(method, host, port, path) {
        url.protocol = URLProtocol.WS
        url.port = port

        request()
    }

    try {
        session.block()
    } catch (cause: Throwable) {
        session.closeExceptionally(cause)
    } finally {
        session.close()
    }
}

/**
 * Create raw [ClientWebSocketSession]: no ping-pong and other service messages are used.
 */
public suspend fun HttpClient.wsRaw(
    method: HttpMethod = HttpMethod.Get,
    host: String = "localhost",
    port: Int = DEFAULT_PORT,
    path: String = "/",
    request: HttpRequestBuilder.() -> Unit = {},
    block: suspend ClientWebSocketSession.() -> Unit
): Unit = webSocketRaw(method, host, port, path, request, block)

/**
 * Open [DefaultClientWebSocketSession].
 */
@Deprecated(
    message = "This method is deprecated. Use one from :ktor-client:ktor-client-core",
    level = DeprecationLevel.HIDDEN
)
public suspend fun HttpClient.ws(
    urlString: String,
    request: HttpRequestBuilder.() -> Unit = {},
    block: suspend DefaultClientWebSocketSession.() -> Unit
): Unit { // ktlint-disable filename no-unit-return
    val url = Url(urlString)
    webSocket(HttpMethod.Get, url.host, url.port, url.encodedPath, request, block)
}

/**
 * Create secure raw [ClientWebSocketSession]: no ping-pong and other service messages are used.
 */
public suspend fun HttpClient.wssRaw(
    method: HttpMethod = HttpMethod.Get,
    host: String = "localhost",
    port: Int = DEFAULT_PORT,
    path: String = "/",
    request: HttpRequestBuilder.() -> Unit = {},
    block: suspend ClientWebSocketSession.() -> Unit
): Unit = webSocketRaw(
    method,
    host,
    port,
    path,
    request = {
        url.protocol = URLProtocol.WSS
        url.port = port

        request()
    },
    block = block
)
