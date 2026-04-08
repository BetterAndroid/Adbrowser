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
import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.backend.permission.model.FilePermissionInfo
import com.highcapable.adbrowser.backend.setting.AppSettings
import com.highcapable.adbrowser.frontend.cl.AppState
import com.highcapable.adbrowser.frontend.ui.vm.base.ViewModel
import com.highcapable.adbrowser.frontend.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.frontend.ui.vm.model.FileEntrySnapshot
import com.highcapable.adbrowser.frontend.ui.vm.model.PathBreadcrumbSegment
import com.highcapable.adbrowser.frontend.ui.vm.model.SelectionOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainStageModel(private val appState: AppState) : ViewModel() {

    companion object {

        const val INVALID_PERMISSION_TOKEN = "__invalid_permission__"
        const val UNKNOWN_ERROR_TOKEN = "__unknown_error__"

        private const val DEVICE_PANE_MIN_WIDTH = 240f

        private const val FILE_COLUMN_MIN_WIDTH_NAME = 180f
        private const val FILE_COLUMN_MIN_WIDTH_SIZE = 90f
        private const val FILE_COLUMN_MIN_WIDTH_MODIFIED = 150f
        private const val FILE_COLUMN_MIN_WIDTH_PERMISSION = 110f
    }

    sealed interface DialogState {
        data object None : DialogState
        data object NewFolder : DialogState
        data class Rename(val initialName: String) : DialogState
        data class DeleteConfirm(val entryName: String) : DialogState
        data class Properties(val snapshot: FileEntrySnapshot) : DialogState
    }

    sealed interface StatusMessage {
        data object None : StatusMessage
        data class Res(val key: Key, val args: List<String> = emptyList()) : StatusMessage
        data class Raw(val message: String) : StatusMessage

        enum class Key {
            CommonUnknownError,
            DevicesUpdated,
            SelectDeviceFirst,
            InvalidFolderName,
            FolderCreated,
            SelectEntryFirst,
            InvalidName,
            Renamed,
            EntryDeleted,
            Copied,
            Cut,
            ClipboardEmpty,
            CrossDevicePasteNotSupported,
            Pasted,
            DialogPropertiesInvalidPermission,
            DialogPropertiesPermissionUpdated
        }
    }

    enum class FileListHint {
        None,
        EmptyFolder,
        DeviceOffline,
        DeviceNotFound,
        PathNotFound,
        PermissionDenied,
        LoadFailed
    }

    private data class ClipboardEntrySnapshot(
        val device: AndroidDeviceItem,
        val name: String,
        val fullPath: String,
        val isCut: Boolean
    )

    class DeviceWorkspaceState internal constructor(device: AndroidDeviceItem) {
        var device by mutableStateOf(device)
        var currentPath by mutableStateOf("/")
        val pathInput = TextFieldState("/")
        val currentEntries = mutableStateListOf<DeviceFileItem>()
        var selectedEntry by mutableStateOf<DeviceFileItem?>(null)
        val pathBreadcrumbSegments = mutableStateListOf<PathBreadcrumbSegment>()
        val navigationHistory = mutableStateListOf("/")
        var navigationIndex by mutableStateOf(0)
        var fileListHint by mutableStateOf(FileListHint.None)
        var directoryChangeVersion by mutableStateOf(0)
        var listScrollIndex by mutableStateOf(0)
        var listScrollOffset by mutableStateOf(0)
        var listHorizontalScrollOffset by mutableStateOf(0)
        var iconScrollRowIndex by mutableStateOf(0)
        var iconScrollRowOffset by mutableStateOf(0)
        var prebuilt by mutableStateOf(false)
    }

    private val adbClient get() = appState.appServices.adbClient
    private val fileSystemService get() = appState.appServices.fileSystemService
    private val permissionService get() = appState.appServices.permissionService
    private val settingsService get() = appState.appServices.settingsService

    private val modelJob = SupervisorJob()
    private val modelScope = CoroutineScope(modelJob + Dispatchers.Main.immediate)
    private var deviceObserverJob: Job? = null

    private var clipboardEntry: ClipboardEntrySnapshot? = null
    private var initialized = false
    private var busyCount = 0
    private val fallbackPathInput = TextFieldState("/")
    private val emptyEntries = mutableStateListOf<DeviceFileItem>()
    private val emptyBreadcrumbSegments = mutableStateListOf<PathBreadcrumbSegment>()
    private val activeWorkspace get() = selectedDevice?.let(::workspace)

    private val workspaces = mutableMapOf<AndroidDeviceItem, DeviceWorkspaceState>()

    val devices = mutableStateListOf<AndroidDeviceItem>()
    
    val deviceWorkspaces = mutableStateListOf<DeviceWorkspaceState>()
    val viewModes = mutableStateListOf<SelectionOption>()
    val sortModes = mutableStateListOf<SelectionOption>()

    var selectedDevice by mutableStateOf<AndroidDeviceItem?>(null)
        private set
    var selectedEntry: DeviceFileItem?
        get() = activeWorkspace?.selectedEntry
        set(value) {
            activeWorkspace?.selectedEntry = value
        }

    val currentPath: String
        get() = activeWorkspace?.currentPath ?: "/"

    val pathInput: TextFieldState
        get() = activeWorkspace?.pathInput ?: fallbackPathInput

    val currentEntries: List<DeviceFileItem>
        get() = activeWorkspace?.currentEntries ?: emptyEntries

    val pathBreadcrumbSegments: List<PathBreadcrumbSegment>
        get() = activeWorkspace?.pathBreadcrumbSegments ?: emptyBreadcrumbSegments

    val fileListHint: FileListHint
        get() = activeWorkspace?.fileListHint ?: FileListHint.None
    var statusMessage by mutableStateOf<StatusMessage>(StatusMessage.None)
        private set
    var isBusy by mutableStateOf(false)
    var isStatusBarVisible by mutableStateOf(true)

    var selectedViewMode by mutableStateOf<SelectionOption?>(null)
        private set
    var selectedSortMode by mutableStateOf<SelectionOption?>(null)
        private set

    var dialogState by mutableStateOf<DialogState>(DialogState.None)
        private set

    val isListViewMode get() = selectedViewMode?.key == "list"
    val isIconViewMode get() = selectedViewMode?.key == "icons"
    val canNavigateBack get() = (activeWorkspace?.navigationIndex ?: 0) > 0
    val canNavigateForward get() = activeWorkspace?.let { it.navigationIndex < it.navigationHistory.size - 1 } == true
    val canNavigateUp get() = currentPath != "/"
    val canNavigateRoot get() = selectedDevice != null && currentPath != "/"
    val canPasteEntry get() = selectedDevice?.let { clipboardEntry?.device == it } == true

    val hasSelectedEntry get() = selectedDevice != null && selectedEntry != null

    var devicePaneWidthDp by mutableStateOf(settingsService.current.devicePaneWidth.toFloat().coerceAtLeast(DEVICE_PANE_MIN_WIDTH))
        private set

    var fileColumnWidthNamePx by mutableStateOf(settingsService.current.fileColumnWidthName.toFloat())
        private set
    var fileColumnWidthSizePx by mutableStateOf(settingsService.current.fileColumnWidthSize.toFloat())
        private set
    var fileColumnWidthModifiedPx by mutableStateOf(settingsService.current.fileColumnWidthModified.toFloat())
        private set
    var fileColumnWidthPermissionPx by mutableStateOf(settingsService.current.fileColumnWidthPermission.toFloat())
        private set

    init {
        viewModes += SelectionOption("list", "List")
        viewModes += SelectionOption("icons", "Icons")
        sortModes += SelectionOption("name", "Name")
        sortModes += SelectionOption("size", "Size")
        sortModes += SelectionOption("modified", "Modified")

        selectedSortMode = sortModes.firstOrNull()
        applyDisplayStylePreference()
        normalizeFileColumnWidths()
    }

    fun workspace(device: AndroidDeviceItem) = workspaces[device]

    fun isSelectedWorkspace(device: AndroidDeviceItem) = selectedDevice == device

    fun setDevicePaneWidth(widthDp: Float) {
        devicePaneWidthDp = widthDp.coerceAtLeast(DEVICE_PANE_MIN_WIDTH)
    }

    fun persistDevicePaneWidth() {
        settingsService.current.devicePaneWidth = devicePaneWidthDp.toDouble()
        saveSettingsAsync()
    }

    fun onExternalSettingsChanged(refreshFileList: Boolean = false) {
        val settings = settingsService.current
        devicePaneWidthDp = settings.devicePaneWidth.toFloat().coerceAtLeast(DEVICE_PANE_MIN_WIDTH)
        fileColumnWidthNamePx = settings.fileColumnWidthName.toFloat()
        fileColumnWidthSizePx = settings.fileColumnWidthSize.toFloat()
        fileColumnWidthModifiedPx = settings.fileColumnWidthModified.toFloat()
        fileColumnWidthPermissionPx = settings.fileColumnWidthPermission.toFloat()
        normalizeFileColumnWidths()
        applyDisplayStylePreference()
        if (refreshFileList)
            selectedDevice?.let { refreshEntriesAsync(device = it, requestedPath = currentPath) }
    }

    fun resizeNameAndSizeColumns(deltaDp: Float) {
        fileColumnWidthNamePx = (fileColumnWidthNamePx + deltaDp).coerceAtLeast(FILE_COLUMN_MIN_WIDTH_NAME)
    }

    fun resizeSizeAndModifiedColumns(deltaDp: Float) {
        fileColumnWidthSizePx = (fileColumnWidthSizePx + deltaDp).coerceAtLeast(FILE_COLUMN_MIN_WIDTH_SIZE)
    }

    fun resizeModifiedAndPermissionColumns(deltaDp: Float) {
        fileColumnWidthModifiedPx = (fileColumnWidthModifiedPx + deltaDp).coerceAtLeast(FILE_COLUMN_MIN_WIDTH_MODIFIED)
    }

    fun persistFileColumnWidths() {
        settingsService.current.fileColumnWidthName = fileColumnWidthNamePx.toDouble()
        settingsService.current.fileColumnWidthSize = fileColumnWidthSizePx.toDouble()
        settingsService.current.fileColumnWidthModified = fileColumnWidthModifiedPx.toDouble()
        settingsService.current.fileColumnWidthPermission = fileColumnWidthPermissionPx.toDouble()
        saveSettingsAsync()
    }

    fun initialize() {
        if (initialized) return

        initialized = true
        refreshDevices(showStatus = false)
        startObserveDevices()
    }

    fun dispose() {
        deviceObserverJob?.cancel()
        modelScope.cancel()
    }

    fun selectDevice(device: AndroidDeviceItem) {
        if (selectedDevice == device) return

        selectedDevice = device
        rememberLastSelectedDevice(device)
        saveSettingsAsync()

        val state = ensureWorkspace(device)
        if (state.prebuilt) return

        refreshEntriesAsync(
            device = device,
            useRememberedPathWhenRequestedPathIsNull = true
        ) { success ->
            if (success) setHistoryToCurrentPath(state)
            state.prebuilt = success
        }
    }

    fun refreshDevices(showStatus: Boolean = true) = launchBusyAction {
        consumeDeviceListResult(
            result = adbClient.listDevices(),
            showStatus = showStatus
        )
    }

    private fun startObserveDevices() {
        if (deviceObserverJob?.isActive == true) return

        deviceObserverJob = modelScope.launch {
            try {
                adbClient.observeDevices().collect { result ->
                    consumeDeviceListResult(
                        result = result,
                        showStatus = false
                    )
                }
            } catch (_: CancellationException) {
                // Ignore cancellation when window/app is closing.
            } catch (t: Throwable) {
                setErrorStatus(t.message)
            }
        }
    }

    private suspend fun consumeDeviceListResult(result: OperationResult<List<AndroidDevice>>, showStatus: Boolean) {
        if (!result.isOk) {
            setErrorStatus(result.errorMessage)
            return
        }

        val previousSelectedDevice = selectedDevice

        devices.clear()
        devices += result.data.orEmpty().map { AndroidDeviceItem.from(it) }
        reconcileWorkspaces()

        val targetDevice = previousSelectedDevice
            ?.let { previous -> devices.firstOrNull { it == previous } }
            ?: selectDefaultDevice()

        if (showStatus) setStatus(StatusMessage.Key.DevicesUpdated)

        if (targetDevice == null) {
            selectedDevice = null
            return
        }

        selectedDevice = targetDevice
        val selectedState = ensureWorkspace(targetDevice)
        if (!selectedState.prebuilt) {
            if (refreshEntriesInternal(selectedState, targetDevice.toDomain(), useRememberedPathWhenRequestedPathIsNull = true)) {
                setHistoryToCurrentPath(selectedState)
                selectedState.prebuilt = true
            }
        }

        prebuildWorkspacesInBackground(skipDevice = targetDevice)
    }

    fun refreshEntries() {
        val device = selectedDevice ?: return
        refreshEntriesAsync(device = device, requestedPath = currentPath)
    }

    fun createNewFolder() {
        if (!ensureDeviceSelected()) return

        dialogState = DialogState.NewFolder
    }

    fun confirmCreateFolder(folderName: String): Boolean {
        val device = selectedDevice?.toDomain() ?: return false
        val name = folderName.trim()
        if (name.isBlank()) {
            setStatus(StatusMessage.Key.InvalidFolderName)
            return false
        }

        val result = runBlocking { fileSystemService.createFolder(device, currentPath, name) }
        if (!result.isOk) {
            setErrorStatus(result.errorMessage)
            return false
        }

        setStatus(StatusMessage.Key.FolderCreated, name)
        selectedDevice?.let { refreshEntriesAsync(device = it, requestedPath = currentPath) }

        return true
    }

    fun renameSelectedEntry() {
        val entry = selectedEntry ?: return

        dialogState = DialogState.Rename(initialName = entry.name)
    }

    fun confirmRenameSelectedEntry(newName: String): Boolean {
        val device = selectedDevice?.toDomain() ?: return false
        val entry = selectedEntry ?: return false

        val targetName = newName.trim()
        if (targetName.isBlank()) {
            setStatus(StatusMessage.Key.InvalidName)
            return false
        }

        val sourcePath = buildEntryFullPath(entry)
        val result = runBlocking { fileSystemService.rename(device, sourcePath, targetName) }
        if (!result.isOk) {
            setErrorStatus(result.errorMessage)
            return false
        }

        setStatus(StatusMessage.Key.Renamed, entry.name, targetName)
        selectedDevice?.let { refreshEntriesAsync(device = it, requestedPath = currentPath) }

        return true
    }

    fun deleteSelectedEntry() {
        val entry = selectedEntry ?: return

        dialogState = DialogState.DeleteConfirm(entryName = entry.name)
    }

    fun confirmDeleteSelectedEntry(): Boolean {
        val device = selectedDevice?.toDomain() ?: return false
        val entry = selectedEntry ?: return false

        val path = buildEntryFullPath(entry)
        val result = runBlocking { fileSystemService.delete(device, path) }
        if (!result.isOk) {
            setErrorStatus(result.errorMessage)
            return false
        }

        setStatus(StatusMessage.Key.EntryDeleted, entry.name)
        selectedDevice?.let { refreshEntriesAsync(device = it, requestedPath = currentPath) }

        return true
    }

    fun showSelectedEntryProperties() {
        val snapshot = buildSelectedEntrySnapshot() ?: return

        dialogState = DialogState.Properties(snapshot)
    }

    fun openSelectedEntry() {
        val device = selectedDevice ?: return
        val entry = selectedEntry ?: return
        openEntry(device, entry)
    }

    fun openSelectedEntryWith() {
        val device = selectedDevice ?: return
        val entry = selectedEntry ?: return
        openEntryWith(device, entry)
    }

    fun dismissDialog() {
        dialogState = DialogState.None
    }

    fun copySelectedEntry() {
        val device = selectedDevice
        val entry = selectedEntry
        if (device == null || entry == null) return

        clipboardEntry = ClipboardEntrySnapshot(
            device = device,
            name = entry.name,
            fullPath = buildEntryFullPath(entry),
            isCut = false
        )
        setStatus(StatusMessage.Key.Copied, entry.name)
    }

    fun cutSelectedEntry() {
        val device = selectedDevice
        val entry = selectedEntry
        if (device == null || entry == null) return

        clipboardEntry = ClipboardEntrySnapshot(
            device = device,
            name = entry.name,
            fullPath = buildEntryFullPath(entry),
            isCut = true
        )
        setStatus(StatusMessage.Key.Cut, entry.name)
    }

    fun pasteToCurrentPath() {
        val device = selectedDevice
        if (device == null) {
            setStatus(StatusMessage.Key.SelectDeviceFirst)
            return
        }

        val clipboard = clipboardEntry
        if (clipboard == null) {
            setStatus(StatusMessage.Key.ClipboardEmpty)
            return
        }

        if (clipboard.device != device) {
            setStatus(StatusMessage.Key.CrossDevicePasteNotSupported)
            return
        }

        val targetPath = if (currentPath == "/")
            "/${clipboard.name}"
        else "${currentPath.trimEnd('/')}/${clipboard.name}"

        launchBusyAction {
            val result = if (clipboard.isCut) {
                fileSystemService.move(device.toDomain(), clipboard.fullPath, targetPath)
            } else {
                fileSystemService.copy(device.toDomain(), clipboard.fullPath, targetPath)
            }

            if (!result.isOk) {
                setErrorStatus(result.errorMessage)
                return@launchBusyAction
            }

            if (clipboard.isCut) clipboardEntry = null
            setStatus(StatusMessage.Key.Pasted, clipboard.name)
            selectedDevice?.let { selected ->
                val workspace = ensureWorkspace(selected)
                refreshEntriesInternal(workspace, selected.toDomain(), requestedPath = workspace.currentPath)
            }
        }
    }

    fun selectAllEntries() {
        selectedEntry = currentEntries.firstOrNull()
    }

    fun inverseSelectEntries() {
        selectedEntry = if (selectedEntry == null) currentEntries.firstOrNull() else null
    }

    fun openViewModeMenu() {
        // Handled inline via Dropdown.
    }

    fun openSortModeMenu() {
        // Handled inline via Dropdown.
    }

    fun onViewModeSelected(option: SelectionOption) {
        selectedViewMode = option
        if (settingsService.current.rememberLastDisplayStyle) {
            settingsService.current.lastFileViewMode = if (option.key == "icons")
                AppSettings.FileViewMode.Grid
            else AppSettings.FileViewMode.List
            saveSettingsAsync()
        }
    }

    fun onSortModeSelected(option: SelectionOption) {
        selectedSortMode = option
        deviceWorkspaces.forEach { applySort(it) }
    }

    fun navigateBack() {
        selectedDevice?.let(::navigateBack)
    }

    fun navigateForward() {
        selectedDevice?.let(::navigateForward)
    }

    fun navigateUp() {
        selectedDevice?.let(::navigateUp)
    }

    fun navigateHome() {
        val device = selectedDevice ?: return
        val remembered = deviceHomePaths(device)
        if (remembered.isNullOrBlank()) {
            navigateTo(device, "/")
            return
        }

        navigateTo(device, remembered)
    }

    fun navigateHome(device: AndroidDeviceItem) {
        val remembered = deviceHomePaths(device)
        if (remembered.isNullOrBlank()) {
            navigateTo(device, "/")
            return
        }

        navigateTo(device, remembered)
    }

    fun navigateRoot() {
        selectedDevice?.let { navigateTo(it, "/") }
    }

    fun toggleStatusBar() {
        isStatusBarVisible = !isStatusBarVisible
    }

    fun openPathFromInput() {
        selectedDevice?.let(::openPathFromInput)
    }

    fun navigateToBreadcrumb(fullPath: String) {
        selectedDevice?.let { navigateToBreadcrumb(it, fullPath) }
    }

    fun openEntry(entry: DeviceFileItem) {
        selectedDevice?.let { openEntry(it, entry) }
    }

    fun navigateBack(device: AndroidDeviceItem) {
        val state = workspace(device) ?: return
        if (state.navigationIndex <= 0) return

        val targetIndex = state.navigationIndex - 1
        val targetPath = state.navigationHistory[targetIndex]

        refreshEntriesAsync(
            device = device,
            requestedPath = targetPath,
            useRememberedPathWhenRequestedPathIsNull = false
        ) { success ->
            if (success) state.navigationIndex = targetIndex
        }
    }

    fun navigateForward(device: AndroidDeviceItem) {
        val state = workspace(device) ?: return
        if (state.navigationIndex >= state.navigationHistory.size - 1) return

        val targetIndex = state.navigationIndex + 1
        val targetPath = state.navigationHistory[targetIndex]

        refreshEntriesAsync(
            device = device,
            requestedPath = targetPath,
            useRememberedPathWhenRequestedPathIsNull = false
        ) { success ->
            if (success) state.navigationIndex = targetIndex
        }
    }

    fun navigateUp(device: AndroidDeviceItem) {
        val state = workspace(device) ?: return
        if (state.currentPath == "/") return

        val index = state.currentPath.lastIndexOf('/')
        val parent = if (index <= 0) "/" else state.currentPath.substring(0, index)
        navigateTo(device, parent)
    }

    fun openPathFromInput(device: AndroidDeviceItem) {
        val state = workspace(device) ?: return
        val path = normalizePath(state.pathInput.text.toString())
        navigateTo(device, path)
    }

    fun navigateToBreadcrumb(device: AndroidDeviceItem, fullPath: String) {
        val state = workspace(device) ?: return
        val normalized = normalizePath(fullPath)
        if (normalized == state.currentPath) return

        navigateTo(device, normalized)
    }

    fun openEntry(device: AndroidDeviceItem, entry: DeviceFileItem) {
        val state = workspace(device) ?: return
        if (!entry.isDirectory) return // TODO: Support opening files with associated applications in the future.

        val next = if (state.currentPath == "/") "/${entry.name}" else "${state.currentPath.trimEnd('/')}/${entry.name}"
        navigateTo(device, next)
    }

    fun openEntryWith(device: AndroidDeviceItem, entry: DeviceFileItem) {
        // TODO: Implement "Open With" functionality.
    }

    fun setSelectedEntry(device: AndroidDeviceItem, entry: DeviceFileItem?) {
        workspace(device)?.selectedEntry = entry
    }

    fun selectedEntryOf(device: AndroidDeviceItem) = workspace(device)?.selectedEntry
    fun entriesOf(device: AndroidDeviceItem) = workspace(device)?.currentEntries ?: emptyEntries
    fun directoryChangeVersionOf(device: AndroidDeviceItem) = workspace(device)?.directoryChangeVersion ?: 0
    fun listScrollIndexOf(device: AndroidDeviceItem) = workspace(device)?.listScrollIndex ?: 0
    fun listScrollOffsetOf(device: AndroidDeviceItem) = workspace(device)?.listScrollOffset ?: 0
    fun listHorizontalScrollOffsetOf(device: AndroidDeviceItem) = workspace(device)?.listHorizontalScrollOffset ?: 0
    fun iconScrollRowIndexOf(device: AndroidDeviceItem) = workspace(device)?.iconScrollRowIndex ?: 0
    fun iconScrollRowOffsetOf(device: AndroidDeviceItem) = workspace(device)?.iconScrollRowOffset ?: 0

    fun updateListScrollState(device: AndroidDeviceItem, index: Int, offset: Int) {
        workspace(device)?.let {
            it.listScrollIndex = index.coerceAtLeast(0)
            it.listScrollOffset = offset.coerceAtLeast(0)
        }
    }

    fun updateListHorizontalScrollState(device: AndroidDeviceItem, offset: Int) {
        workspace(device)?.listHorizontalScrollOffset = offset.coerceAtLeast(0)
    }

    fun updateIconScrollState(device: AndroidDeviceItem, rowIndex: Int, rowOffset: Int) {
        workspace(device)?.let {
            it.iconScrollRowIndex = rowIndex.coerceAtLeast(0)
            it.iconScrollRowOffset = rowOffset.coerceAtLeast(0)
        }
    }

    fun pathInputOf(device: AndroidDeviceItem) = workspace(device)?.pathInput ?: fallbackPathInput
    fun breadcrumbsOf(device: AndroidDeviceItem) = workspace(device)?.pathBreadcrumbSegments ?: emptyBreadcrumbSegments
    fun fileListHintOf(device: AndroidDeviceItem) = workspace(device)?.fileListHint ?: FileListHint.None
    fun canNavigateBack(device: AndroidDeviceItem) = (workspace(device)?.navigationIndex ?: 0) > 0
    fun canNavigateForward(device: AndroidDeviceItem) =
        workspace(device)?.let { it.navigationIndex < it.navigationHistory.size - 1 } == true
    fun canNavigateUp(device: AndroidDeviceItem) = workspace(device)?.currentPath?.let { it != "/" } ?: false

    fun loadPermission(snapshot: FileEntrySnapshot) = runBlocking {
        permissionService.getPermission(snapshot.device, snapshot.fullPath)
    }

    fun applyPermission(
        snapshot: FileEntrySnapshot,
        modeText: String,
        reportStatus: Boolean = true
    ): OperationResult<FilePermissionInfo> {
        val mode = parsePermissionMode(modeText)
        if (mode == null) {
            if (reportStatus) setStatus(StatusMessage.Key.DialogPropertiesInvalidPermission)
            return OperationResult.failure(INVALID_PERMISSION_TOKEN)
        }

        val setResult = runBlocking { permissionService.setPermission(snapshot.device, snapshot.fullPath, mode) }
        if (!setResult.isOk) {
            if (reportStatus) setErrorStatus(setResult.errorMessage)
            return OperationResult.failure(setResult.errorMessage.orUnknownErrorToken())
        }

        val getResult = runBlocking { permissionService.getPermission(snapshot.device, snapshot.fullPath) }
        val info = getResult.data
        if (!getResult.isOk || info == null) {
            if (reportStatus) setErrorStatus(getResult.errorMessage)
            return OperationResult.failure(getResult.errorMessage.orUnknownErrorToken())
        }

        if (reportStatus) setStatus(StatusMessage.Key.DialogPropertiesPermissionUpdated)
        updateEntryPermission(snapshot.fullPath, info.symbolicPermission)

        return OperationResult.success(info)
    }

    private fun navigateTo(device: AndroidDeviceItem, path: String) {
        val state = workspace(device) ?: return
        val normalized = normalizePath(path)

        refreshEntriesAsync(
            device = device,
            requestedPath = normalized,
            useRememberedPathWhenRequestedPathIsNull = false
        ) { success ->
            if (!success) return@refreshEntriesAsync
            pushHistory(state, state.currentPath)
        }
    }

    private fun refreshEntriesAsync(
        device: AndroidDeviceItem,
        requestedPath: String? = null,
        useRememberedPathWhenRequestedPathIsNull: Boolean = false,
        onCompleted: (Boolean) -> Unit = {}
    ) {
        launchBusyAction {
            val state = ensureWorkspace(device)
            val success = refreshEntriesInternal(
                state = state,
                device = device.toDomain(),
                requestedPath = requestedPath,
                useRememberedPathWhenRequestedPathIsNull = useRememberedPathWhenRequestedPathIsNull
            )
            onCompleted(success)
        }
    }

    private suspend fun refreshEntriesInternal(
        state: DeviceWorkspaceState,
        device: AndroidDevice,
        requestedPath: String? = null,
        useRememberedPathWhenRequestedPathIsNull: Boolean = false
    ): Boolean {
        val targetPath = resolveTargetPath(state, device, requestedPath, useRememberedPathWhenRequestedPathIsNull)
        val previousPath = state.currentPath
        val result = fileSystemService.list(device, targetPath)

        applyPath(state, targetPath)

        if (result.isOk) {
            fillEntries(state, result.data.orEmpty())
            state.fileListHint = if (state.currentEntries.isEmpty()) FileListHint.EmptyFolder else FileListHint.None
            if (state.currentPath != previousPath)
                state.directoryChangeVersion += 1
            persistCurrentPath(state, device)
        } else {
            fillEntries(state, emptyList())
            state.fileListHint = resolveFailureHint(result.errorMessage)
            setErrorStatus(result.errorMessage)
        }

        return true
    }

    private fun resolveTargetPath(
        state: DeviceWorkspaceState,
        device: AndroidDevice,
        requestedPath: String?,
        useRememberedPathWhenRequestedPathIsNull: Boolean
    ): String {
        if (!requestedPath.isNullOrBlank()) return normalizePath(requestedPath)

        if (useRememberedPathWhenRequestedPathIsNull &&
            settingsService.current.rememberDevicePath
        ) deviceLastPaths(device)?.let { return normalizePath(it) }

        return normalizePath(state.currentPath)
    }

    private fun buildSelectedEntrySnapshot(): FileEntrySnapshot? {
        val device = selectedDevice?.toDomain() ?: return null
        val entry = selectedEntry ?: return null

        return FileEntrySnapshot(
            device = device,
            name = entry.name,
            fullPath = buildEntryFullPath(entry),
            isDirectory = entry.isDirectory,
            isSymlink = entry.isSymbolicLink,
            sizeBytes = entry.sizeBytes,
            modifiedAt = entry.modifiedAt,
            symbolicPermission = entry.permission
        )
    }

    private fun updateEntryPermission(fullPath: String, symbolicPermission: String) {
        if (fullPath.isBlank() || symbolicPermission.isBlank()) return

        val state = activeWorkspace ?: return
        val index = state.currentEntries.indexOfFirst { buildEntryFullPath(it) == fullPath }
        if (index < 0) return

        val old = state.currentEntries[index]
        state.currentEntries[index] = old.copy(permission = symbolicPermission)

        if (state.selectedEntry == old) state.selectedEntry = state.currentEntries[index]
    }

    private fun fillEntries(state: DeviceWorkspaceState, entries: List<DeviceFileEntry>) {
        val showHidden = settingsService.current.showHiddenFiles
        val selectedPath = state.selectedEntry?.let { buildEntryFullPath(it) }

        state.currentEntries.clear()
        state.currentEntries += entries
            .asSequence()
            .filter { showHidden || !isHiddenEntryName(it.name) }
            .map { DeviceFileItem.from(it) }
            .toList()

        applySort(state)

        state.selectedEntry = selectedPath?.let { path ->
            state.currentEntries.firstOrNull { buildEntryFullPath(it) == path }
        }
    }

    private fun applySort(state: DeviceWorkspaceState) {
        val sortMode = selectedSortMode?.key ?: "name"
        val foldersFirst = settingsService.current.foldersFirst

        val sorted = if (foldersFirst) when (sortMode) {
            "size" -> state.currentEntries.sortedWith(
                compareByDescending<DeviceFileItem> { it.isDirectory }
                    .thenByDescending { it.sizeBytes }
                    .thenBy { it.name.lowercase() }
            )
            "modified" -> state.currentEntries.sortedWith(
                compareByDescending<DeviceFileItem> { it.isDirectory }
                    .thenByDescending { it.modifiedAt }
                    .thenBy { it.name.lowercase() }
            )
            else -> state.currentEntries.sortedWith(
                compareByDescending<DeviceFileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        } else when (sortMode) {
            "size" -> state.currentEntries.sortedWith(
                compareByDescending<DeviceFileItem> { it.sizeBytes }
                    .thenBy { it.name.lowercase() }
            )
            "modified" -> state.currentEntries.sortedWith(
                compareByDescending<DeviceFileItem> { it.modifiedAt }
                    .thenBy { it.name.lowercase() }
            )
            else -> state.currentEntries.sortedBy { it.name.lowercase() }
        }

        state.currentEntries.clear()
        state.currentEntries += sorted
    }

    private fun ensureWorkspace(device: AndroidDeviceItem): DeviceWorkspaceState {
        val existing = workspaces[device]
        if (existing != null) {
            if (existing.device != device) existing.device = device
            return existing
        }

        val created = DeviceWorkspaceState(device)
        workspaces[device] = created
        deviceWorkspaces += created

        return created
    }

    private fun reconcileWorkspaces() {
        val deviceItems = devices.toSet()

        val removedItems = workspaces.keys.filter { it !in deviceItems }
        removedItems.forEach { workspaces.remove(it) }

        val ordered = mutableListOf<DeviceWorkspaceState>()
        devices.forEach { device ->
            ordered += ensureWorkspace(device)
        }

        deviceWorkspaces.clear()
        deviceWorkspaces += ordered
    }

    private fun prebuildWorkspacesInBackground(skipDevice: AndroidDeviceItem? = null) {
        modelScope.launch {
            deviceWorkspaces.forEach { workspace ->
                if (workspace.device == skipDevice) return@forEach
                if (workspace.prebuilt) return@forEach

                runCatching {
                    val success = refreshEntriesInternal(
                        state = workspace,
                        device = workspace.device.toDomain(),
                        useRememberedPathWhenRequestedPathIsNull = true
                    )
                    workspace.prebuilt = success
                    if (success) setHistoryToCurrentPath(workspace)
                }
            }
        }
    }

    private fun selectDefaultDevice() = if (settingsService.current.rememberLastDevice)
        rememberedSelectedDevice() ?: devices.firstOrNull()
    else devices.firstOrNull()

    private fun applyDisplayStylePreference() {
        val target = if (settingsService.current.rememberLastDisplayStyle)
            settingsService.current.lastFileViewMode
        else AppSettings.FileViewMode.List

        selectedViewMode = when (target) {
            AppSettings.FileViewMode.Grid -> viewModes.firstOrNull { it.key == "icons" }
            AppSettings.FileViewMode.List -> viewModes.firstOrNull { it.key == "list" }
        } ?: viewModes.first()
    }

    private fun normalizeFileColumnWidths() {
        devicePaneWidthDp = devicePaneWidthDp.coerceAtLeast(DEVICE_PANE_MIN_WIDTH)
        fileColumnWidthNamePx = fileColumnWidthNamePx.coerceAtLeast(FILE_COLUMN_MIN_WIDTH_NAME)
        fileColumnWidthSizePx = fileColumnWidthSizePx.coerceAtLeast(FILE_COLUMN_MIN_WIDTH_SIZE)
        fileColumnWidthModifiedPx = fileColumnWidthModifiedPx.coerceAtLeast(FILE_COLUMN_MIN_WIDTH_MODIFIED)
        fileColumnWidthPermissionPx = fileColumnWidthPermissionPx.coerceAtLeast(FILE_COLUMN_MIN_WIDTH_PERMISSION)
    }

    private fun launchBusyAction(block: suspend () -> Unit) {
        modelScope.launch {
            beginBusy()
            try {
                block()
            } catch (t: Throwable) {
                setErrorStatus(t.message)
            } finally {
                endBusy()
            }
        }
    }

    private fun saveSettingsAsync() {
        modelScope.launch {
            runCatching { settingsService.save() }
        }
    }

    private fun beginBusy() {
        busyCount++
        isBusy = busyCount > 0
    }

    private fun endBusy() {
        busyCount = (busyCount - 1).coerceAtLeast(0)
        isBusy = busyCount > 0
    }

    private fun pushHistory(state: DeviceWorkspaceState, path: String) {
        if (state.navigationIndex < state.navigationHistory.size - 1)
            repeat(state.navigationHistory.size - 1 - state.navigationIndex) {
                state.navigationHistory.removeAt(state.navigationHistory.lastIndex)
            }

        if (state.navigationHistory.isNotEmpty() && state.navigationHistory.last() == path) return

        state.navigationHistory += path
        state.navigationIndex = state.navigationHistory.lastIndex
    }

    private fun setHistoryToCurrentPath(state: DeviceWorkspaceState) {
        clearHistory(state)
        state.navigationHistory += state.currentPath
        state.navigationIndex = state.navigationHistory.lastIndex
    }

    private fun clearHistory(state: DeviceWorkspaceState) {
        state.navigationHistory.clear()
        state.navigationIndex = 0
    }

    private fun persistCurrentPath(state: DeviceWorkspaceState, device: AndroidDevice) {
        if (!settingsService.current.rememberDevicePath) return

        updateDeviceLastPaths(device, state.currentPath)
        saveSettingsAsync()
    }

    private fun rememberLastSelectedDevice(device: AndroidDeviceItem) {
        settingsService.current.lastDeviceSerial = device.serial
    }

    private fun rememberedSelectedDevice(): AndroidDeviceItem? {
        val rememberedSerial = settingsService.current.lastDeviceSerial
            .takeIf { it.isNotBlank() }
            ?: return null

        return devices.firstOrNull { it.serial == rememberedSerial }
    }

    private fun deviceHomePaths(device: AndroidDeviceItem) =
        settingsService.current.deviceHomePaths[device.serial]

    private fun deviceLastPaths(device: AndroidDevice) =
        settingsService.current.deviceLastPaths[device.serial]
            ?.takeIf { it.isNotBlank() }

    private fun updateDeviceLastPaths(device: AndroidDevice, path: String) {
        settingsService.current.deviceLastPaths[device.serial] = path
    }

    private fun applyPath(state: DeviceWorkspaceState, path: String) {
        state.currentPath = normalizePath(path)
        if (state.pathInput.text.toString() != state.currentPath)
            state.pathInput.edit { replace(0, length, state.currentPath) }

        rebuildBreadcrumb(state, state.currentPath)
    }

    private fun rebuildBreadcrumb(state: DeviceWorkspaceState, path: String) {
        state.pathBreadcrumbSegments.clear()
        if (path.isBlank() || path == "/") return

        val segments = path.split("/").filter { it.isNotBlank() }
        var current = ""
        segments.forEachIndexed { index, segment ->
            current = "$current/$segment"
            state.pathBreadcrumbSegments += PathBreadcrumbSegment(
                displayName = segment,
                fullPath = current,
                showLeadingArrow = index > 0
            )
        }
    }

    private fun buildEntryFullPath(entry: DeviceFileItem) = if (entry.path == "/")
        "/${entry.name}"
    else "${entry.path.trimEnd('/')}/${entry.name}"

    private fun normalizePath(path: String): String {
        if (path.isBlank()) return "/"

        var normalized = path.trim()
        if (!normalized.startsWith('/')) normalized = "/$normalized"

        while (normalized.contains("//")) normalized = normalized.replace("//", "/")

        return normalized
    }

    private fun resolveFailureHint(errorMessage: String?): FileListHint {
        val message = errorMessage.orEmpty().lowercase()

        return when {
            "device offline" in message -> FileListHint.DeviceOffline
            ("device '" in message && "' not found" in message) ||
                ("device" in message && "not found" in message && "error:" in message) -> FileListHint.DeviceNotFound
            "no such file or directory" in message || "not found" in message -> FileListHint.PathNotFound
            "permission denied" in message ||
                "operation not permitted" in message || 
                "not permitted" in message -> FileListHint.PermissionDenied
            else -> FileListHint.LoadFailed
        }
    }

    @Suppress("KotlinConstantConditions")
    private fun parsePermissionMode(modeText: String): Int? {
        val value = modeText.trim()
        val parsed = value.toIntOrNull() ?: return null
        if (parsed !in 0..777) return null

        val owner = parsed / 100
        val group = (parsed / 10) % 10
        val other = parsed % 10
        if (owner > 7 || group > 7 || other > 7) return null

        return parsed
    }

    private fun ensureDeviceSelected(): Boolean {
        if (selectedDevice != null) return true

        setStatus(StatusMessage.Key.SelectDeviceFirst)
        return false
    }

    private fun isHiddenEntryName(name: String): Boolean =
        name.isNotBlank() && name.startsWith('.')

    private fun setStatus(key: StatusMessage.Key, vararg args: String) {
        statusMessage = StatusMessage.Res(key = key, args = args.toList())
    }

    private fun setErrorStatus(message: String?) {
        statusMessage = message
            ?.takeIf { it.isNotBlank() }
            ?.let { StatusMessage.Raw(it) }
            ?: StatusMessage.Res(StatusMessage.Key.CommonUnknownError)
    }

    private fun String?.orUnknownErrorToken(): String =
        this?.takeIf { it.isNotBlank() } ?: UNKNOWN_ERROR_TOKEN
}