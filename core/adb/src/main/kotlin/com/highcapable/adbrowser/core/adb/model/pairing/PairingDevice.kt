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
package com.highcapable.adbrowser.core.adb.model.pairing

import com.highcapable.adbrowser.core.common.network.NetworkEndpoint

/**
 * Represents an ADB wireless-debugging pairing endpoint discovered through `adb mdns services`.
 */
data class PairingDevice(
    val serviceName: String,
    val host: String,
    val port: Int
) {

    /**
     * Returns the normalized `host:port` form used by `adb pair`.
     */
    val address = NetworkEndpoint(host, port).address
}