/*
 * Adbrowser - A modern cross-platform Android file manager powered by ADB.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/Adbrowser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2026/4/23.
 */
package com.highcapable.adbrowser.core.common.shell

/**
 * Collects shell tokens and builds one command string with consistent quoting rules.
 *
 * Two usage styles are intentionally supported:
 *
 * `val command = ShellArguments.create().add("ls", "-la").addQuotes(path).build()`
 *
 * `val command = ShellArguments { add("ls", "-la"); addQuotes(path) }`
 */
class ShellArguments private constructor() {

    companion object {

        operator fun invoke(block: ShellArguments.() -> Unit) =
            create().apply(block).build()

        fun create() = ShellArguments()

        /**
         * Wraps one shell argument in single quotes.
         *
         * POSIX single-quoted strings already preserve backslashes literally, so we only need to
         * break/reopen the literal around inner single quotes. Escaping backslashes here would
         * alter the original payload and break paths such as `a\b`.
         */
        internal fun quote(value: Any?): String {
            val text = value?.toString() ?: ""
            return "'${text.replace("'", "'\\''")}'"
        }
    }

    private val segments = mutableListOf<String>()

    fun add(vararg values: Any?) = apply {
        values.forEach(::appendRawValue)
    }

    /**
     * Semantically identical to [add], but kept for shell grammar fragments such as `;`, `then`,
     * or `fi`, which improves readability at call sites that mix command tokens with shell syntax.
     */
    fun addRaw(vararg values: Any?) = apply {
        values.forEach(::appendRawValue)
    }

    fun addQuotes(vararg values: Any?) = apply {
        values.forEach { value ->
            segments += quote(value)
        }
    }

    fun build() = segments.joinToString(" ")

    private fun appendRawValue(value: Any?) {
        if (value == null) return

        val token = value.toString()
        if (token.isBlank()) return
        segments += token
    }
}