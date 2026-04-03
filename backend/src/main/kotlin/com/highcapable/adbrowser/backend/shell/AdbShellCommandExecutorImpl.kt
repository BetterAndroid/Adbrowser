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
package com.highcapable.adbrowser.backend.shell

import com.highcapable.adbrowser.backend.adb.AdbClient
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.AdbResponse
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.setting.AppSettingsService
import me.tatarka.inject.annotations.Inject

/**
 * Executes shell commands through adb and applies configured superuser fallback strategy.
 */
@Inject
class AdbShellCommandExecutorImpl(
    private val adbClient: AdbClient,
    private val settingsService: AppSettingsService,
    private val logService: LogService
) : AdbShellCommandExecutor {

    private companion object {

        const val CATEGORY = "ADB Shell"
    }

    /**
     * Executes command with `su -c` first when superuser mode is enabled, then falls back.
     */
    override suspend fun executeFileOperation(device: AndroidDevice, command: String): AdbResponse {
        if (!settingsService.current.useSuperuser) return adbClient.executeShell(device, command)

        return try {
            val suCommand = "su -c \"${escapeForDoubleQuotedShell(command)}\""
            adbClient.executeShell(device, suCommand)
        } catch (t: Throwable) {
            logService.log(
                LogLevel.Warning,
                CATEGORY,
                "su execution failed: ${t.message ?: t::class.simpleName}. Fallback to normal shell."
            )
            adbClient.executeShell(device, command)
        }
    }

    private fun escapeForDoubleQuotedShell(value: String) = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("`", "\\`")
}