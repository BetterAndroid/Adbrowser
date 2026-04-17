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
 * This file is created by fankes on 2026/4/2.
 */
package com.highcapable.adbrowser.core.adb.model

/**
 * Represents a connected Android device discovered by ADB.
 */
data class AndroidDevice(
    val name: String,
    val brand: String,
    val model: String,
    val systemVersion: String,
    val serial: String,
    val isOnline: Boolean,
    val type: Type
) {

    /**
     * Describes how the ADB transport is exposed to the host.
     *
     * AOSP itself mainly distinguishes USB and LOCAL transports. We split LOCAL further into
     * [Type.Emulator] and [Type.Network] based on the reported serial so the app can react differently
     * to emulators and ADB-over-network devices.
     */
    enum class Type {
        Usb,
        Emulator,
        Network
    }

    override fun equals(other: Any?) = when (other) {
        is AndroidDevice -> this.serial == other.serial
        else -> super.equals(other)
    }

    override fun hashCode() = serial.hashCode()

    override fun toString() = "$name ($brand $model) - $serial"
}