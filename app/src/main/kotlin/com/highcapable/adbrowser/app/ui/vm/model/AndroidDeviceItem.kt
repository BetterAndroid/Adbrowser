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
 * This file is created by fankes on 2026/4/3.
 */
package com.highcapable.adbrowser.app.ui.vm.model

import com.highcapable.adbrowser.core.adb.model.AndroidDevice

data class AndroidDeviceItem(
    val name: String,
    val brand: String,
    val model: String,
    val serial: String,
    val systemVersion: String,
    val isOnline: Boolean,
    val type: AndroidDevice.Type
) {

    companion object {

        fun from(device: AndroidDevice) = AndroidDeviceItem(
            name = device.name,
            brand = device.brand,
            model = device.model,
            serial = device.serial,
            systemVersion = device.systemVersion,
            isOnline = device.isOnline,
            type = device.type
        )
    }

    val brandModel = "$brand $model".trim().ifBlank { "Unknown" }

    fun toDomain() = AndroidDevice(
        serial = serial,
        name = name,
        brand = brand,
        model = model,
        systemVersion = systemVersion,
        isOnline = isOnline,
        type = type
    )

    override fun equals(other: Any?) = when (other) {
        // Synchronous UI refresh to identify online status matches
        // must be the same device, otherwise the device status may not be refreshed at all.
        is AndroidDeviceItem -> this.serial == other.serial &&
            this.isOnline == other.isOnline
        else -> super.equals(other)
    }

    override fun hashCode() = serial.hashCode() + isOnline.hashCode()
}