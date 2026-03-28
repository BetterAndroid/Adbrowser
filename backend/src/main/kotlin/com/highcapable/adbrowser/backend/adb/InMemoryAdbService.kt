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
 */
package com.highcapable.adbrowser.backend.adb

import com.highcapable.adbrowser.backend.domain.AdbDevice
import com.highcapable.adbrowser.backend.domain.DeviceConnectionState
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * In-memory ADB service used as current project skeleton.
 */
class InMemoryAdbService(initialPath: String? = null) : AdbService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val logs = ArrayDeque<String>()
    private var executablePath: String? = initialPath

    override fun getExecutablePath(): String? = executablePath

    override fun setExecutablePath(path: String?) {
        executablePath = path?.trim().orEmpty().ifBlank { null }
        appendLog("Set adb path: ${executablePath ?: "<empty>"}")
    }

    override fun isExecutableValid(path: String?): Boolean {
        val target = path?.trim().orEmpty()
        if (target.isBlank()) return false
        return runCatching {
            val p: Path = Paths.get(target)
            Files.exists(p) && Files.isRegularFile(p) && Files.isExecutable(p)
        }.getOrDefault(false)
    }

    override fun listDevices(): List<AdbDevice> {
        if (!isExecutableValid()) {
            appendLog("Skip device listing because adb path is invalid")
            return emptyList()
        }
        appendLog("Listed connected devices")
        return listOf(
            AdbDevice(
                id = "emulator-5554",
                name = "Pixel_6_API_35",
                model = "Pixel 6",
                state = DeviceConnectionState.Online
            ),
            AdbDevice(
                id = "R58M1234ABC",
                name = "Galaxy S24",
                model = "SM-S9210",
                state = DeviceConnectionState.Offline
            )
        )
    }

    override fun recentLogs(limit: Int): List<String> {
        val safeLimit = limit.coerceAtLeast(1)
        return logs.takeLast(safeLimit)
    }

    private fun appendLog(message: String) {
        val timestamp = DateTimeFormatter.ISO_LOCAL_TIME
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val line = "[$timestamp] $message"
        logs.addLast(line)
        if (logs.size > 1000) logs.removeFirst()
        logger.info(message)
    }
}