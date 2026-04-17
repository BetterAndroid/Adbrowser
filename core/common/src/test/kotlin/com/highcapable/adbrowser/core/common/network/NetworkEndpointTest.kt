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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkEndpointTest {

    @Test
    fun parseAddsDefaultPortWhenInputOmitsIt() {
        val endpoint = assertNotNull(
            NetworkEndpoint.parse(
                value = "192.168.0.30",
                defaultPort = NetworkEndpoint.DEFAULT_ADB_PORT
            )
        )

        assertEquals("192.168.0.30", endpoint.host)
        assertEquals(NetworkEndpoint.DEFAULT_ADB_PORT, endpoint.port)
        assertEquals("192.168.0.30:5555", endpoint.address)
    }

    @Test
    fun parseKeepsExplicitPortWhenProvided() {
        val endpoint = assertNotNull(
            NetworkEndpoint.parse(
                value = "192.168.0.30:37123",
                defaultPort = NetworkEndpoint.DEFAULT_ADB_PORT
            )
        )

        assertEquals("192.168.0.30", endpoint.host)
        assertEquals(37123, endpoint.port)
        assertEquals("192.168.0.30:37123", endpoint.address)
    }

    @Test
    fun parseNormalizesIpv6WithoutExplicitPortUsingDefault() {
        val endpoint = assertNotNull(
            NetworkEndpoint.parse(
                value = "fe80::1234",
                defaultPort = NetworkEndpoint.DEFAULT_ADB_PORT
            )
        )

        assertEquals("fe80::1234", endpoint.host)
        assertEquals(NetworkEndpoint.DEFAULT_ADB_PORT, endpoint.port)
        assertEquals("[fe80::1234]:5555", endpoint.address)
    }

    @Test
    fun parseRequiresExplicitPortWhenNoDefaultIsProvided() {
        assertNull(NetworkEndpoint.parse("192.168.0.30"))
        assertNull(NetworkEndpoint.parse("[fe80::1234]"))
    }
}