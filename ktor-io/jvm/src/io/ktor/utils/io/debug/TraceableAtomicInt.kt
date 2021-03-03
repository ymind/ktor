/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.debug

import kotlinx.atomicfu.*
import java.util.*

internal class Operation(val description: String, val stack: Array<StackTraceElement>) {
    override fun toString(): String = description
}

internal class TraceableAtomicInt(
    initialValue: Int = 0,
    private val captureStack: Boolean = false,
    private val maxLogSize: Int = Int.MAX_VALUE
) {
    private val _log: MutableList<Operation> = Collections.synchronizedList(mutableListOf())
    private val _value = atomic(initialValue)

    val log: List<Operation> get() = _log

    var value: Int
        get() = _value.value
        set(value) {
            writeLog("Set value ${_value.value} -> $value")
            _value.value = value
        }

    init {
        writeLog("Init $initialValue")
    }

    private fun writeLog(value: String) {
        val stack = if (captureStack) {
            Exception().fillInStackTrace().stackTrace
        } else emptyArray()

        _log += Operation(value, stack)

        if (_log.size > maxLogSize) {
            _log.removeAt(0)
        }
    }

    inline fun getAndUpdate(block: (Int) -> Int): Int {
        return _value.getAndUpdate {
            val result = block(it)
            writeLog("getAndUpdate $it -> $result")
            result
        }
    }

    inline fun update(block: (Int) -> Int): Int {
        return _value.updateAndGet {
            val result = block(it)
            writeLog("update $it -> $result")
            result
        }
    }

    fun getAndSet(value: Int): Int {
        val result = _value.getAndSet(value)
        writeLog("getAndSet; old: $result, new: $value")
        return result
    }
}
