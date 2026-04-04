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
package com.highcapable.adbrowser.backend.permission

import com.highcapable.adbrowser.backend.RecordingAdbShellCommandExecutor
import com.highcapable.adbrowser.backend.RecordingLogService
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.AdbResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PermissionServiceImplTest {

    @Test
    fun getPermission_parsesSymbolicAndNumericMode() = runBlocking {
        val shell = RecordingAdbShellCommandExecutor { _, _ ->
            AdbResponse(
                exitCode = 0,
                standardOutput = "-rwxr-sr-t 1 root root 0 Apr 05 12:00 /sdcard/demo.sh\n",
                standardError = ""
            )
        }
        val service = PermissionServiceImpl(shell, RecordingLogService())
        val device = AndroidDevice(serial = "serial", name = "device", model = "device", isOnline = true)

        val result = service.getPermission(device, "/sdcard/demo.sh")

        assertTrue(result.isOk, result.errorMessage.orEmpty())
        val info = requireNotNull(result.data)
        assertEquals("rwxr-sr-t", info.symbolicPermission)
        assertEquals(755, info.numericPermission)
        assertEquals("ls -ld '/sdcard/demo.sh'", shell.commands.single())
    }
}