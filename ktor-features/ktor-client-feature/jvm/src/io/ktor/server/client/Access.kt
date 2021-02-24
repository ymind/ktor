/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.client

import io.ktor.application.*
import io.ktor.util.pipeline.*

public suspend fun PipelineContext<*, ApplicationCall>.client(): io.ktor.client.HttpClient {
    return call.application.feature(HttpClient).client()
}

public suspend fun PipelineContext<*, ApplicationCall>.client(name: HttpClient.Id): io.ktor.client.HttpClient {
    return call.application.feature(HttpClient).clientFor(name)
}
