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
package com.highcapable.adbrowser.core.adb.permission

import com.highcapable.adbrowser.core.adb.di.AdbScope
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.model.OperationRunner
import com.highcapable.adbrowser.core.adb.shell.AdbShellExecutor
import com.highcapable.adbrowser.core.common.fs.FilePermission
import com.highcapable.adbrowser.core.common.utils.extension.escapeQuotes
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.core.logging.LogService
import me.tatarka.inject.annotations.Inject

/**
 * Permission service implementation based on ADB shell commands.
 */
@AdbScope
@Inject
class PermissionServiceImpl(
    private val shellExecutor: AdbShellExecutor,
    private val logService: LogService
) : PermissionService {

    private companion object {

        const val CATEGORY = "Permission"
    }

    private val runner = OperationRunner(logService, CATEGORY)

    override suspend fun getPermission(device: AndroidDevice, path: String) = runner.exec<FilePermission.Info> {
        logService.log(LogLevel.Trace, CATEGORY, "Reading permission for '$path'.")
        val response = shellExecutor.execute(device, "ls", "-ld", "'${path.escapeQuotes()}'")

        if (response.isOk) {
            val info = parsePermission(response.standardOutput)
            logService.log(
                LogLevel.Trace,
                CATEGORY,
                "Read permission for '$path': ${info.symbolicPermission} (${info.numericPermission})."
            )

            info to response
        } else null to response
    }

    override suspend fun setPermission(device: AndroidDevice, path: String, mode: Int) = runner.exec {
        val response = shellExecutor.execute(device, "chmod", mode, "'${path.escapeQuotes()}'")
        if (response.isOk) logService.log(LogLevel.Information, CATEGORY, "Updated permission for '$path' to $mode.")

        null to response
    }

    private fun parsePermission(lsOutput: String): FilePermission.Info {
        val line = lsOutput
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: error("Permission query returned empty output.")

        val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty() || tokens.first().length < 10) error("Unexpected permission output: $line")

        val raw = tokens.first()
        val symbolic = raw.substring(1, 10)

        return FilePermission.fromSymbolic(symbolic)
    }
}