package com.highcapable.adbrowser.frontend.debug

import com.highcapable.adbrowser.backend.AppServices
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.frontend.debug.DeviceDebugTest.Companion.ENABLE_REAL_DEVICE_TESTS
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Manual device debug tests for backend services from frontend module.
 *
 * Toggle [ENABLE_REAL_DEVICE_TESTS] to true when you want to run these tests locally.
 */
class DeviceDebugTest {

    private companion object {

        // Set to true to enable real-device debug tests.
        // Make sure you have a properly connected Android device and ADB setup before enabling.
        const val ENABLE_REAL_DEVICE_TESTS = false

        // Override with ADB_EXEC_PATH env var if needed.
        const val ADB_EXEC_PATH = ""

        // Set to true if you want to test with root permissions.
        const val USE_ROOT = false

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
            println("[DEBUG] #$index ${device.serial} ${device.name} ${device.model} online=${device.isOnline}")
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

    private suspend fun initializeServices(): AppServices {
        val services = AppServices()
        services.initialize()

        services.settingsService.current.adbExecPath = ADB_EXEC_PATH
        services.settingsService.current.useSuperuser = USE_ROOT

        val validateResult = services.adbClient.validateAdbExecPath()
        assertTrue(validateResult.isOk, validateResult.errorMessage ?: "ADB executable path is invalid.")

        println("[DEBUG] useRoot=$USE_ROOT adbPath='${services.settingsService.current.adbExecPath}'")
        return services
    }

    private suspend fun initializeServicesAndFirstDevice(): Pair<AppServices, AndroidDevice> {
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

    private fun buildFullPath(entry: DeviceFileEntry): String = if (entry.path == "/") 
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