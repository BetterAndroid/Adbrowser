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
 * This file is created by fankes on 2025/6/4.
 */
package com.highcapable.adbrowser.frontend.cl

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ApplicationScope
import com.highcapable.adbrowser.backend.BackendFacade
import com.highcapable.adbrowser.backend.DefaultBackendFacade
import com.highcapable.adbrowser.backend.domain.AdbDevice
import com.highcapable.adbrowser.backend.domain.FsEntry
import com.highcapable.adbrowser.frontend.state.AppPreferences
import com.highcapable.adbrowser.frontend.state.AppPreferencesStore
import com.highcapable.adbrowser.frontend.state.FileSortMode
import com.highcapable.adbrowser.frontend.state.FileViewMode

/**
 * Application-level state holder, acting as the current MVVM ViewModel root.
 */
class AppState(
    val application: ApplicationScope,
    private val preferencesStore: AppPreferencesStore = AppPreferencesStore()
) {

    val backend: BackendFacade = DefaultBackendFacade()

    var preferences by mutableStateOf(preferencesStore.load())
        private set

    var isInitialSetupRequired by mutableStateOf(true)
        private set

    val devices = mutableStateListOf<AdbDevice>()
    val entries = mutableStateListOf<FsEntry>()
    private val appLogs = mutableStateListOf<String>()

    var selectedDeviceId by mutableStateOf<String?>(null)
        private set

    var currentPath by mutableStateOf("/")
        private set

    var statusMessage by mutableStateOf("")
        private set

    var viewMode by mutableStateOf(FileViewMode.List)
    var sortMode by mutableStateOf(FileSortMode.Name)
    var searchQuery by mutableStateOf("")

    init {
        initialize()
    }

    /** Initializes preferences and data loading. */
    fun initialize() {
        backend.adb.setExecutablePath(preferences.adbExecutablePath)
        isInitialSetupRequired = !backend.adb.isExecutableValid(preferences.adbExecutablePath)
        statusMessage = if (isInitialSetupRequired) {
            "ADB path is not configured or invalid."
        } else {
            "Ready"
        }
        if (!isInitialSetupRequired) refreshDevices()
    }

    /** Applies adb path from setup or preferences window. */
    fun submitAdbPath(path: String): Boolean {
        backend.adb.setExecutablePath(path)
        val isValid = backend.adb.isExecutableValid(path)
        statusMessage = if (isValid) "ADB path is valid" else "ADB path is invalid"
        if (!isValid) return false

        preferences = preferences.copy(adbExecutablePath = path.trim())
        preferencesStore.save(preferences)
        isInitialSetupRequired = false
        appendAppLog("ADB path updated: ${path.trim()}")
        refreshDevices()
        return true
    }

    /** Reloads current devices from backend. */
    fun refreshDevices() {
        val latest = backend.adb.listDevices()
        devices.clear()
        devices += latest

        val rememberedDevice = if (preferences.rememberLastDevice) selectedDeviceId else null
        selectedDeviceId = rememberedDevice?.takeIf { id -> latest.any { it.id == id } }
            ?: latest.firstOrNull()?.id

        if (selectedDeviceId != null) {
            val path = resolveInitialPath(selectedDeviceId.orEmpty())
            openPath(path)
        } else {
            entries.clear()
            statusMessage = "No devices connected"
        }
    }

    /** Selects an active device then opens initial path. */
    fun selectDevice(deviceId: String) {
        selectedDeviceId = deviceId
        val path = resolveInitialPath(deviceId)
        openPath(path)
    }

    /** Opens one directory path under current selected device. */
    fun openPath(path: String) {
        val deviceId = selectedDeviceId
        if (deviceId.isNullOrBlank()) {
            statusMessage = "No selected device"
            return
        }
        val target = normalizePath(path)
        val list = backend.fs.listEntries(deviceId, target)
        entries.clear()
        entries += list
        currentPath = target

        if (preferences.rememberDevicePath) {
            preferences = preferences.copy(
                deviceLastPaths = preferences.deviceLastPaths + (deviceId to target)
            )
            preferencesStore.save(preferences)
        }
        statusMessage = "Loaded ${list.size} entries"
    }

    /** Opens the parent directory when available. */
    fun goUp() {
        if (currentPath == "/") return
        openPath(parentPath(currentPath))
    }

    /** Creates a folder then refreshes current path. */
    fun createFolder(name: String): Boolean {
        val deviceId = selectedDeviceId ?: return false
        val success = backend.fs.createFolder(deviceId, currentPath, name)
        if (success) openPath(currentPath)
        statusMessage = if (success) "Folder created" else "Create folder failed"
        return success
    }

    /** Deletes one entry then refreshes current path. */
    fun deleteEntry(entry: FsEntry): Boolean {
        val deviceId = selectedDeviceId ?: return false
        val success = backend.fs.delete(deviceId, entry.path)
        if (success) openPath(currentPath)
        statusMessage = if (success) "Deleted ${entry.name}" else "Delete failed"
        return success
    }

    /** Renames one entry then refreshes current path. */
    fun renameEntry(entry: FsEntry, newName: String): Boolean {
        val deviceId = selectedDeviceId ?: return false
        val success = backend.fs.rename(deviceId, entry.path, newName)
        if (success) openPath(currentPath)
        statusMessage = if (success) "Renamed to $newName" else "Rename failed"
        return success
    }

    /** Updates persisted preferences and refreshes related runtime state. */
    fun updatePreferences(newPreferences: AppPreferences) {
        val adbPathChanged = preferences.adbExecutablePath != newPreferences.adbExecutablePath
        preferences = newPreferences
        preferencesStore.save(newPreferences)

        if (adbPathChanged) {
            backend.adb.setExecutablePath(newPreferences.adbExecutablePath)
            isInitialSetupRequired = !backend.adb.isExecutableValid(newPreferences.adbExecutablePath)
            if (!isInitialSetupRequired) refreshDevices()
        }
    }

    /** Returns entries based on search and sort mode. */
    fun visibleEntries(): List<FsEntry> {
        val filtered = entries.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery.trim(), ignoreCase = true)
        }
        return when (sortMode) {
            FileSortMode.Name -> filtered.sortedBy { it.name.lowercase() }
            FileSortMode.Size -> filtered.sortedByDescending { it.sizeBytes }
            FileSortMode.ModifiedTime -> filtered.sortedByDescending { it.lastModifiedEpochMillis }
        }
    }

    /** Returns backend adb logs. */
    fun adbLogs(): List<String> = backend.adb.recentLogs(300)

    /** Returns app internal logs. */
    fun appLogs(): List<String> = appLogs.toList()

    /** Appends one line into app log panel. */
    fun appendAppLog(message: String) {
        appLogs += message
        if (appLogs.size > 500) appLogs.removeAt(0)
    }

    private fun resolveInitialPath(deviceId: String): String {
        if (preferences.rememberDevicePath) {
            preferences.deviceLastPaths[deviceId]?.let { return it }
        }
        preferences.deviceHomePaths[deviceId]?.let { return it }
        return "/"
    }

    private fun normalizePath(path: String): String {
        val noTrailing = path.trim().ifBlank { "/" }.replace('\\', '/').trimEnd('/')
        return if (noTrailing.isBlank()) "/" else if (noTrailing.startsWith('/')) noTrailing else "/$noTrailing"
    }

    private fun parentPath(path: String): String {
        if (path == "/") return "/"
        val idx = path.lastIndexOf('/')
        if (idx <= 0) return "/"
        return path.take(idx)
    }
}

val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided")
}