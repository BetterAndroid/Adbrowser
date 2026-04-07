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
package com.highcapable.adbrowser.backend.shell

import com.highcapable.adbrowser.backend.InMemoryAppSettingsService
import com.highcapable.adbrowser.backend.RecordingAdbClient
import com.highcapable.adbrowser.backend.RecordingLogService
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.AdbResponse
import com.highcapable.adbrowser.backend.setting.AppSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdbShellCommandExecutorImplTest {

    @Test
    fun executeFileOperation_fallsBackToNormalWhenSuFails() = runBlocking {
        val adbClient = RecordingAdbClient { _, command ->
            if (command.startsWith("su -c")) error("su not available")
            AdbResponse(exitCode = 0, standardOutput = "ok", standardError = "")
        }
        val settingsService = InMemoryAppSettingsService(
            AppSettings(useSuperuser = true)
        )
        val logService = RecordingLogService()
        val executor = AdbShellCommandExecutorImpl(adbClient, settingsService, logService)
        val device = AndroidDevice(
            name = "2cb1e5f",
            brand = "Google",
            model = "Pixel 8",
            serial = "serial",
            systemVersion = "Android 15 (35)",
            isOnline = true
        )

        val response = executor.executeFileOperation(device, "ls /data/local/tmp")

        assertTrue(response.isOk)
        assertEquals(2, adbClient.commands.size)
        assertTrue(adbClient.commands[0].startsWith("su -c \""))
        assertEquals("ls /data/local/tmp", adbClient.commands[1])
        assertTrue(logService.entries.any { it.message.contains("Fallback to normal shell") })
    }

    @Test
    fun executeFileOperation_fallsBackToNormalWhenSuIsUnavailableResponse() = runBlocking {
        val adbClient = RecordingAdbClient { _, command ->
            if (command.startsWith("su -c")) {
                AdbResponse(
                    exitCode = 127,
                    standardOutput = "",
                    standardError = "/system/bin/sh: su: inaccessible or not found"
                )
            } else AdbResponse(exitCode = 0, standardOutput = "ok", standardError = "")
        }
        val settingsService = InMemoryAppSettingsService(
            AppSettings(useSuperuser = true)
        )
        val logService = RecordingLogService()
        val executor = AdbShellCommandExecutorImpl(adbClient, settingsService, logService)
        val device = AndroidDevice(
            name = "2cb1e5f",
            brand = "Google",
            model = "Pixel 8",
            serial = "serial",
            systemVersion = "Android 15 (35)",
            isOnline = true
        )

        val response = executor.executeFileOperation(device, "ls /sdcard")

        assertTrue(response.isOk)
        assertEquals(2, adbClient.commands.size)
        assertTrue(adbClient.commands[0].startsWith("su -c \""))
        assertEquals("ls /sdcard", adbClient.commands[1])
        assertTrue(logService.entries.any { it.message.contains("Fallback to normal shell") })
    }
}