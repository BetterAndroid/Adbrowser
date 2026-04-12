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
package com.highcapable.adbrowser.core.adb

import com.highcapable.adbrowser.core.adb.model.AdbResponse
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.model.OperationResult
import kotlinx.coroutines.flow.Flow

/**
 * Defines ADB communication capabilities used by upper layers.
 */
interface AdbClient {

    /**
     * Provides the current adb executable path.
     */
    var execPath: () -> String

    /**
     * Verifies whether the configured adb executable path is valid and executable.
     * @param pathValue the adb executable path to validate, or blank to validate the currently configured path.
     */
    suspend fun validateExecPath(pathValue: String = execPath()): OperationResult<Unit>

    /**
     * Lists connected devices and their online states.
     */
    suspend fun listDevices(): OperationResult<List<AndroidDevice>>

    /**
     * Observes device list changes and emits the newest snapshot when changed.
     *
     * - `pollIntervalMillis` controls backend-side polling cadence for change detection.
     */
    fun observeDevices(pollIntervalMillis: Long = 1500L): Flow<OperationResult<List<AndroidDevice>>>

    /**
     * Executes an adb command for the target device.
     */
    suspend fun executeCommand(device: AndroidDevice, vararg command: String): AdbResponse
}