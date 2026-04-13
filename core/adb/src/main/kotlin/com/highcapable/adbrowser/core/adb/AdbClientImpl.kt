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
@file:Suppress("LocalVariableName")

package com.highcapable.adbrowser.core.adb

import com.highcapable.adbrowser.core.adb.di.AdbScope
import com.highcapable.adbrowser.core.adb.model.AdbResponse
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.model.OperationResult
import com.highcapable.adbrowser.core.adb.model.OperationRunner
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.core.logging.LogService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Basic ADB client implementation for backend bootstrap.
 */
@AdbScope
@Inject
class AdbClientImpl(private val environment: AdbEnvironment, private val logService: LogService) : AdbClient {

    private companion object {

        const val CATEGORY = "ADB"

        const val ADB_VERSION_SIGNATURE = "Android Debug Bridge"
        const val DEVICE_PROPS_COMMAND = "getprop ro.product.brand; " +
            "getprop ro.build.version.release; " +
            "getprop ro.build.version.sdk"

        const val MIN_DEVICE_OBSERVER_INTERVAL_MS = 500L
        const val ADB_VALIDATE_TIMEOUT_MS = 5000L
        const val STREAM_READ_TIMEOUT_MULTIPLIER = 1L
    }

    private data class ParsedDevice(
        val serial: String,
        val name: String,
        val model: String,
        val isOnline: Boolean
    )

    private data class DeviceExtra(
        val brand: String,
        val systemVersion: String
    ) {

        companion object {

            val Empty = DeviceExtra(brand = "", systemVersion = "")
        }
    }

    private val runner = OperationRunner(logService, CATEGORY)

    override suspend fun validateExecPath(pathValue: String?) = runner.exec {
        val _pathValue = pathValue ?: environment.adbExecPath()
        if (_pathValue.isEmpty()) return OperationResult.error("ADB path is empty.")

        val adbExecPath = Path.of(_pathValue)
        if (!Files.exists(adbExecPath))
            return OperationResult.error("ADB executable was not found.")
        if (!OsType.isWindows && !Files.isExecutable(adbExecPath))
            return OperationResult.error("ADB executable is not executable.")

        val response = runAdb(
            arguments = arrayOf("version"),
            pathValue = _pathValue,
            timeoutMs = ADB_VALIDATE_TIMEOUT_MS
        )
        when {
            !response.isOk -> null to response
            !isAdbVersionResponse(response) -> null to response.copy(
                exitCode = -1,
                standardError = "ADB executable is invalid."
            )
            else -> {
                logService.log(LogLevel.Information, CATEGORY, "Validated ADB path: $_pathValue")
                null to response
            }
        }
    }

    override suspend fun listDevices() = runner.exec<List<AndroidDevice>> {
        logService.log(LogLevel.Trace, CATEGORY, "Listing devices via adb.")
        val response = obtainListDevices()

        if (response.isOk) {
            val lines = response.standardOutput
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()

            val parsedDevices = lines.mapNotNull(::parseDeviceLine)
            val result = coroutineScope {
                parsedDevices.map { parsed ->
                    async {
                        val extra = if (parsed.isOnline)
                            fetchDeviceExtra(parsed.serial)
                        else DeviceExtra.Empty

                        AndroidDevice(
                            name = parsed.name,
                            model = parsed.model,
                            brand = extra.brand,
                            systemVersion = extra.systemVersion,
                            serial = parsed.serial,
                            isOnline = parsed.isOnline
                        )
                    }
                }.awaitAll()
                    .toMutableList()
                    // Prioritize online devices in the list,
                    // while preserving relative order among online and offline devices.
                    .sortedByDescending { it.isOnline }
            }

            result to response
        } else null to response
    }

