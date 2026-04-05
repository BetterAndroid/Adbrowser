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
package com.highcapable.adbrowser.backend.adb

import com.highcapable.adbrowser.backend.InMemoryAppSettingsService
import com.highcapable.adbrowser.backend.RecordingLogService
import com.highcapable.adbrowser.backend.setting.AppSettings
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdbClientImplTest {

    @Test
    fun listDevices_parsesAdbDevicesOutput() = runBlocking {
        val tempDir = Files.createTempDirectory("adb-client-test")
        val fakeAdb = tempDir.resolve("fake-adb")
        Files.writeString(
            fakeAdb,
            """
            #!/bin/sh
            if [ "$1" = "version" ]; then
              echo "Android Debug Bridge version 1.0.41"
              exit 0
            fi
            if [ "$1" = "devices" ] && [ "$2" = "-l" ]; then
              echo "List of devices attached"
              echo "emulator-5554 device product:sdk model:Pixel_8 device:pixel_8"
              echo "offline-123 offline transport_id:2"
              exit 0
            fi
            if [ "$1" = "-s" ] && [ "$3" = "shell" ] && [ "$2" = "emulator-5554" ] && [ "$4" = "getprop ro.product.brand; getprop ro.build.version.release; getprop ro.build.version.sdk" ]; then
              echo "google"
              echo "16"
              echo "36"
              exit 0
            fi
            if [ "$1" = "-s" ]; then
              echo "shell-ok"
              exit 0
            fi
            echo "unsupported args: $@" 1>&2
            exit 1
            """.trimIndent()
        )
        check(fakeAdb.toFile().setExecutable(true)) { "Failed to mark fake adb as executable." }

        val logService = RecordingLogService()
        val settingsService = InMemoryAppSettingsService(
            AppSettings(adbExecPath = fakeAdb.toString())
        )
        val client = AdbClientImpl(logService, settingsService)

        val validate = client.validateAdbExecPath()
        assertTrue(validate.isOk, validate.errorMessage.orEmpty())

        val result = client.listDevices()
        assertTrue(result.isOk, result.errorMessage.orEmpty())

        val devices = requireNotNull(result.data)
        assertEquals(2, devices.size)

        assertEquals("emulator-5554", devices[0].serial)
        assertEquals("pixel 8", devices[0].name)
        assertEquals("Pixel 8", devices[0].model)
        assertEquals("Google", devices[0].brand)
        assertEquals("Android 16 (36)", devices[0].systemVersion)
        assertTrue(devices[0].isOnline)

        assertEquals("offline-123", devices[1].serial)
        assertEquals("offline-123", devices[1].name)
        assertEquals("offline-123", devices[1].model)
        assertEquals("", devices[1].brand)
        assertEquals("", devices[1].systemVersion)
        assertTrue(!devices[1].isOnline)
    }
}