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

/**
 * Represents the type of mDNS service used for ADB over network discovery.
 */
internal enum class MdnsServiceType(val rawValue: String) {
    Pairing("_adb-tls-pairing._tcp."),
    Connect("_adb-tls-connect._tcp."),
    Legacy("_adb._tcp.");

    companion object {

        fun from(raw: String): MdnsServiceType? {
            val normalized = raw.trim().removeSuffix(".")
            return entries.firstOrNull {
                it.rawValue.removeSuffix(".").equals(normalized, ignoreCase = true)
            }
        }
    }
}