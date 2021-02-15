/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.resources

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.resources.*

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.get(
    resource: T,
): R = get(resource) {}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.get(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return get<R> {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.post(
    resource: T,
): R = post(resource) {}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.post(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return post<R> {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.put(
    resource: T,
): R = put(resource) {}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.put(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return put<R> {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.delete(
    resource: T,
): R = delete(resource) {}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.delete(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return delete<R> {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.options(
    resource: T,
): R = options(resource) {}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.options(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return options<R> {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.head(
    resource: T,
): R = head(resource) {}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.head(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return head<R> {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}

public suspend inline fun <reified T : Any, reified R : Any> HttpClient.request(
    resource: T,
    builder: HttpRequestBuilder.() -> Unit
): R {
    val resources = this[Resources]
    return request {
        href(resources.resourcesFormat, resource, url)
        builder()
    }
}
