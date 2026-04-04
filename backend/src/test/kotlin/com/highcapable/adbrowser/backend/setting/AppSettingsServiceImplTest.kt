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
package com.highcapable.adbrowser.backend.setting

import com.highcapable.adbrowser.backend.RecordingLogService
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSettingsServiceImplTest {

    @Test
    fun loadAndSave_persistsSettingsToDisk() = runBlocking {
        // This implementation prefers APPDATA on Windows. We only run this test in
        // environments where APPDATA is not set so we can isolate by overriding user.home.
        if (!System.getenv("APPDATA").isNullOrBlank()) return@runBlocking

        val originalHome = System.getProperty("user.home")
        val tempHome = Files.createTempDirectory("adbrowser-settings-test")

        try {
            System.setProperty("user.home", tempHome.toString())

            val firstService = AppSettingsServiceImpl(RecordingLogService())
            firstService.load()

            // Defaults should be initialized on first load.
            assertEquals("", firstService.current.language)

            firstService.current.language = "zh-CN"
            firstService.current.adbExecPath = "/tmp/fake-adb"
            firstService.current.useSuperuser = true
            firstService.current.showHiddenFiles = false
            firstService.current.devicePaneWidth = 420.0
            firstService.save()

            val secondService = AppSettingsServiceImpl(RecordingLogService())
            secondService.load()

            assertEquals("zh-CN", secondService.current.language)
            assertEquals("/tmp/fake-adb", secondService.current.adbExecPath)
            assertTrue(secondService.current.useSuperuser)
            assertEquals(false, secondService.current.showHiddenFiles)
            assertEquals(420.0, secondService.current.devicePaneWidth)

            val settingsFiles = Files.walk(tempHome).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.fileName.toString() == "settings.json" }
                    .toList()
            }
            assertEquals(1, settingsFiles.size)
        } finally {
            System.setProperty("user.home", originalHome)
        }
    }
}