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
 * This file is created by fankes on 2026/4/13.
 */
package com.highcapable.adbrowser.core.adb

import com.highcapable.adbrowser.core.adb.DeviceDebugTest.Companion.ENABLE_REAL_DEVICE_TESTS
import com.highcapable.adbrowser.core.adb.di.AdbComponent
import com.highcapable.adbrowser.core.adb.di.create
import com.highcapable.adbrowser.core.adb.fs.FileSystemService
import com.highcapable.adbrowser.core.adb.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.permission.PermissionService
import com.highcapable.adbrowser.core.logging.di.LoggingComponent
import com.highcapable.adbrowser.core.logging.di.create
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Manual device debug tests for core adb module.
 *
 * Toggle [ENABLE_REAL_DEVICE_TESTS] to true when you want to run these tests locally.
 */
class DeviceDebugTest {

    private data class Services(
        val adbClient: AdbClient,
        val fileSystemService: FileSystemService,
        val permissionService: PermissionService,
        val adbExecPath: String,
        val useSuperuser: Boolean
    )

    private companion object {

        // Set to true to enable real-device debug tests.
        // Make sure you have a properly connected Android device and ADB setup before enabling.
        const val ENABLE_REAL_DEVICE_TESTS = false

        // Override with ADB_EXEC_PATH env var if needed.
        const val ADB_EXEC_PATH = ""

        // Set to true if you want to test with root permissions.
        const val USE_SUPERUSER = false

        const val LIST_PATH = "/"
    }

    @Test
    fun connectToFirstAvailableDevice() = runBlocking {
        ensureEnabled()

        val services = initializeServices()
        val devicesResult = services.adbClient.listDevices()

        assertTrue(devicesResult.isOk, devicesResult.errorMessage ?: "Failed to list devices.")
        val devices = devicesResult.data.orEmpty()
        assertTrue(devices.isNotEmpty(), "No connected Android device found.")

        println("[DEBUG] connected devices (${devices.size}):")
        devices.forEachIndexed { index, device ->
            println("[DEBUG] #$index $device online=${device.isOnline}")
        }
    }

    @Test
    fun listFilesFromFirstDeviceAndPrint() = runBlocking {
        ensureEnabled()

        val (services, device) = initializeServicesAndFirstDevice()
        val listResult = services.fileSystemService.list(device, LIST_PATH)

        assertTrue(listResult.isOk, listResult.errorMessage ?: "Failed to list files from '$LIST_PATH'.")
        val entries = listResult.data.orEmpty()

        println("[DEBUG] file list on ${device.serial} path '$LIST_PATH' (${entries.size} entries):")
        entries.forEach { entry ->
            println("[DEBUG] ${formatEntry(entry)}")
        }
    }

    @Test
    fun getPermissionFromFirstListedEntry() = runBlocking {
        ensureEnabled()

        val (services, device) = initializeServicesAndFirstDevice()
        val listResult = services.fileSystemService.list(device, LIST_PATH)

        assertTrue(listResult.isOk, listResult.errorMessage ?: "Failed to list files from '$LIST_PATH'.")
        val firstEntry = listResult.data.orEmpty().firstOrNull()
        assertNotNull(firstEntry, "No file entry available to query permission.")

        val targetPath = buildFullPath(firstEntry)
        val permissionResult = services.permissionService.getPermission(device, targetPath)

        assertTrue(permissionResult.isOk, permissionResult.errorMessage ?: "Failed to get permission for '$targetPath'.")
        val info = assertNotNull(permissionResult.data)

        println("[DEBUG] permission for '$targetPath': ${info.symbolicPermission} (${info.numericPermission})")
    }

    private suspend fun initializeServices(): Services {
        val environment = AdbEnvironment(
            adbExecPath = { ADB_EXEC_PATH },
            useSuperuser = { USE_SUPERUSER }
        )
        val logging = LoggingComponent::class.create()
        val adb = AdbComponent::class.create(logging, environment)
        val services = Services(
            adbClient = adb.provideAdbClient(),
            fileSystemService = adb.provideFileSystemService(),
            permissionService = adb.providePermissionService(),
            adbExecPath = ADB_EXEC_PATH,
            useSuperuser = USE_SUPERUSER
        )

        val validateResult = services.adbClient.validateExecPath()
        assertTrue(validateResult.isOk, validateResult.errorMessage ?: "ADB executable path is invalid.")

        println("[DEBUG] useSuperuser=${services.useSuperuser} adbPath='${services.adbExecPath}'")
        return services
    }

    private suspend fun initializeServicesAndFirstDevice(): Pair<Services, AndroidDevice> {
        val services = initializeServices()
        val devicesResult = services.adbClient.listDevices()

        assertTrue(devicesResult.isOk, devicesResult.errorMessage ?: "Failed to list devices.")
        val firstDevice = devicesResult.data.orEmpty().firstOrNull()
        assertNotNull(firstDevice, "No connected Android device found.")

        return services to firstDevice
    }

    private fun ensureEnabled() {
        assumeTrue(
            "Real-device debug tests are disabled. Set ENABLE_REAL_DEVICE_TESTS = true to run.",
            ENABLE_REAL_DEVICE_TESTS
        )
    }

    private fun buildFullPath(entry: DeviceFileEntry) = if (entry.path == "/")
        "/${entry.name}"
    else "${entry.path.trimEnd('/')}/${entry.name}"

    private fun formatEntry(entry: DeviceFileEntry): String {
        val kind = when {
            entry.isDirectory -> "DIR"
            entry.isSymlink -> "LNK"
            else -> "FILE"
        }

        return "$kind ${entry.permission} ${entry.size} ${buildFullPath(entry)}"
    }
}