    override fun observeDevices(pollIntervalMillis: Long) = flow {
        val intervalMillis = pollIntervalMillis.coerceAtLeast(MIN_DEVICE_OBSERVER_INTERVAL_MS)
        var lastSnapshot = snapshotDeviceStates().data
        var lastError: String? = null

        while (currentCoroutineContext().isActive) {
            delay(intervalMillis)

            val snapshotResult = snapshotDeviceStates()
            if (!snapshotResult.isOk) {
                val message = snapshotResult.errorMessage.orEmpty().ifBlank { "Failed to observe devices." }
                if (message != lastError) {
                    lastError = message
                    emit(OperationResult.failure(message))
                }

                continue
            }

            val currentSnapshot = snapshotResult.data.orEmpty()
            if (currentSnapshot == lastSnapshot) continue

            lastSnapshot = currentSnapshot
            lastError = null

            emit(listDevices())
        }
    }

    override suspend fun executeCommand(device: AndroidDevice, vararg arguments: Any): AdbResponse {
        logService.log(LogLevel.Trace, CATEGORY, "$device ${arguments.joinToString(" ")}")
        val response = runAdb("-s", device.serial, *arguments.map { it.toString() }.toTypedArray())

        return response
    }

    /**
     * Executes "adb devices -l" and captures the output for device parsing.
     */
    private suspend fun obtainListDevices() = runAdb("devices", "-l")

    /**
     * Runs adb process with explicit argument list and captures stdout/stderr.
     */
    private suspend fun runAdb(
        vararg arguments: String,
        pathValue: String? = null,
        timeoutMs: Long? = null
    ) = withContext(Dispatchers.IO) {
        val _pathValue = pathValue ?: environment.adbExecPath()
        require(_pathValue.isNotEmpty()) {
            "ADB path is not configured."
        }

        val process = ProcessBuilder(mutableListOf(_pathValue).apply { addAll(arguments) })
            .redirectErrorStream(false)
            .start()
        closeProcessInput(process)

        coroutineContext[Job]?.invokeOnCompletion {
            terminateProcess(process)
        }

        coroutineScope {
            val stdoutTask = async(Dispatchers.IO) {
                process.inputStream.bufferedReader().use { it.readText() }
            }
            val stderrTask = async(Dispatchers.IO) {
                process.errorStream.bufferedReader().use { it.readText() }
            }
            val finished = timeoutMs?.let { waitMs ->
                withContext(Dispatchers.IO) { process.waitFor(waitMs, TimeUnit.MILLISECONDS) }
            } ?: withContext(Dispatchers.IO) {
                process.waitFor()
                true
            }

            if (!finished) {
                terminateProcess(process)
                closeProcessStreams(process)
                stdoutTask.cancel()
                stderrTask.cancel()

                return@coroutineScope AdbResponse(
                    exitCode = -1,
                    standardOutput = "",
                    standardError = "ADB command timed out after ${timeoutMs}ms."
                )
            }

            val streamResult = timeoutMs?.let { commandTimeoutMs ->
                val streamReadTimeoutMs = commandTimeoutMs * STREAM_READ_TIMEOUT_MULTIPLIER
                withTimeoutOrNull(streamReadTimeoutMs) {
                    awaitAll(stdoutTask, stderrTask)
                } ?: run {
                    terminateProcess(process)
                    closeProcessStreams(process)
                    stdoutTask.cancel()
                    stderrTask.cancel()
                    return@coroutineScope AdbResponse(
                        exitCode = process.exitValue(),
                        standardOutput = "",
                        standardError = "ADB output read timed out after ${streamReadTimeoutMs}ms."
                    )
                }
            } ?: awaitAll(stdoutTask, stderrTask)

            val exitCode = process.exitValue()
            val (stdout, stderr) = streamResult

            AdbResponse(
                exitCode = exitCode,
                standardOutput = stdout,
                standardError = stderr
            )
        }
    }

    private fun closeProcessStreams(process: Process) {
        runCatching { process.inputStream.close() }
        runCatching { process.errorStream.close() }
        runCatching { process.outputStream.close() }
    }

