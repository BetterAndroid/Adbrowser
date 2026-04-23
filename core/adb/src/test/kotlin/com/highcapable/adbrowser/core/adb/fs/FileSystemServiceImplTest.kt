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
 * This file is created by fankes on 2026/4/23.
 */
package com.highcapable.adbrowser.core.adb.fs

import com.highcapable.adbrowser.core.adb.model.AdbResponse
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.shell.AdbShellExecutor
import com.highcapable.adbrowser.core.logging.LogEntry
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.core.logging.LogService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileSystemServiceImplTest {

    @Test
    fun createFolderReturnsFailureWhenEntryAlreadyExists() = runBlocking {
        val shellExecutor = FakeAdbShellExecutor(
            listResponse = successResponse(
                """
                drwxr-xr-x 2 root root 0 2026-04-23 12:00 existing
                """.trimIndent()
            )
        )
        val service = FileSystemServiceImpl(shellExecutor, FakeLogService())

        val result = service.createFolder(device = testDevice, parentPath = "/sdcard", folderName = "existing")

        assertFalse(result.isOk)
        assertEquals("An entry named 'existing' already exists in '/sdcard'.", result.errorMessage)
        assertEquals(listOf("ls -la '/sdcard/'"), shellExecutor.commands)
    }

    @Test
    fun renameReturnsFailureWhenSiblingWithSameNameAlreadyExists() = runBlocking {
        val shellExecutor = FakeAdbShellExecutor(
            listResponse = successResponse(
                """
                -rw-r--r-- 1 root root 0 2026-04-23 12:00 current.txt
                -rw-r--r-- 1 root root 0 2026-04-23 12:00 target.txt
                """.trimIndent()
            )
        )
        val service = FileSystemServiceImpl(shellExecutor, FakeLogService())

        val result = service.rename(device = testDevice, path = "/sdcard/current.txt", newName = "target.txt")

        assertFalse(result.isOk)
        assertEquals("An entry named 'target.txt' already exists in '/sdcard'.", result.errorMessage)
        assertEquals(listOf("ls -la '/sdcard/'"), shellExecutor.commands)
    }

    @Test
    fun renameSkipsShellExecutionWhenTargetNameIsUnchanged() = runBlocking {
        val shellExecutor = FakeAdbShellExecutor()
        val service = FileSystemServiceImpl(shellExecutor, FakeLogService())

        val result = service.rename(device = testDevice, path = "/sdcard/current.txt", newName = "current.txt")

        assertTrue(result.isOk)
        assertTrue(shellExecutor.commands.isEmpty())
    }

    @Test
    fun renameExecutesMoveWhenNoSiblingConflictExists() = runBlocking {
        val shellExecutor = FakeAdbShellExecutor(
            listResponse = successResponse(
                """
                -rw-r--r-- 1 root root 0 2026-04-23 12:00 current.txt
                """.trimIndent()
            )
        )
        val service = FileSystemServiceImpl(shellExecutor, FakeLogService())

        val result = service.rename(device = testDevice, path = "/sdcard/current.txt", newName = "renamed.txt")

        assertTrue(result.isOk, result.errorMessage ?: "Rename should succeed when no sibling conflict exists.")
        assertEquals(
            listOf(
                "ls -la '/sdcard/'",
                "mv '/sdcard/current.txt' '/sdcard/renamed.txt'"
            ),
            shellExecutor.commands
        )
    }

    private class FakeAdbShellExecutor(
        private val listResponse: AdbResponse = successResponse(),
        private val writeResponse: AdbResponse = successResponse()
    ) : AdbShellExecutor {

        val commands = mutableListOf<String>()

        override suspend fun execute(device: AndroidDevice, vararg arguments: Any): AdbResponse {
            val command = arguments.joinToString(" ") { it.toString() }
            commands += command
            return if (command.startsWith("ls -la ")) listResponse else writeResponse
        }
    }

    private class FakeLogService : LogService {

        override val entries: List<LogEntry> = emptyList()

        override val entryCount = 0

        override fun log(level: LogLevel, category: String, message: String) = Unit

        override fun clear() = Unit

        override fun isLevelVisible(level: LogLevel) = true

        override fun setLevelVisible(level: LogLevel, visible: Boolean) = Unit

        override fun observeEntries(): Flow<List<LogEntry>> = flowOf(emptyList())

        override fun observeVisibleEntries(): Flow<List<LogEntry>> = flowOf(emptyList())

        override fun observeEntryCount(): Flow<Int> = flowOf(0)

        override fun observeVisibleLevels(): Flow<Set<LogLevel>> = emptyFlow()
    }

    private companion object {

        val testDevice = AndroidDevice(
            name = "test",
            brand = "Google",
            model = "Pixel",
            systemVersion = "14",
            serial = "device-serial",
            isOnline = true,
            type = AndroidDevice.Type.Usb
        )

        fun successResponse(output: String = "", error: String = "") = AdbResponse(
            exitCode = 0,
            standardOutput = output,
            standardError = error
        )
    }
}