/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.request

import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Executes a [HttpClient] GET request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.get(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = get {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] GET request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.getRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = get(url, block)

/**
 * Executes a [HttpClient] POST request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.post(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = post {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] POST request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.postRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = post(url, block)

/**
 * Executes a [HttpClient] PUT request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.put(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = put {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] PUT request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.putRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = put(url, block)

/**
 * Executes a [HttpClient] PATCH request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.patch(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = patch {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] PATCH request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.patchRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = patch(url, block)

/**
 * Executes a [HttpClient] OPTIONS request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.options(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = options {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] OPTIONS request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.optionsRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = options(url, block)

/**
 * Executes a [HttpClient] HEAD request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.head(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = head {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] HEAD request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.headRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = head(url, block)

/**
 * Executes a [HttpClient] HEAD request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 *
 * Tries to receive a specific type [T], if fails, an exception is thrown.
 */
public suspend inline fun <reified T> HttpClient.delete(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = delete {
    this.url.takeFrom(url)
    block()
}

/**
 * Executes a [HttpClient] HEAD request, with the specified [url] as Url and
 * an optional [block] receiving an [HttpRequestBuilder] for further configuring the request.
 */
public suspend fun HttpClient.deleteRaw(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = delete(url, block)

/**
 * Sets the [HttpRequestBuilder.url] from [url].
 */
public fun HttpRequestBuilder.url(url: Url): Unit { // ktlint-disable no-unit-return
    this.url.takeFrom(url)
}
