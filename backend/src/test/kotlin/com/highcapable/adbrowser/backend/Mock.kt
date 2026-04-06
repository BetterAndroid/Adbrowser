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
 * This file is created by fankes on 2026/4/5.
 */
package com.highcapable.adbrowser.backend

import com.highcapable.adbrowser.backend.adb.AdbClient
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.AdbResponse
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.logging.LogEntry
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.setting.AppSettings
import com.highcapable.adbrowser.backend.setting.AppSettingsService
import com.highcapable.adbrowser.backend.shell.AdbShellCommandExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.Instant

class RecordingLogService : LogService {

    private val mutableEntries = mutableListOf<LogEntry>()

    override val entries: List<LogEntry>
        get() = mutableEntries.toList()

    override fun log(level: LogLevel, category: String, message: String) {
        mutableEntries.add(0, LogEntry(Instant.now(), level, category, message))
    }
}

class InMemoryAppSettingsService(
    override val current: AppSettings = AppSettings()
) : AppSettingsService {

    override suspend fun load() = Unit

    override suspend fun save() = Unit
}

class RecordingAdbShellCommandExecutor(
    private val responseProvider: suspend (AndroidDevice, String) -> AdbResponse
) : AdbShellCommandExecutor {

    val commands = mutableListOf<String>()

    override suspend fun executeFileOperation(device: AndroidDevice, command: String): AdbResponse {
        commands += command
        return responseProvider(device, command)
    }
}

class RecordingAdbClient(
    private val responseProvider: suspend (AndroidDevice, String) -> AdbResponse
) : AdbClient {

    val commands = mutableListOf<String>()

    override suspend fun validateAdbExecPath(pathValue: String?): OperationResult<Unit> = OperationResult.ok()

    override suspend fun listDevices(): OperationResult<List<AndroidDevice>> = OperationResult.success(emptyList())

    override fun observeDevices(pollIntervalMillis: Long): Flow<OperationResult<List<AndroidDevice>>> = emptyFlow()

    override suspend fun executeShell(device: AndroidDevice, command: String): AdbResponse {
        commands += command
        return responseProvider(device, command)
    }
}