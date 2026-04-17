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
import com.highcapable.adbrowser.core.adb.model.pairing.PairingDevice
import com.highcapable.adbrowser.core.adb.model.pairing.QrPairingSession
import kotlinx.coroutines.flow.Flow

/**
 * Defines ADB communication capabilities used by upper layers.
 */
interface AdbClient {

    /**
     * Verifies whether the configured adb executable path is valid and executable.
     * @param pathValue the adb executable path to validate, or null to validate the currently configured path.
     */
    suspend fun validateExecPath(pathValue: String? = null): OperationResult<Unit>

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
     * Disconnects an ADB-over-network device from the local ADB server.
     */
    suspend fun disconnectDevice(device: AndroidDevice): OperationResult<Unit>

    /**
     * Creates a QR-based wireless debugging pairing session payload.
     *
     * The returned [QrPairingSession.qrContent] can be rendered by the app as a QR code for the
     * Android device to scan. The actual pairing still needs to be completed via [completeQrPairing].
     */
    suspend fun createQrPairingSession(): OperationResult<QrPairingSession>

    /**
     * Waits for the Android device to advertise the QR-requested pairing service over mDNS and then
     * completes `adb pair` with the session password.
     *
     * ADB itself will attempt to connect after a successful pair, following the official wireless
     * debugging flow used by Android Studio and `adb pair`.
     */
    suspend fun completeQrPairing(
        session: QrPairingSession,
        timeoutMillis: Long = 60_000L,
        pollIntervalMillis: Long = 1_500L
    ): OperationResult<PairingDevice>

    /**
     * Observes devices on the local network that are currently advertising the ADB pairing service.
     */
    fun observePairingDevices(pollIntervalMillis: Long = 1_500L): Flow<OperationResult<List<PairingDevice>>>

    /**
     * Completes manual wireless debugging pairing for the selected pairing endpoint.
     */
    suspend fun pairDevice(device: PairingDevice, pairingCode: String): OperationResult<Unit>

    /**
     * Connects to an ADB-over-network endpoint using `host:port`.
     */
    suspend fun connectDevice(address: String): OperationResult<Unit>

    /**
     * Executes an adb command for the target device.
     */
    suspend fun executeCommand(device: AndroidDevice, vararg arguments: Any): AdbResponse
}