    private fun closeProcessInput(process: Process) {
        runCatching { process.outputStream.close() }
    }

    private fun terminateProcess(process: Process) {
        runCatching { process.toHandle() }.getOrNull()
            ?.descendants()
            ?.toList()
            ?.asReversed()
            ?.forEach { child -> runCatching { if (child.isAlive) child.destroyForcibly() } }

        runCatching { if (process.isAlive) process.destroyForcibly() }
    }

    private fun isAdbVersionResponse(response: AdbResponse): Boolean {
        val output = buildString {
            append(response.standardOutput)
            append('\n')
            append(response.standardError)
        }
        return output.contains(ADB_VERSION_SIGNATURE, ignoreCase = true)
    }

    private fun parseDeviceLine(line: String): ParsedDevice? {
        if (line.startsWith("List of devices attached", ignoreCase = true) ||
            line.startsWith("*")
        ) return null

        val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.size < 2) return null

        val serial = tokens[0]
        val state = tokens[1]
        var name = ""
        var model = ""
        val isOnline = state.equals("device", ignoreCase = true)

        tokens.drop(2).forEach { token ->
            when {
                token.startsWith("model:", ignoreCase = true) ->
                    model = token.substring(6).replace('_', ' ')
                token.startsWith("device:", ignoreCase = true) ->
                    name = token.substring(7).replace('_', ' ')
            }
        }

        if (name.isBlank()) name = model.ifBlank { serial }
        if (model.isBlank()) model = name

        return ParsedDevice(
            serial = serial,
            name = name,
            model = model,
            isOnline = isOnline
        )
    }

    private suspend fun fetchDeviceExtra(serial: String): DeviceExtra {
        val response = runAdb("-s", serial, "shell", DEVICE_PROPS_COMMAND)
        if (!response.isOk) {
            logService.log(
                LogLevel.Warning,
                CATEGORY,
                "Failed to query device extra info for $serial: ${response.standardError.trim()}"
            )
            return DeviceExtra.Empty
        }

        val lines = response.standardOutput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val brand = normalizeBrand(lines.getOrNull(0).orEmpty())
        val systemVersion = buildSystemVersion(
            release = lines.getOrNull(1).orEmpty(),
            sdk = lines.getOrNull(2).orEmpty()
        )

        return DeviceExtra(brand, systemVersion)
    }

    /**
     * Creates a lightweight device signature used for change detection.
     */
    private suspend fun snapshotDeviceStates(): OperationResult<List<String>> {
        val response = obtainListDevices()
        if (!response.isOk) return OperationResult.failure(response)

        val states = response.standardOutput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull(::parseDeviceStateToken)
            .sorted()
            .toList()

        return OperationResult.success(states)
    }

    private fun parseDeviceStateToken(line: String): String? {
        if (line.startsWith("List of devices attached", ignoreCase = true) ||
            line.startsWith("*")
        ) return null

        val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.size < 2) return null

        val serial = tokens[0]
        val state = tokens[1].lowercase()

        return "$serial:$state"
    }

    private fun normalizeBrand(raw: String): String {
        val normalized = raw.replace('_', ' ').trim()
        if (normalized.isBlank()) return ""

        return normalized.split(Regex("\\s+")).joinToString(" ") { token ->
            val lower = token.lowercase()
            lower.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    private fun buildSystemVersion(release: String, sdk: String): String {
        val normalizedRelease = release.trim()
        val normalizedSdk = sdk.trim()
        val releaseText = when {
            normalizedRelease.isBlank() -> ""
            normalizedRelease.startsWith("Android", ignoreCase = true) -> normalizedRelease
            else -> "Android $normalizedRelease"
        }

        return when {
            releaseText.isNotBlank() && normalizedSdk.isNotBlank() -> "$releaseText ($normalizedSdk)"
            releaseText.isNotBlank() -> releaseText
            normalizedSdk.isNotBlank() -> "Android ($normalizedSdk)"
            else -> ""
        }
    }
}