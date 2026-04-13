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
package com.highcapable.adbrowser.core.adb.shell

import com.highcapable.adbrowser.core.adb.AdbClient
import com.highcapable.adbrowser.core.adb.AdbEnvironment
import com.highcapable.adbrowser.core.adb.di.AdbScope
import com.highcapable.adbrowser.core.adb.model.AdbResponse
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.common.utils.extension.escapeSpecialChars
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.core.logging.LogService
import me.tatarka.inject.annotations.Inject

/**
 * Executes shell commands through adb and applies configured superuser fallback strategy.
 */
@AdbScope
@Inject
class AdbShellExecutorImpl(
    private val environment: AdbEnvironment,
    private val adbClient: AdbClient,
    private val logService: LogService
) : AdbShellExecutor {

    private companion object {

        const val CATEGORY = "ADB Shell"

        const val SHELL_PREFIX = "shell"

        val suFallbackIndicators = listOf(
            "su: inaccessible or not found",
            "su: not found",
            "su: inaccessible",
            "su: permission denied"
        )
    }

    override suspend fun execute(device: AndroidDevice, vararg arguments: Any): AdbResponse {
        if (!environment.useSuperuser()) return executeShell(device, *arguments)

        return try {
            val suCommand = """"${arguments.joinToString(" ") { it.toString().escapeSpecialChars() }}""""
            val suResponse = adbClient.executeCommand(device, SHELL_PREFIX, "su", "-c", suCommand)

            if (shouldFallbackToNormalShell(suResponse)) {
                logService.log(
                    LogLevel.Warning,
                    CATEGORY,
                    "su is unavailable on ${device.serial}. Fallback to normal shell."
                )
                executeShell(device, *arguments)
            } else suResponse
        } catch (t: Throwable) {
            logService.log(
                LogLevel.Warning,
                CATEGORY,
                "su execution failed: ${t.message ?: t::class.simpleName}. Fallback to normal shell."
            )
            executeShell(device, *arguments)
        }
    }

    private suspend fun executeShell(device: AndroidDevice, vararg arguments: Any) =
        adbClient.executeCommand(device, SHELL_PREFIX, *arguments)

    private fun shouldFallbackToNormalShell(response: AdbResponse): Boolean {
        if (response.isOk) return false

        val message = buildString {
            append(response.standardError)
            append('\n')
            append(response.standardOutput)
        }.lowercase()

        return suFallbackIndicators.any { indicator -> message.contains(indicator) }
    }
}