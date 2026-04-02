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
package com.highcapable.adbrowser.backend.permission

import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.shell.ShellCommandExecutor

/**
 * Permission service implementation based on ADB shell commands.
 */
class PermissionServiceImpl(
    private val shellCommandExecutor: ShellCommandExecutor,
    private val logService: LogService
) : PermissionService {

    override suspend fun getPermission(device: AndroidDevice, path: String): FilePermissionInfo {
        logService.log(LogLevel.Trace, "Permission", "Reading permission for '$path'.")
        val output = shellCommandExecutor.executeFileOperation(device, command = "ls -ld '${escapeShell(path)}'")
        val info = parsePermission(output)
        logService.log(
            LogLevel.Trace,
            "Permission",
            "Read permission for '$path': ${info.symbolicPermission} (${info.numericPermission})."
        )

        return info
    }

    override suspend fun setPermission(device: AndroidDevice, path: String, mode: Int): OperationResult = try {
        shellCommandExecutor.executeFileOperation(device, "chmod $mode '${escapeShell(path)}'")
        logService.log(LogLevel.Information, "Permission", "Updated permission for '$path' to $mode.")
        OperationResult.success()
    } catch (t: Throwable) {
        val message = t.message ?: t::class.simpleName ?: "Unknown error"
        logService.log(LogLevel.Error, "Permission", message)
        OperationResult.failure(message)
    }

    private fun escapeShell(value: String) = value.replace("'", "'\\''")

    private fun parsePermission(lsOutput: String): FilePermissionInfo {
        val line = lsOutput
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: error("Permission query returned empty output.")

        val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty() || tokens.first().length < 10) error("Unexpected permission output: $line")

        val raw = tokens.first()
        val symbolic = raw.substring(1, 10)
        val numeric = toNumericPermission(symbolic)

        return FilePermissionInfo(symbolicPermission = symbolic, numericPermission = numeric)
    }

    private fun toNumericPermission(symbolic: String): Int {
        require(symbolic.length == 9) {
            "Permission string must be 9 chars."
        }

        val owner = bitsToOctal(symbolic[0], symbolic[1], symbolic[2])
        val group = bitsToOctal(symbolic[3], symbolic[4], symbolic[5])
        val other = bitsToOctal(symbolic[6], symbolic[7], symbolic[8])

        return owner * 100 + group * 10 + other
    }

    private fun bitsToOctal(read: Char, write: Char, execute: Char): Int {
        var value = 0
        if (read == 'r') value += 4
        if (write == 'w') value += 2
        if (execute == 'x' || execute == 's' || execute == 't') value += 1

        return value
    }
}