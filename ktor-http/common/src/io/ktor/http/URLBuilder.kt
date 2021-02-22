/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http

/**
 * Select default port value from protocol.
 */
public const val DEFAULT_PORT: Int = 0

/**
 * A URL builder with all mutable components
 *
 * @property protocol URL protocol (scheme)
 * @property host name without port (domain)
 * @property port port number
 * @property user username part (optional)
 * @property password password part (optional)
 * @property pathSegments URL path without query
 * @property parameters URL query parameters
 * @property fragment URL fragment (anchor name)
 * @property trailingQuery keep a trailing question character even if there are no query parameters
 */
public class URLBuilder(
    public var protocol: URLProtocol = URLProtocol.HTTP,
    public var host: String = "localhost",
    public var port: Int = DEFAULT_PORT,
    user: String? = null,
    password: String? = null,
    pathSegments: List<String> = mutableListOf(""),
    parameters: Parameters = Parameters.Empty,
    fragment: String = "",
    public var trailingQuery: Boolean = false
) {

    public var user: String?
        get() = encodedUser?.decodeURLPart()
        set(value) {
            encodedUser = value?.encodeURLParameter()
        }

    public var password: String?
        get() = encodedPassword?.decodeURLPart()
        set(value) {
            encodedPassword = value?.encodeURLParameter()
        }

    public var fragment: String
        get() = encodedFragment.decodeURLQueryComponent()
        set(value) {
            encodedFragment = value.encodeURLQueryComponent()
        }

    public var pathSegments: List<String>
        get() = _encodedPathSegments.map { it.decodeURLPart() }
        set(value) {
            encodedPathSegments = value.map { it.encodeURLPath() }
        }

    public var parameters: Parameters
        get() = decodeParameters()
        set(value) {
            encodedParameters = encodeParameters(value)
        }

    public var encodedUser: String?
    public var encodedPassword: String?
    public var encodedFragment: String
    public var encodedParameters: ParametersBuilder
    internal var _encodedPathSegments: MutableList<String>
    public var encodedPathSegments: List<String>
        get() = _encodedPathSegments
        set(value) {
            _encodedPathSegments = value.toMutableList()
        }

    init {
        originHost?.let { takeFrom(it) }
        encodedPassword = password?.encodeURLParameter()
        encodedUser = user?.encodeURLParameter()
        encodedFragment = fragment.encodeURLQueryComponent()
        _encodedPathSegments = pathSegments.map { it.encodeURLPath() }.toMutableList()
        encodedParameters = encodeParameters(parameters)
    }

    /**
     * Build a URL string
     */
    // note: 256 should fit 99.5% of all urls according to http://www.supermind.org/blog/740/average-length-of-a-url-part-2
    public fun buildString(): String = appendTo(StringBuilder(256)).toString()

    /**
     * Build a [Url] instance (everything is copied to a new instance)
     */
    public fun build(): Url = Url(
        protocol = protocol,
        host = host,
        specifiedPort = port,
        pathSegments = pathSegments,
        parameters = parameters,
        fragment = fragment,
        user = user,
        password = password,
        trailingQuery = trailingQuery,
        urlString = buildString()
    )

    // Required to write external extension function
    public companion object
}

private fun <A : Appendable> URLBuilder.appendTo(out: A): A {
    out.append(protocol.name)

    when (protocol.name) {
        "file" -> {
            out.appendFile(host, encodedPath)
            return out
        }
        "mailto" -> {
            out.appendMailto(encodedUserAndPassword, host)
            return out
        }
    }

    out.append("://")
    out.append(authority)

    out.appendUrlFullPath(encodedPath, encodedParameters, trailingQuery)

    if (encodedFragment.isNotEmpty()) {
        out.append('#')
        out.append(encodedFragment)
    }

    return out
}

private fun Appendable.appendMailto(encodedUser: String, host: String) {
    append(":")
    append(encodedUser)
    append(host)
}

