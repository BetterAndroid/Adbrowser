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
import com.highcapable.adbrowser.core.adb.model.pairing.MdnsService
import com.highcapable.adbrowser.core.adb.model.pairing.MdnsServiceType
import com.highcapable.adbrowser.core.adb.model.pairing.PairingDevice
import com.highcapable.adbrowser.core.adb.model.pairing.QrPairingSession
import com.highcapable.adbrowser.core.common.network.NetworkEndpoint
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.core.logging.LogService
import kotlinx.coroutines.CancellationException
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
import java.security.SecureRandom
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
        const val MIN_MDNS_OBSERVER_INTERVAL_MS = 500L

        const val ADB_VALIDATE_TIMEOUT_MS = 5000L
        const val ADB_PAIR_TIMEOUT_MS = 15_000L
        const val ADB_CONNECT_TIMEOUT_MS = 10_000L

        const val MDNS_QR_CONTENT = "WIFI:T:ADB;S:{serviceName};P:{password};;"
        const val MDNS_QR_SERVICE_NAME_PREFIX = "debug-"
        const val MDNS_QR_SERVICE_NAME_SUFFIX_LENGTH = 6
        const val MDNS_QR_PASSWORD_LENGTH = 6
        const val MDNS_SERVICE_NAME_QUOTE = '"'
        const val STREAM_READ_TIMEOUT_MULTIPLIER = 1L
        const val QR_PAIRING_TRANSIENT_PROTOCOL_FAULT = "protocol fault"
        const val QR_PAIRING_TRANSIENT_STATUS_READ_FAILURE = "couldn't read status message"
        const val QR_PAIRING_MAX_RETRY_ATTEMPTS = 5
        const val ADB_CONNECT_SERVICE_NAME_PREFIX = "adb-"

        val emulatorSerialRegex = """^emulator-\d+$""".toRegex()
        val networkSerialRegex = """^(?:\[[0-9A-Fa-f:]+]|[^:\s]+):\d+$""".toRegex()
        val mdnsNetworkSerialRegex = """^.+\._adb(?:-tls-connect)?\._tcp\.?$""".toRegex()
        val pairGuidRegex = """\[guid=([^\]]+)]""".toRegex()

        val numericQrTokenChars = "0123456789".toCharArray()
    }

    private data class AndroidDeviceExtra(
        val brand: String,
        val systemVersion: String
    ) {

        companion object {

            val Empty = AndroidDeviceExtra(brand = "", systemVersion = "")
        }
    }

    private val runner = OperationRunner(logService, CATEGORY)
    private val secureRandom = SecureRandom()

    override suspend fun validateExecPath(pathValue: String?) = runner.exec {
        val _pathValue = pathValue ?: environment.adbExecPath()
        if (_pathValue.isNotEmpty()) {
            val adbExecPath = Path.of(_pathValue)

            when {
                !Files.exists(adbExecPath) -> null to errorResponse("ADB executable was not found.")
                !OsType.isWindows && !Files.isExecutable(adbExecPath) ->
                    null to errorResponse("ADB executable is not executable.")
                else -> {
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
            }
        } else null to errorResponse("ADB path is empty.")
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
                        else AndroidDeviceExtra.Empty

                        parsed.copy(
                            brand = extra.brand,
                            systemVersion = extra.systemVersion
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
        val initialSnapshot = snapshotDeviceStates()
        var lastSnapshot = initialSnapshot.data
        var lastError = initialSnapshot.errorMessage?.takeIf { it.isNotBlank() }

        if (lastError != null) logService.log(LogLevel.Error, CATEGORY, lastError)

        while (currentCoroutineContext().isActive) {
            delay(intervalMillis)

            val snapshotResult = snapshotDeviceStates()
            if (!snapshotResult.isOk) {
                val message = snapshotResult.errorMessage.orEmpty().ifBlank { "Failed to observe devices." }
                if (message != lastError) {
                    lastError = message
                    logService.log(LogLevel.Error, CATEGORY, message)
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

    override suspend fun disconnectDevice(device: AndroidDevice) = runner.exec {
        if (device.type != AndroidDevice.Type.Network) return@exec null to errorResponse("Only network ADB devices can be disconnected.")

        logService.log(LogLevel.Information, CATEGORY, "Disconnecting device: ${device.serial}")
        null to runAdb("disconnect", device.serial)
    }

    override suspend fun createQrPairingSession() = runner.exec<QrPairingSession> {
        // Some devices appear to be far more reliable with a short DNS-SD instance name and a
        // numeric 6-digit shared secret, which is also close to the familiar manual pairing-code
        // flow. The protocol only requires both sides to agree on S/P, so we favor compatibility
        // here over mirroring Android Studio's longer randomized payload shape exactly.
        val serviceName = MDNS_QR_SERVICE_NAME_PREFIX + randomToken(
            length = MDNS_QR_SERVICE_NAME_SUFFIX_LENGTH,
            alphabet = numericQrTokenChars
        )
        val password = randomToken(
            length = MDNS_QR_PASSWORD_LENGTH,
            alphabet = numericQrTokenChars
        )
        val qrContent = MDNS_QR_CONTENT
            .replace("{serviceName}", serviceName)
            .replace("{password}", password)

        logService.log(LogLevel.Information, CATEGORY, "Created QR pairing session for service: $serviceName")
        QrPairingSession(
            serviceName = serviceName,
            password = password,
            qrContent = qrContent
        ) to successResponse("QR pairing session created.")
    }

    override suspend fun completeQrPairing(
        session: QrPairingSession,
        timeoutMillis: Long,
        pollIntervalMillis: Long
    ) = runner.exec<PairingDevice> {
        // Some adb builds wrap mDNS service names in quotes when printing `adb mdns services`,
        // while the QR payload stores the raw instance name. Always normalize both sides before
        // matching, otherwise we can discover the correct pairing endpoint but still never pair.
        val serviceName = normalizeMdnsServiceName(session.serviceName)
        val password = session.password.trim()
        if (serviceName.isEmpty()) return@exec null to errorResponse("QR pairing service name is empty.")
        if (password.isEmpty()) return@exec null to errorResponse("QR pairing password is empty.")

        val intervalMillis = pollIntervalMillis.coerceAtLeast(MIN_MDNS_OBSERVER_INTERVAL_MS)
        var lastPairFailure: AdbResponse? = null
        var terminalPairFailure: AdbResponse? = null
        var transientRetryAttempts = 0
        val pairingDevice = withTimeoutOrNull(timeoutMillis.coerceAtLeast(intervalMillis)) {
            while (currentCoroutineContext().isActive) {
                val snapshot = listPairingDevicesSnapshot()
                if (snapshot.isOk) {
                    val matchingDevice = snapshot.data.orEmpty()
                        .firstOrNull { normalizeMdnsServiceName(it.serviceName) == serviceName }
                    if (matchingDevice != null) {
                        val response = runPair(matchingDevice.address, password)
                        if (response.isOk) {
                            val resolvedServiceName = parsePairGuidServiceName(response) ?: matchingDevice.serviceName
                            return@withTimeoutOrNull matchingDevice.copy(serviceName = resolvedServiceName)
                        }

                        // QR pairing can fail with "protocol fault (couldn't read status message)"
                        // even after the device has already exposed the expected `_adb-tls-pairing`
                        // endpoint. This is not reliably reproducible across OEM ROMs: some devices
                        // pair on the first try, while others transiently reject the first few
                        // attempts even though the QR payload is otherwise valid.
                        if (!shouldRetryQrPairing(response)) {
                            terminalPairFailure = response
                            return@withTimeoutOrNull null
                        }

                        transientRetryAttempts += 1
                        lastPairFailure = response

                        // Do not retry forever. A persistent protocol fault is usually a device-side
                        // compatibility issue rather than timing, so after a small retry budget we
                        // surface the raw adb error back to the UI unchanged for diagnostics.
                        if (transientRetryAttempts >= QR_PAIRING_MAX_RETRY_ATTEMPTS) {
                            terminalPairFailure = response
                            return@withTimeoutOrNull null
                        }

                        logService.log(
                            LogLevel.Warning,
                            CATEGORY,
                            "QR pairing handshake for service '$serviceName' at ${matchingDevice.address} " +
                                "failed transiently, retrying ($transientRetryAttempts/$QR_PAIRING_MAX_RETRY_ATTEMPTS): ${response.message}"
                        )
                    }
                }

                delay(intervalMillis)
            }

            null
        }

        if (pairingDevice == null) {
            val failure = terminalPairFailure ?: lastPairFailure
            return@exec null to (failure ?: errorResponse(
                "Timed out waiting for QR pairing device '$serviceName' after ${timeoutMillis}ms."
            ))
        }

        logService.log(
            LogLevel.Information,
            CATEGORY,
            "Completed QR pairing for service '$serviceName' at ${pairingDevice.address}"
        )
        pairingDevice to successResponse("QR pairing completed.")
    }

    override fun observePairingDevices(pollIntervalMillis: Long) = flow {
        val intervalMillis = pollIntervalMillis.coerceAtLeast(MIN_MDNS_OBSERVER_INTERVAL_MS)
        val initialSnapshot = listPairingDevicesSnapshot().filterOwnQrPairingServices()
        var lastSnapshot = initialSnapshot.data
        var lastError = initialSnapshot.errorMessage?.takeIf { it.isNotBlank() }

        emit(initialSnapshot)
        if (lastError != null) logService.log(LogLevel.Error, CATEGORY, lastError)

        while (currentCoroutineContext().isActive) {
            delay(intervalMillis)

            val snapshotResult = listPairingDevicesSnapshot().filterOwnQrPairingServices()
            if (!snapshotResult.isOk) {
                val message = snapshotResult.errorMessage.orEmpty().ifBlank {
                    "Failed to observe pairing devices."
                }
                if (message != lastError) {
                    lastError = message
                    logService.log(LogLevel.Error, CATEGORY, message)
                    emit(OperationResult.failure(message))
                }

                continue
            }

            val currentSnapshot = snapshotResult.data.orEmpty()
            if (currentSnapshot == lastSnapshot) continue

            lastSnapshot = currentSnapshot
            lastError = null

            emit(snapshotResult)
        }
    }

    override suspend fun pairDevice(device: PairingDevice, pairingCode: String) = runner.exec {
        val code = pairingCode.trim()
        if (code.isEmpty()) return@exec null to errorResponse("Pairing code is empty.")

        logService.log(LogLevel.Information, CATEGORY, "Pairing to ${device.address}")
        null to runPair(device.address, code)
    }

    override suspend fun connectDevice(address: String) = runner.exec {
        val normalizedAddress = NetworkEndpoint.normalize(
            value = address,
            defaultPort = NetworkEndpoint.DEFAULT_ADB_PORT
        ) ?: return@exec null to errorResponse("Device address is invalid.")

        logService.log(LogLevel.Information, CATEGORY, "Connecting to network device: $normalizedAddress")
        val response = runAdb("connect", normalizedAddress, timeoutMs = ADB_CONNECT_TIMEOUT_MS)
        if (!response.isOk) return@exec null to response

        // Coerce a successful connection by checking for the expected success line in the output,
        // since ADB may return 0 even for certain failure cases (e.g. "unable to connect to <ip>:<port>").
        val successLine = "connected to $normalizedAddress"
        if (response.outputLines().none { it == successLine }) {
            val message = response.message.ifBlank {
                "ADB connect did not report a successful connection to '$normalizedAddress'."
            }
            return@exec null to errorResponse(message)
        }

        null to response
    }

    override suspend fun executeCommand(device: AndroidDevice, vararg arguments: Any): AdbResponse {
        logService.log(LogLevel.Trace, CATEGORY, "$device ${arguments.joinToString(" ")}")
        val response = runAdb("-s", device.serial, *arguments)

        return response
    }

    /**
     * Executes "adb devices -l" and captures the output for device parsing.
     */
    private suspend fun obtainListDevices() = runAdb("devices", "-l")

    /**
     * Executes "adb mdns services" and parses discoverable wireless debugging services.
     */
    private suspend fun obtainMdnsServices() = runAdb("mdns", "services")

    /**
     * Executes "adb pair <address> <credential>" to complete wireless debugging pairing.
     */
    private suspend fun runPair(address: String, credential: String) =
        runAdb("pair", address, credential, timeoutMs = ADB_PAIR_TIMEOUT_MS)

    /**
     * Runs adb process with explicit argument list and captures stdout/stderr.
     */
    private suspend fun runAdb(
        vararg arguments: Any,
        pathValue: String? = null,
        timeoutMs: Long? = null
    ) = withContext(Dispatchers.IO) {
        val _pathValue = pathValue ?: environment.adbExecPath()
        if (_pathValue.isEmpty()) return@withContext errorResponse("ADB path is not configured.")

        val _arguments = arguments.map { it.toString() }
        val process = ProcessBuilder(mutableListOf(_pathValue).apply { addAll(_arguments) })
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

    private fun parseDeviceLine(line: String): AndroidDevice? {
        if (line.startsWith("List of devices attached", ignoreCase = true) ||
            line.startsWith("*")
        ) return null

        val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.size < 2) return null

        val serial = tokens[0]
        val state = tokens[1]
        var name = ""
        var model = ""
        val isOnline = state.equals("device", ignoreCase = true)
        val type = resolveDeviceType(serial, tokens)

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

        return AndroidDevice(
            serial = serial,
            name = name,
            brand = "",
            model = model,
            systemVersion = "",
            isOnline = isOnline,
            type = type
        )
    }

    /**
     * Resolves a product-facing device transport type from `adb devices -l` output.
     *
     * ADB host internals mainly expose USB and LOCAL transports. LOCAL covers both emulators and
     * TCP devices, so we refine it here using the stable serial formats returned by ADB.
     */
    private fun resolveDeviceType(serial: String, tokens: List<String>): AndroidDevice.Type {
        if (tokens.any { it.startsWith("usb:", ignoreCase = true) }) return AndroidDevice.Type.Usb
        if (emulatorSerialRegex.matches(serial)) return AndroidDevice.Type.Emulator
        if (networkSerialRegex.matches(serial) || mdnsNetworkSerialRegex.matches(serial)) return AndroidDevice.Type.Network

        // Physical devices may not always include an usb: segment in every state, but their serials
        // also do not match emulator/network patterns, so treat the remaining connected entries as USB.
        return AndroidDevice.Type.Usb
    }

    private suspend fun fetchDeviceExtra(serial: String): AndroidDeviceExtra {
        val response = runAdb("-s", serial, "shell", DEVICE_PROPS_COMMAND)
        if (!response.isOk) {
            logService.log(
                LogLevel.Warning,
                CATEGORY,
                "Failed to query device extra info for $serial: ${response.standardError.trim()}"
            )
            return AndroidDeviceExtra.Empty
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

        return AndroidDeviceExtra(brand, systemVersion)
    }

    /**
     * Creates a lightweight device signature used for change detection.
     */
    private suspend fun snapshotDeviceStates() = try {
        val response = obtainListDevices()
        if (!response.isOk)
            OperationResult.failure(response)
        else {
            val states = response.standardOutput
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull(::parseDeviceStateToken)
                .sorted()
                .toList()

            OperationResult.success(states)
        }
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        OperationResult.failure(t.message ?: t::class.simpleName ?: "Failed to observe devices.")
    }

    /**
     * Queries adb's built-in mDNS discovery list and keeps only pairing endpoints.
     */
    private suspend fun listPairingDevicesSnapshot() = try {
        val response = obtainMdnsServices()
        if (!response.isOk)
            OperationResult.failure(response)
        else {
            val pairingDevices = response.standardOutput
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull(::parseMdnsServiceLine)
                .filter { it.type == MdnsServiceType.Pairing }
                .map {
                    PairingDevice(
                        serviceName = it.serviceName,
                        host = it.host,
                        port = it.port
                    )
                }
                .sortedWith(compareBy<PairingDevice> { it.serviceName }.thenBy { it.address })
                .toList()

            OperationResult.success(pairingDevices)
        }
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        OperationResult.failure(t.message ?: t::class.simpleName ?: "Failed to discover pairing devices.")
    }

    private fun parseMdnsServiceLine(line: String): MdnsService? {
        val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.size < 3) return null

        val endpoint = NetworkEndpoint.parse(tokens.last()) ?: return null
        val type = MdnsServiceType.from(tokens[tokens.size - 2]) ?: return null
        val serviceName = normalizeMdnsServiceName(tokens.subList(0, tokens.size - 2).joinToString(" "))
        if (serviceName.isEmpty()) return null

        return MdnsService(
            serviceName = serviceName,
            type = type,
            host = endpoint.host,
            port = endpoint.port
        )
    }

    private fun AdbResponse.outputLines() = buildList {
        standardOutput.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(::add)
        standardError.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(::add)
    }

    private fun successResponse(message: String) = AdbResponse(
        exitCode = 0,
        standardOutput = message,
        standardError = ""
    )

    private fun errorResponse(message: String) = AdbResponse(
        exitCode = -1,
        standardOutput = "",
        standardError = message
    )

    /**
     * Some adb builds wrap mDNS instance names in quotes when printing `adb mdns services`.
     * QR pairing needs an exact service-name match, so we normalize the printed form back to the
     * logical DNS-SD instance name before comparing or exposing it to upper layers.
     */
    private fun normalizeMdnsServiceName(raw: String): String {
        val normalized = raw.trim()
        if (normalized.length < 2) return normalized

        return if (normalized.first() == MDNS_SERVICE_NAME_QUOTE &&
            normalized.last() == MDNS_SERVICE_NAME_QUOTE
        ) normalized.substring(1, normalized.lastIndex).trim()
        else normalized
    }

    private fun isOwnQrPairingService(serviceName: String): Boolean {
        val normalized = normalizeMdnsServiceName(serviceName)
        return normalized.startsWith(MDNS_QR_SERVICE_NAME_PREFIX, ignoreCase = true)
    }

    /**
     * Successful `adb pair` responses can expose the device guid used by the later auto-connect
     * service. QR-created pairing services use our own host-side instance name (`debug-xxxxxx`),
     * which is not useful for retrospective selection once the final connected device shows up as an
     * `adb-<guid>-<suffix>._adb-tls-connect._tcp` serial. Prefer the returned guid when available.
     */
    private fun parsePairGuidServiceName(response: AdbResponse): String? {
        val guid = pairGuidRegex.find(response.message)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (guid.isEmpty()) return null

        return if (guid.startsWith(ADB_CONNECT_SERVICE_NAME_PREFIX, ignoreCase = true)) guid
        else "$ADB_CONNECT_SERVICE_NAME_PREFIX$guid"
    }

    private fun OperationResult<List<PairingDevice>>.filterOwnQrPairingServices(): OperationResult<List<PairingDevice>> {
        if (!isOk) return this

        return copy(data = data.orEmpty().filterNot { isOwnQrPairingService(it.serviceName) })
    }

    /**
     * QR pairing sometimes reports a low-level protocol fault even though the device has already
     * published the requested mDNS endpoint. In practice this can happen during the brief window
     * where the pairing service is discoverable but the handshake is not yet fully ready, so we
     * keep retrying inside the QR wait timeout for these specific transport-layer faults.
     */
    private fun shouldRetryQrPairing(response: AdbResponse): Boolean {
        val message = response.message.lowercase()
        return QR_PAIRING_TRANSIENT_PROTOCOL_FAULT in message &&
            QR_PAIRING_TRANSIENT_STATUS_READ_FAILURE in message
    }

    private fun parseDeviceStateToken(line: String): String? {
        if (line.startsWith("List of devices attached", ignoreCase = true) ||
            line.startsWith("*")
        ) return null

        val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.size < 2) return null

        val serial = tokens[0]
        val state = tokens[1].lowercase()

        return "$serial:$state"
    }

    private fun normalizeBrand(raw: String): String {
        val normalized = raw.replace('_', ' ').trim()
        if (normalized.isBlank()) return ""

        return normalized.split("\\s+".toRegex()).joinToString(" ") { token ->
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

    private fun randomToken(length: Int, alphabet: CharArray) = buildString(length) {
        repeat(length) {
            append(alphabet[secureRandom.nextInt(alphabet.size)])
        }
    }
}