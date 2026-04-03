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
package com.highcapable.adbrowser.backend.adb

import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.AdbResponse
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.domain.OperationRunner
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.setting.AppSettingsService
import com.highcapable.adbrowser.backend.utils.SystemKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import java.nio.file.Files
import java.nio.file.Path

/**
 * Basic ADB client implementation for backend bootstrap.
 */
@Inject
class AdbClientImpl(private val logService: LogService, private val settingsService: AppSettingsService) : AdbClient {

    private companion object {

        const val CATEGORY = "ADB"
    }

    private val runner = OperationRunner(logService, CATEGORY)
    private val adbExecPath get() = settingsService.current.adbExecPath.trim()

    override suspend fun validateAdbExecPath() = runner.exec {
        val pathValue = adbExecPath
        if (pathValue.isEmpty()) return OperationResult.error("ADB path is empty.")

        val adbExecPath = Path.of(pathValue)
        if (!Files.exists(adbExecPath))
            return OperationResult.error("ADB executable was not found.")
        if (!SystemKind.isWindows && !Files.isExecutable(adbExecPath))
            return OperationResult.error("ADB executable is not executable.")

        val response = runAdb(listOf("version"))
        if (response.isOk) logService.log(LogLevel.Information, CATEGORY, "Validated ADB path: $pathValue")

        null to response
    }

    override suspend fun listDevices() = runner.exec<List<AndroidDevice>> {
        logService.log(LogLevel.Trace, CATEGORY, "Listing devices via adb.")
        val response = runAdb(listOf("devices", "-l"))

        if (response.isOk) {
            val result = mutableListOf<AndroidDevice>()
            val lines = response.standardOutput
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()

            lines.forEach { line ->
                if (line.startsWith("List of devices attached", ignoreCase = true) || line.startsWith("*"))
                    return@forEach

                val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (tokens.size < 2) return@forEach

                val serial = tokens[0]
                val state = tokens[1]
                val isOnline = state.equals("device", ignoreCase = true)
                var model = ""
                var name = ""

                for (token in tokens.drop(2)) when {
                    token.startsWith("model:", ignoreCase = true) ->
                        model = token.substring(6).replace('_', ' ')
                    token.startsWith("device:", ignoreCase = true) ->
                        name = token.substring(7).replace('_', ' ')
                }

                if (name.isBlank()) name = model.ifBlank { serial }
                if (model.isBlank()) model = name

                result += AndroidDevice(
                    serial = serial,
                    name = name,
                    model = model,
                    isOnline = isOnline
                )
            }

            result to response
        } else null to response
    }

    override suspend fun executeShell(device: AndroidDevice, command: String): AdbResponse {
        logService.log(LogLevel.Trace, CATEGORY, "$device $ $command")
        val response = runAdb(listOf("-s", device.serial, "shell", command))

        return response
    }

    /**
     * Runs adb process with explicit argument list and captures stdout/stderr.
     */
    private suspend fun runAdb(arguments: List<String>) = withContext(Dispatchers.IO) {
        val pathValue = adbExecPath
        require(pathValue.isNotEmpty()) {
            "ADB path is not configured."
        }

        val process = ProcessBuilder(mutableListOf(pathValue).apply { addAll(arguments) })
            .redirectErrorStream(false)
            .start()

        coroutineContext[Job]?.invokeOnCompletion {
            if (process.isAlive) process.destroyForcibly()
        }

        coroutineScope {
            val stdoutTask = async(Dispatchers.IO) {
                process.inputStream.bufferedReader().use { it.readText() }
            }
            val stderrTask = async(Dispatchers.IO) {
                process.errorStream.bufferedReader().use { it.readText() }
            }
            val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
            val (stdout, stderr) = awaitAll(stdoutTask, stderrTask)

            AdbResponse(
                exitCode = exitCode,
                standardOutput = stdout,
                standardError = stderr
            )
        }
    }
}