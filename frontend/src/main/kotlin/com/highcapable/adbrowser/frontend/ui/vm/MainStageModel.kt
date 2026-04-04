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
 * This file is created by fankes on 2026/4/3.
 */
package com.highcapable.adbrowser.frontend.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.frontend.cl.AppState
import com.highcapable.adbrowser.frontend.ui.vm.base.ViewModel
import com.highcapable.adbrowser.frontend.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.frontend.ui.vm.model.PathBreadcrumbSegment
import com.highcapable.adbrowser.frontend.ui.vm.model.SelectionOption

class MainStageModel(private val appState: AppState) : ViewModel() {

    val devices = mutableStateListOf<AndroidDeviceItem>()
    val currentEntries = mutableStateListOf<DeviceFileItem>()
    val viewModes = mutableStateListOf<SelectionOption>()
    val sortModes = mutableStateListOf<SelectionOption>()
    val pathBreadcrumbSegments = mutableStateListOf<PathBreadcrumbSegment>()

    var selectedDevice by mutableStateOf<AndroidDeviceItem?>(null)
    var selectedEntry by mutableStateOf<DeviceFileItem?>(null)

    var currentPath by mutableStateOf("/")
        private set
    val pathInput = TextFieldState("/")
    var searchKeyword by mutableStateOf("")
    var statusMessage by mutableStateOf("")
    var isBusy by mutableStateOf(false)
    var isStatusBarVisible by mutableStateOf(true)
    var fileListHintMessage by mutableStateOf("")

    var selectedViewMode by mutableStateOf<SelectionOption?>(null)
    var selectedSortMode by mutableStateOf<SelectionOption?>(null)

    private val navigationHistory = mutableStateListOf("/")
    private var navigationIndex by mutableStateOf(0)

    val isListViewMode get() = selectedViewMode?.key == "list"
    val isIconViewMode get() = selectedViewMode?.key == "icons"
    val canNavigateBack get() = navigationIndex > 0
    val canNavigateForward get() = navigationIndex < navigationHistory.size - 1
    val canNavigateUp get() = currentPath != "/"

    init {
        viewModes += SelectionOption("list", "List")
        viewModes += SelectionOption("icons", "Icons")
        sortModes += SelectionOption("name", "Name")
        sortModes += SelectionOption("size", "Size")
        sortModes += SelectionOption("modified", "Modified")

        selectedViewMode = viewModes.firstOrNull()
        selectedSortMode = sortModes.firstOrNull()
    }

    fun refreshDevices() {
        // TODO: implement with backend adb service.
    }

    fun refreshEntries() {
        // TODO: implement with backend file-system service.
    }

    fun createNewFolder() {
        // TODO: implement create folder flow.
    }

    fun renameSelectedEntry() {
        // TODO: implement rename flow.
    }

    fun deleteSelectedEntry() {
        // TODO: implement delete flow.
    }

    fun showSelectedEntryProperties() {
        // TODO: implement properties dialog flow.
    }

    fun cutSelectedEntry() {
        // TODO: implement cut flow.
    }

    fun copySelectedEntry() {
        // TODO: implement copy flow.
    }

    fun pasteToCurrentPath() {
        // TODO: implement paste flow.
    }

    fun selectAllEntries() {
        // TODO: implement select-all in list/grid.
    }

    fun inverseSelectEntries() {
        // TODO: implement inverse selection in list/grid.
    }

    fun openViewModeMenu() {
        // Handled inline via Dropdown in MainScreen.
    }

    fun openSortModeMenu() {
        // Handled inline via Dropdown in MainScreen.
    }

    fun navigateBack() {
        if (!canNavigateBack) return

        navigationIndex--
        val target = navigationHistory[navigationIndex]
        applyPath(target)
        // TODO: load entries from backend.
    }

    fun navigateForward() {
        if (!canNavigateForward) return

        navigationIndex++
        val target = navigationHistory[navigationIndex]
        applyPath(target)
        // TODO: load entries from backend.
    }

    fun navigateUp() {
        if (currentPath == "/") return

        val index = currentPath.lastIndexOf('/')
        val parent = if (index <= 0) "/" else currentPath.substring(0, index)
        navigateTo(parent)
    }

    fun navigateHome() {
        // TODO: navigate to device home path from settings.
        navigateTo("/")
    }

    fun navigateRoot() {
        navigateTo("/")
    }

    fun searchInCurrentPath() {
        // TODO: implement search action.
    }

    fun toggleStatusBar() {
        isStatusBarVisible = !isStatusBarVisible
    }

    fun openPathFromInput() {
        val path = normalizePath(pathInput.text.toString())
        navigateTo(path)
    }

    fun navigateToBreadcrumb(fullPath: String) {
        if (fullPath == currentPath) return

        navigateTo(fullPath)
    }

    fun openEntry(entry: DeviceFileItem) {
        if (!entry.isDirectory) return

        val next = if (currentPath == "/") "/${entry.name}" else "${currentPath.trimEnd('/')}/${entry.name}"
        navigateTo(next)
    }

    private fun navigateTo(path: String) {
        val normalized = normalizePath(path)
        pushHistory(normalized)
        applyPath(normalized)
        // TODO: load entries from backend.
    }

    private fun applyPath(path: String) {
        currentPath = path
        if (pathInput.text.toString() != path)
            pathInput.edit { replace(0, length, path) }

        rebuildBreadcrumb(path)
    }

    private fun pushHistory(path: String) {
        if (navigationIndex < navigationHistory.size - 1)
            repeat(navigationHistory.size - 1 - navigationIndex) {
                navigationHistory.removeAt(navigationHistory.lastIndex)
            }

        if (navigationHistory.isNotEmpty() && navigationHistory.last() == path) return

        navigationHistory += path
        navigationIndex = navigationHistory.lastIndex
    }

    private fun rebuildBreadcrumb(path: String) {
        pathBreadcrumbSegments.clear()
        if (path.isBlank() || path == "/") return

        val segments = path.split("/").filter { it.isNotEmpty() }
        var current = ""
        segments.forEachIndexed { index, segment ->
            current = "$current/$segment"
            pathBreadcrumbSegments += PathBreadcrumbSegment(segment, current, index > 0)
        }
    }

    private fun normalizePath(path: String): String {
        if (path.isBlank()) return "/"

        var normalized = path.trim()
        if (!normalized.startsWith('/')) normalized = "/$normalized"

        return normalized.replace("//", "/")
    }
}