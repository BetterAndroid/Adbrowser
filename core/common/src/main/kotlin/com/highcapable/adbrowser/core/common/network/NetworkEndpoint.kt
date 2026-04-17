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
 * This file is created by fankes on 2026/4/18.
 */
package com.highcapable.adbrowser.core.common.network

/**
 * Represents a normalized network endpoint.
 *
 * Host values are stored without square brackets. Use [address] when a normalized `host:port`
 * string is needed for display, persistence, or adb command arguments.
 */
data class NetworkEndpoint(val host: String, val port: Int) {

    companion object {

        const val DEFAULT_ADB_PORT = 5555

        /**
         * Parses a user-facing endpoint string.
         *
         * - When [defaultPort] is non-null, inputs without an explicit port are accepted and filled.
         * - When [defaultPort] is null, an explicit port is required.
         */
        fun parse(value: String, defaultPort: Int? = null): NetworkEndpoint? {
            val normalized = value.trim()
            if (normalized.isEmpty()) return null

            if (normalized.startsWith('[') && normalized.endsWith(']')) {
                val host = normalized.removePrefix("[").removeSuffix("]").trim()
                val port = defaultPort ?: return null
                if (host.isEmpty()) return null
                return NetworkEndpoint(host, port)
            }

            if (!normalized.contains(':')) {
                val port = defaultPort ?: return null
                return NetworkEndpoint(normalized, port)
            }

            if (normalized.count { it == ':' } > 1 && !normalized.startsWith('[')) {
                val lastSegment = normalized.substringAfterLast(':')
                val hasExplicitPort = lastSegment.toIntOrNull()?.let { it in 1..65535 } == true

                if (!hasExplicitPort) {
                    val port = defaultPort ?: return null
                    return NetworkEndpoint(normalized, port)
                }
            }

            val separatorIndex = normalized.lastIndexOf(':')
            if (separatorIndex <= 0 || separatorIndex >= normalized.lastIndex) return null

            val rawHost = normalized.take(separatorIndex).trim()
            val host = rawHost.removePrefix("[").removeSuffix("]")
            val port = normalized.substring(separatorIndex + 1).toIntOrNull() ?: return null
            if (host.isEmpty()) return null

            return NetworkEndpoint(host, port)
        }

        /**
         * Parses and immediately returns the normalized `host:port` form.
         */
        fun normalize(value: String, defaultPort: Int? = null) = parse(value, defaultPort)?.address
    }

    init {
        require(host.isNotBlank()) { "Host cannot be blank." }
        require(port in 1..65535) { "Port must be between 1 and 65535." }
    }

    /**
     * Returns the normalized `host:port` representation.
     *
     * IPv6 literals are wrapped in square brackets so the result is unambiguous and can be reused
     * directly as an adb serial / connect target.
     */
    val address = buildString {
        if (host.contains(':')) append("[$host]") else append(host)
        append(':')
        append(port)
    }
}