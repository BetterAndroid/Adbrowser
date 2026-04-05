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
package com.highcapable.adbrowser.backend.fs

import com.highcapable.adbrowser.backend.RecordingAdbShellCommandExecutor
import com.highcapable.adbrowser.backend.RecordingLogService
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.AdbResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileSystemServiceImplTest {

    @Test
    fun list_parsesEntries() = runBlocking {
        val shell = RecordingAdbShellCommandExecutor { _, command ->
            when {
                command.startsWith("ls -la '/sdcard/'") -> AdbResponse(
                    exitCode = 0,
                    standardOutput = "-rw-r--r-- 1 root root 12 2026-04-05 10:30 hello.txt\n",
                    standardError = ""
                )

                else -> AdbResponse(
                    exitCode = 1,
                    standardOutput = "",
                    standardError = "unexpected command: $command"
                )
            }
        }
        val service = FileSystemServiceImpl(shell, RecordingLogService())
        val device = AndroidDevice(
            name = "2cb1e5f",
            brand = "Google",
            model = "Pixel 8",
            serial = "serial",
            systemVersion = "Android 15 (35)",
            isOnline = true
        )

        val result = service.list(device, "/sdcard")

        assertTrue(result.isOk, result.errorMessage.orEmpty())
        assertEquals(1, shell.commands.size)

        val entries = requireNotNull(result.data)
        assertEquals(1, entries.size)

        val entry = entries.first()
        assertEquals("/sdcard", entry.path)
        assertEquals("hello.txt", entry.name)
        assertTrue(!entry.isDirectory)
        assertTrue(!entry.isSymlink)
        assertEquals(12L, entry.size)
        assertEquals("rw-r--r--", entry.permission)
        assertEquals(entry.modifiedAt, entry.modifiedAt)
    }
}