private fun Appendable.appendFile(host: String, encodedPath: String) {
    append("://")
    append(host)
    if (!encodedPath.startsWith('/')) {
        append('/')
    }
    append(encodedPath)
}

/**
 * Hostname of current origin.
 *
 * It uses "localhost" for all platforms except js.
 */
internal expect val URLBuilder.Companion.originHost: String?

/**
 * Create a copy of this builder. Modifications in a copy is not reflected in the original instance and vise-versa.
 */
public fun URLBuilder.clone(): URLBuilder = URLBuilder().takeFrom(this)


internal val URLBuilder.encodedUserAndPassword: String
    get() = buildString {
        appendUserAndPassword(encodedUser, encodedPassword)
    }

/**
 * Adds [components] to current [encodedPath]
 */
public fun URLBuilder.appendPathSegments(segments: List<String>): URLBuilder {
    val paths = segments
        .map { part -> part.dropWhile { it == '/' }.dropLastWhile { it == '/' }.encodeURLPath() }
        .filter { it.isNotEmpty() }

    _encodedPathSegments.addAll(paths)

    return this
}

/**
 * Adds [components] to current [encodedPath]
 */
public fun URLBuilder.appendPathSegments(vararg components: String): URLBuilder {
    return appendPathSegments(components.toList())
}

@Deprecated("Please assign to [pathSegments] directly", replaceWith = ReplaceWith("pathSegments = listOf(path)"))
public fun URLBuilder.path(vararg path: String) {
    pathSegments = path.toList()
}

/**
 * [URLBuilder] authority.
 */
public val URLBuilder.authority: String
    get() = buildString {
        append(encodedUserAndPassword)
        append(host)

        if (port != DEFAULT_PORT && port != protocol.defaultPort) {
            append(":")
            append(port.toString())
        }
    }

public var URLBuilder.encodedPath: String
    get() {
        val path = encodedPathSegments.joinToString("/")
        return if (encodedPathSegments.isEmpty() || path.startsWith('/')) path else "/$path"
    }
    set(value) {
        encodedPathSegments = when (value) {
            "" -> mutableListOf()
            "/" -> mutableListOf("")
            else -> {
                val segments = value.split('/')
                segments.toMutableList()
            }
        }
    }

/**
 * Adds query parameter with [name] and [value]
 */
public fun URLBuilder.appendQueryParameter(name: String, value: String?) {
    appendQueryParameters(name, value?.let { listOf(it) } ?: emptyList())
}

/**
 * Adds query parameter with [name] and [values]
 */
public fun URLBuilder.appendQueryParameters(name: String, values: List<String>) {
    encodedParameters.appendAll(
        name.encodeURLParameter(spaceToPlus = true),
        values.map { it.encodeURLParameterValue() }
    )
}

/**
 * Adds query parameter with [name] and [values]
 */
public fun URLBuilder.appendQueryParameters(parameters: Parameters) {
    encodedParameters.appendAll(
        encodeParameters(parameters).build()
    )
}

/**
 * Removes query parameter with [name] and [value] or all parameters with [name] if [value] is `null`
 */
public fun URLBuilder.removeQueryParameter(name: String, value: String? = null) {
    if (value == null) {
        encodedParameters.remove(name.encodeURLParameter())
    } else {
        encodedParameters.remove(name.encodeURLParameter(), value.encodeURLParameterValue())
    }
}

private fun URLBuilder.decodeParameters(): Parameters = parametersOf(
    *encodedParameters.entries()
        .map { (key, values) ->
            key.decodeURLQueryComponent() to values.map { it.decodeURLQueryComponent(plusIsSpace = true) }
        }.toTypedArray()
)

private fun encodeParameters(parameters: Parameters): ParametersBuilder = ParametersBuilder().apply {
    parameters.entries()
        .map { (key, values) ->
            key.encodeURLParameter() to values.map { it.encodeURLParameterValue() }
        }
        .forEach { (key, values) ->
            appendAll(key, values)
        }
}
