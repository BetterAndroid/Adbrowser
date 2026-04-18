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
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.cl.coordinator.PendingDeviceSelectionCoordinator
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.app.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.app.ui.vm.model.FileEntrySnapshot
import com.highcapable.adbrowser.app.ui.vm.model.PathBreadcrumbSegment
import com.highcapable.adbrowser.app.ui.vm.model.SelectionOption
import com.highcapable.adbrowser.core.adb.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.core.adb.model.AndroidDevice
import com.highcapable.adbrowser.core.adb.model.OperationResult
import com.highcapable.adbrowser.core.common.fs.FilePermission
import com.highcapable.adbrowser.core.domain.setting.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    /**
     * Dialogs are modeled as explicit states so the stage can render them declaratively.
     */
    sealed interface DialogState {
        data object None : DialogState
        data object DeviceConnect : DialogState
        data object DevicePair : DialogState
        data object NewFolder : DialogState
        data class Rename(val initialName: String) : DialogState
        data class DeleteConfirm(val entryCount: Int, val primaryEntryName: String?) : DialogState
        data class Properties(val snapshot: FileEntrySnapshot) : DialogState
    }

    /**
     * Status bar payloads are split between structured resource keys and raw backend messages.
     * Raw messages are kept as a fallback for unexpected backend failures that do not yet have
     * a dedicated i18n mapping.
     */
    sealed interface StatusMessage {
        data object None : StatusMessage
        data class Res(val key: Key, val args: List<String> = emptyList()) : StatusMessage
        data class Raw(val message: String) : StatusMessage

        enum class Key {
            CommonUnknownError,
            DevicesUpdated,
            DeviceConnectionPending,
            DeviceConnected,
            DeviceDisconnected,
            SelectDeviceFirst,
            InvalidFolderName,
            FolderCreated,
            SelectEntryFirst,
            InvalidName,
            Renamed,
            EntryDeleted,
            EntryDeletedMultiple,
            Copied,
            CopiedMultiple,
            Cut,
            CutMultiple,
            ClipboardEmpty,
            CrossDevicePasteNotSupported,
            Pasted,
            PastedMultiple,
            DialogPropertiesInvalidPermission,
            DialogPropertiesPermissionUpdated
        }
    }

    /**
     * Empty / failure hints that replace the file area content when entries cannot be shown.
     */
    enum class FileListHint {
        None,
        EmptyFolder,
        DeviceOffline,
        DeviceUnauthorized,
        DeviceNotFound,
        PathNotFound,
        PermissionDenied,
        LoadFailed
    }

    /**
     * Logical selection movement directions used by keyboard navigation.
     */
    enum class NavigationDirection {
        Up,
        Down,
        Left,
        Right
    }

    private data class ClipboardItemSnapshot(
        val name: String,
        val fullPath: String
    )

    private data class ClipboardEntrySnapshot(
        val device: AndroidDeviceItem,
        val items: List<ClipboardItemSnapshot>,
        val isCut: Boolean
    )

    private data class TargetPathResolution(
        val path: String,
        val restoredFromRememberedPath: Boolean
    )

    /**
     * Per-device UI state that survives device switching.
     *
     * The fields here intentionally mirror transient UI concerns such as selection, scroll
     * position, and breadcrumb state. This keeps `MainStage` mostly stateless and avoids having
     * to reconstruct the right pane from scratch whenever the active device changes.
     */
    class DeviceWorkspaceState internal constructor(device: AndroidDeviceItem) {
        var device by mutableStateOf(device)
        var currentPath by mutableStateOf("/")
        val pathInput = TextFieldState("/")
        val currentEntries = mutableStateListOf<DeviceFileItem>()
        val directorySnapshots = mutableMapOf<String, FileEntrySnapshot>()
        var hiddenEntryCount by mutableStateOf(0)
        var selectedEntry by mutableStateOf<DeviceFileItem?>(null)
        val selectedEntryPaths = mutableStateListOf<String>()
        var selectionAnchorPath by mutableStateOf<String?>(null)
        var suppressNextDoubleOpen by mutableStateOf(false)
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
    private val pendingDeviceSelectionCoordinator get() = appState.pendingDeviceSelectionCoordinator

    private var deviceObserverJob: Job? = null
    private var pendingDeviceSelectionObserverJob: Job? = null

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
            activeWorkspace?.let { state ->
                setSelection(
                    state = state,
                    selectedPaths = value?.let(::buildEntryFullPath)?.let(::setOf).orEmpty(),
                    primaryPath = value?.let(::buildEntryFullPath),
                    anchorPath = value?.let(::buildEntryFullPath)
                )
            }
        }

    val selectedEntries: List<DeviceFileItem>
        get() = activeWorkspace?.let(::selectedEntriesOfState) ?: emptyEntries

    val currentPath: String
        get() = activeWorkspace?.currentPath ?: "/"

    val pathInput: TextFieldState
        get() = activeWorkspace?.pathInput ?: fallbackPathInput

    val currentEntries: List<DeviceFileItem>
        get() = activeWorkspace?.currentEntries ?: emptyEntries

    val hiddenEntryCount: Int
        get() = activeWorkspace?.hiddenEntryCount ?: 0

    val isShowingHiddenFiles get() = settingsService.current.showHiddenFiles

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
    val canShowBlankFileContextMenu get() = selectedDevice?.let(::canShowBlankFileContextMenu) == true
    val canPasteEntry get() = selectedDevice?.let { clipboardEntry?.device == it } == true
    val canShowFileProperties
        get() = hasSingleSelectedEntry || (selectedDevice?.let(::canShowCurrentDirectoryProperties) == true)

    val hasSelectedEntry get() = selectedEntries.isNotEmpty()
    val hasSingleSelectedEntry get() = selectedEntries.size == 1
    val hasMultipleSelectedEntries get() = selectedEntries.size > 1
    val selectedEntryIsDirectory get() = hasSingleSelectedEntry && selectedEntry?.isDirectory == true

    var devicePaneWidthDp by mutableStateOf(settingsService.current.devicePaneWidth
        .toFloat()
        .coerceAtLeast(DEVICE_PANE_MIN_WIDTH))
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
        applyFileViewModePreference()
        normalizeFileColumnWidths()
    }

    /** Returns the cached workspace for a device, if it has been materialized already. */
    fun workspace(device: AndroidDeviceItem) = workspaces[device]

    /** Used by the UI to decide which prebuilt file pane should currently be visible. */
    fun isSelectedWorkspace(device: AndroidDeviceItem) = selectedDevice == device

    /** Network transports are the only device entries that can be disconnected explicitly. */
    fun canDisconnectDevice(device: AndroidDeviceItem) = device.type == AndroidDevice.Type.Network

    /** Updates the splitter width in memory; persistence is intentionally deferred until drag end. */
    fun setDevicePaneWidth(widthDp: Float) {
        devicePaneWidthDp = widthDp.coerceAtLeast(DEVICE_PANE_MIN_WIDTH)
    }

    /** Persists the current device pane width to settings after the user finishes dragging. */
    fun persistDevicePaneWidth() {
        settingsService.current.devicePaneWidth = devicePaneWidthDp.toDouble()
        saveSettingsAsync()
    }

    /**
     * Re-applies persisted presentation settings to the live stage.
     *
     * This is called after Preferences are saved or reset. Widths are normalized defensively
     * because settings can come from older versions or external edits.
     */
    fun onExternalSettingsChanged(refreshFileList: Boolean = false) {
        val settings = settingsService.current
        devicePaneWidthDp = settings.devicePaneWidth.toFloat().coerceAtLeast(DEVICE_PANE_MIN_WIDTH)
        fileColumnWidthNamePx = settings.fileColumnWidthName.toFloat()
        fileColumnWidthSizePx = settings.fileColumnWidthSize.toFloat()
        fileColumnWidthModifiedPx = settings.fileColumnWidthModified.toFloat()
        fileColumnWidthPermissionPx = settings.fileColumnWidthPermission.toFloat()
        normalizeFileColumnWidths()
        applyFileViewModePreference()
        if (refreshFileList)
            selectedDevice?.let { refreshEntriesAsync(device = it, requestedPath = currentPath) }
    }

    /** Resizes the Name column and returns the actually applied delta after clamping. */
    fun resizeNameAndSizeColumns(deltaDp: Float): Float {
        val old = fileColumnWidthNamePx
        fileColumnWidthNamePx = (old + deltaDp).coerceAtLeast(FILE_COLUMN_MIN_WIDTH_NAME)
        return fileColumnWidthNamePx - old
    }

    /** Resizes the Size column and returns the actually applied delta after clamping. */
    fun resizeSizeAndModifiedColumns(deltaDp: Float): Float {
        val old = fileColumnWidthSizePx
        fileColumnWidthSizePx = (old + deltaDp).coerceAtLeast(FILE_COLUMN_MIN_WIDTH_SIZE)
        return fileColumnWidthSizePx - old
    }

    /** Resizes the Modified column and returns the actually applied delta after clamping. */
    fun resizeModifiedAndPermissionColumns(deltaDp: Float): Float {
        val old = fileColumnWidthModifiedPx
        fileColumnWidthModifiedPx = (old + deltaDp).coerceAtLeast(FILE_COLUMN_MIN_WIDTH_MODIFIED)
        return fileColumnWidthModifiedPx - old
    }

    /** Persists current file header column widths after the resize gesture completes. */
    fun persistFileColumnWidths() {
        settingsService.current.fileColumnWidthName = fileColumnWidthNamePx.toDouble()
        settingsService.current.fileColumnWidthSize = fileColumnWidthSizePx.toDouble()
        settingsService.current.fileColumnWidthModified = fileColumnWidthModifiedPx.toDouble()
        settingsService.current.fileColumnWidthPermission = fileColumnWidthPermissionPx.toDouble()
        saveSettingsAsync()
    }

    /** Starts the initial load exactly once and wires live device observation afterwards. */
    fun initialize() {
        if (initialized) return

        initialized = true
        refreshDevices(showStatus = false)
        startObserveDevices()
        startObservePendingDeviceSelections()
    }

    /** Cancels observers and model coroutines when the stage is disposed. */
    fun dispose() {
        deviceObserverJob?.cancel()
        pendingDeviceSelectionObserverJob?.cancel()
        modelScope.cancel()
    }

    /**
     * Activates a device workspace.
     *
     * The first selection triggers an initial directory load for that device. Later switches reuse
     * the existing workspace so the right pane can come back with the previous scroll position and
     * selection intact.
     */
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

    /** Forces an immediate device refresh. The live observer uses the same result pipeline. */
    fun refreshDevices(showStatus: Boolean = true) = launchBusyAction {
        consumeDeviceListResult(
            result = adbClient.listDevices(),
            showStatus = showStatus
        )
    }

    /** Opens the device pair dialog from the device pane action menu. */
    fun pairNewDevice() {
        dialogState = DialogState.DevicePair
    }

    /** Opens the device connect dialog from the device pane action menu. */
    fun connectToDevice() {
        dialogState = DialogState.DeviceConnect
    }

    /** Disconnects a network ADB transport and refreshes devices immediately on success. */
    fun disconnectDevice(device: AndroidDeviceItem) = launchBusyAction {
        if (device.type != AndroidDevice.Type.Network) return@launchBusyAction

        val result = adbClient.disconnectDevice(device.toDomain())
        if (!result.isOk) {
            setErrorStatus(result.errorMessage)
            return@launchBusyAction
        }

        setStatus(StatusMessage.Key.DeviceDisconnected, device.brandModel)
        consumeDeviceListResult(
            result = adbClient.listDevices(),
            showStatus = false
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

    /**
     * Bridges app-scoped pending device announcements into this stage's status bar.
     *
     * The pending coordinator lives with [AppState], while this observer only exists for the
     * lifetime of the current main stage. This keeps the UI lifecycle-aware and avoids statically
     * pinning any Compose-facing state holder just to surface a transient status message.
     */
    private fun startObservePendingDeviceSelections() {
        if (pendingDeviceSelectionObserverJob?.isActive == true) return

        pendingDeviceSelectionObserverJob = modelScope.launch {
            pendingDeviceSelectionCoordinator.observeAnnouncements().collect { selection ->
                when (selection.reason) {
                    PendingDeviceSelectionCoordinator.Reason.Connected ->
                        setStatus(StatusMessage.Key.DeviceConnectionPending, selection.serial)
                }
            }
        }
    }

    private suspend fun consumeDeviceListResult(
        result: OperationResult<List<AndroidDevice>>,
        showStatus: Boolean
    ): AndroidDeviceItem? {
        if (!result.isOk) {
            setErrorStatus(result.errorMessage)
            return null
        }

        val previousSelectedDevice = selectedDevice

        devices.clear()
        devices += result.data.orEmpty().map { AndroidDeviceItem.from(it) }
        reconcileWorkspaces()

        val deviceSelection = pendingDeviceSelectionCoordinator.resolveDeviceSelection(
            devices = devices,
            previousSelectedDevice = previousSelectedDevice,
            selectDefaultDevice = ::selectDefaultDevice
        )
        val targetDevice = deviceSelection.targetDevice

        if (targetDevice == null) {
            selectedDevice = null
            if (devices.isEmpty()) statusMessage = StatusMessage.None
            return null
        }

        applyDeviceSelectionAfterRefresh(targetDevice)
        when {
            deviceSelection.pendingSelection != null && deviceSelection.pendingDevice != null ->
                applyPendingSelectionStatus(deviceSelection.pendingSelection, deviceSelection.pendingDevice)
            showStatus -> setStatus(StatusMessage.Key.DevicesUpdated)
        }

        return targetDevice
    }

    /** Reloads the current directory and clears selection, matching desktop file manager behavior. */
    fun refreshEntries() {
        val device = selectedDevice ?: return
        refreshEntriesAndClearSelection(device, requestedPath = currentPath)
    }

    /** Opens the "new folder" dialog when a device is available. */
    fun createNewFolder() {
        if (!ensureDeviceSelected()) return

        dialogState = DialogState.NewFolder
    }

    /** Creates a folder and refreshes the current directory if the backend operation succeeds. */
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
        selectedDevice?.let { refreshEntriesAndClearSelection(it, requestedPath = currentPath) }

        return true
    }

    /** Opens the rename dialog using the current single selection as the initial value. */
    fun renameSelectedEntry() {
        val entry = selectedEntry ?: return

        dialogState = DialogState.Rename(initialName = entry.name)
    }

    /** Renames the selected entry and refreshes the current directory on success. */
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
        selectedDevice?.let { refreshEntriesAndClearSelection(it, requestedPath = currentPath) }

        return true
    }

    /** Opens the delete confirmation dialog for the current selection. */
    fun deleteSelectedEntry() {
        val entries = selectedEntries
        if (entries.isEmpty()) return

        dialogState = DialogState.DeleteConfirm(
            entryCount = entries.size,
            primaryEntryName = selectedEntry?.name
        )
    }

    /**
     * Deletes the current selection asynchronously.
     *
     * Deletion is intentionally executed one entry at a time so partial failures can still
     * identify which item failed in the status message.
     */
    fun confirmDeleteSelectedEntry(): Boolean {
        val device = selectedDevice ?: return false
        val entries = selectedEntries
        if (entries.isEmpty()) return false

        val items = entries.map {
            ClipboardItemSnapshot(
                name = it.name,
                fullPath = buildEntryFullPath(it)
            )
        }

        launchBusyAction {
            items.forEach { item ->
                val result = fileSystemService.delete(device.toDomain(), item.fullPath)
                if (!result.isOk) {
                    setErrorStatus(result.errorMessage?.let { "${item.name}: $it" } ?: item.name)
                    return@launchBusyAction
                }
            }

            if (items.size == 1)
                setStatus(StatusMessage.Key.EntryDeleted, items.first().name)
            else setStatus(StatusMessage.Key.EntryDeletedMultiple, items.size.toString())
            refreshEntriesAndClearSelection(device, requestedPath = currentPath)
        }

        return true
    }

    /** Opens the properties dialog for the current single selection. */
    fun showSelectedEntryProperties() {
        val snapshot = buildSelectedEntrySnapshot() ?: return

        dialogState = DialogState.Properties(snapshot)
    }

    /** Opens the properties dialog for the current directory shown in the active file pane. */
    fun showCurrentDirectoryProperties() {
        val snapshot = buildCurrentDirectorySnapshot() ?: return

        dialogState = DialogState.Properties(snapshot)
    }

    /**
     * Opens properties for the current context.
     *
     * File-menu invocation prefers the selected entry and only falls back to the current directory
     * when that directory has a real entry to inspect. Root is intentionally excluded because it
     * does not have a parent entry we can resolve reliably from the backend today.
     */
    fun showProperties() {
        buildSelectedEntrySnapshot()
            ?.let { dialogState = DialogState.Properties(it) }
            ?: selectedDevice
                ?.takeIf(::canShowCurrentDirectoryProperties)
                ?.let { showCurrentDirectoryProperties() }
    }

    /** Opens the currently selected entry using the default open behavior. */
    fun openSelectedEntry() {
        val device = selectedDevice ?: return
        val entry = selectedEntry ?: return
        openEntry(device, entry)
    }

    /** Placeholder entry point for the future "Open With" workflow. */
    fun openSelectedEntryWith() {
        val device = selectedDevice ?: return
        val entry = selectedEntry ?: return
        openEntryWith(device, entry)
    }

    /** Closes whichever dialog is currently shown. */
    fun dismissDialog() {
        dialogState = DialogState.None
    }

    /** Copies the current selection into the in-memory clipboard snapshot. */
    fun copySelectedEntry() {
        val device = selectedDevice
        val entries = selectedEntries
        if (device == null || entries.isEmpty()) return

        clipboardEntry = ClipboardEntrySnapshot(
            device = device,
            items = entries.map {
                ClipboardItemSnapshot(
                    name = it.name,
                    fullPath = buildEntryFullPath(it)
                )
            },
            isCut = false
        )
        if (entries.size == 1)
            setStatus(StatusMessage.Key.Copied, entries.first().name)
        else setStatus(StatusMessage.Key.CopiedMultiple, entries.size.toString())
    }

    /** Cuts the current selection into the in-memory clipboard snapshot. */
    fun cutSelectedEntry() {
        val device = selectedDevice
        val entries = selectedEntries
        if (device == null || entries.isEmpty()) return

        clipboardEntry = ClipboardEntrySnapshot(
            device = device,
            items = entries.map {
                ClipboardItemSnapshot(
                    name = it.name,
                    fullPath = buildEntryFullPath(it)
                )
            },
            isCut = true
        )
        if (entries.size == 1)
            setStatus(StatusMessage.Key.Cut, entries.first().name)
        else setStatus(StatusMessage.Key.CutMultiple, entries.size.toString())
    }

    /**
     * Pastes the in-memory clipboard into the current path.
     *
     * Cross-device paste is intentionally blocked for now because backend copy/move semantics
     * are device-local and the UI should not pretend otherwise.
     */
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

        launchBusyAction {
            clipboard.items.forEach { item ->
                val targetPath = if (currentPath == "/")
                    "/${item.name}"
                else "${currentPath.trimEnd('/')}/${item.name}"

                val result = if (clipboard.isCut) {
                    fileSystemService.move(device.toDomain(), item.fullPath, targetPath)
                } else {
                    fileSystemService.copy(device.toDomain(), item.fullPath, targetPath)
                }

                if (!result.isOk) {
                    setErrorStatus(result.errorMessage?.let { "${item.name}: $it" } ?: item.name)
                    return@launchBusyAction
                }
            }

            if (clipboard.isCut) clipboardEntry = null
            if (clipboard.items.size == 1)
                setStatus(StatusMessage.Key.Pasted, clipboard.items.first().name)
            else setStatus(StatusMessage.Key.PastedMultiple, clipboard.items.size.toString())
            selectedDevice?.let { refreshEntriesAndClearSelection(it, requestedPath = currentPath) }
        }
    }

    /** Selects all visible entries in the active workspace. */
    fun selectAllEntries() {
        val state = activeWorkspace ?: return
        val paths = state.currentEntries.map(::buildEntryFullPath).toSet()
        val primaryPath = state.selectedEntry
            ?.let(::buildEntryFullPath)
            ?.takeIf(paths::contains)
            ?: state.currentEntries.firstOrNull()?.let(::buildEntryFullPath)

        setSelection(
            state = state,
            selectedPaths = paths,
            primaryPath = primaryPath,
            anchorPath = state.currentEntries.firstOrNull()?.let(::buildEntryFullPath)
        )
    }

    /** Inverts selection against the currently visible entry set. */
    fun inverseSelectEntries() {
        val state = activeWorkspace ?: return
        val allPaths = state.currentEntries.map(::buildEntryFullPath).toSet()
        val inverted = allPaths - state.selectedEntryPaths.toSet()
        val primaryPath = state.currentEntries.firstOrNull { buildEntryFullPath(it) in inverted }?.let(::buildEntryFullPath)

        setSelection(
            state = state,
            selectedPaths = inverted,
            primaryPath = primaryPath,
            anchorPath = primaryPath
        )
    }

    fun openViewModeMenu() {
        // Handled inline via Dropdown.
    }

    fun openSortModeMenu() {
        // Handled inline via Dropdown.
    }

    /** Applies a new file view mode and persists it if the preference is enabled. */
    fun onViewModeSelected(option: SelectionOption) {
        selectedViewMode = option
        if (settingsService.current.rememberLastFileViewMode) {
            settingsService.current.lastFileViewMode = if (option.key == "icons")
                AppSettings.FileViewMode.Grid
            else AppSettings.FileViewMode.List
            saveSettingsAsync()
        }
    }

    /** Applies a new sort mode to every cached workspace, not just the active one. */
    fun onSortModeSelected(option: SelectionOption) {
        selectedSortMode = option
        deviceWorkspaces.forEach { applySort(it) }
    }

    /** Convenience wrapper that navigates back in the active workspace, if any. */
    fun navigateBack() {
        selectedDevice?.let(::navigateBack)
    }

    /** Convenience wrapper that navigates forward in the active workspace, if any. */
    fun navigateForward() {
        selectedDevice?.let(::navigateForward)
    }

    /** Convenience wrapper that navigates to the parent directory in the active workspace. */
    fun navigateUp() {
        selectedDevice?.let(::navigateUp)
    }

    /** Navigates to the configured home path for the active device, falling back to `/`. */
    fun navigateHome() {
        val device = selectedDevice ?: return
        val remembered = deviceHomePaths(device)
        if (remembered.isNullOrBlank()) {
            navigateTo(device, "/")
            return
        }

        navigateTo(device, remembered)
    }

    /** Navigates to the configured home path for a specific device, falling back to `/`. */
    fun navigateHome(device: AndroidDeviceItem) {
        val remembered = deviceHomePaths(device)
        if (remembered.isNullOrBlank()) {
            navigateTo(device, "/")
            return
        }

        navigateTo(device, remembered)
    }

    /** Navigates the active workspace to the filesystem root. */
    fun navigateRoot() {
        selectedDevice?.let { navigateTo(it, "/") }
    }

    /** Toggles status bar visibility without touching persisted preferences. */
    fun toggleStatusBar() {
        isStatusBarVisible = !isStatusBarVisible
    }

    /** Resolves the active path input field and attempts to open the entered path. */
    fun openPathFromInput() {
        selectedDevice?.let(::openPathFromInput)
    }

    /** Navigates to a breadcrumb target in the active workspace. */
    fun navigateToBreadcrumb(fullPath: String) {
        selectedDevice?.let { navigateToBreadcrumb(it, fullPath) }
    }

    /** Opens an entry in the active workspace. */
    fun openEntry(entry: DeviceFileItem) {
        selectedDevice?.let { openEntry(it, entry) }
    }

    /** Navigates backward within one device-specific history stack. */
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

    /** Navigates forward within one device-specific history stack. */
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

    /** Navigates to the parent directory within one device-specific workspace. */
    fun navigateUp(device: AndroidDeviceItem) {
        val state = workspace(device) ?: return
        if (state.currentPath == "/") return

        val index = state.currentPath.lastIndexOf('/')
        val parent = if (index <= 0) "/" else state.currentPath.substring(0, index)
        navigateTo(device, parent)
    }

    /** Opens whatever path the device-specific path field currently contains. */
    fun openPathFromInput(device: AndroidDeviceItem) {
        val state = workspace(device) ?: return
        val path = normalizePath(state.pathInput.text.toString())
        navigateTo(device, path)
    }

    /** Navigates to a breadcrumb target for a specific device workspace. */
    fun navigateToBreadcrumb(device: AndroidDeviceItem, fullPath: String) {
        val state = workspace(device) ?: return
        val normalized = normalizePath(fullPath)
        if (normalized == state.currentPath) return

        navigateTo(device, normalized)
    }

    /** Opens a file entry inside a specific workspace. */
    fun openEntry(device: AndroidDeviceItem, entry: DeviceFileItem) {
        val state = workspace(device) ?: return
        if (!entry.isDirectory) return // TODO: Support opening files with associated applications in the future.

        val next = if (state.currentPath == "/") "/${entry.name}" else "${state.currentPath.trimEnd('/')}/${entry.name}"
        navigateTo(device, next)
    }

    fun openEntryWith(device: AndroidDeviceItem, entry: DeviceFileItem) {
        // TODO: Implement "Open With" functionality.
    }

    /** Replaces selection with a single entry or clears it when `entry` is null. */
    fun setSelectedEntry(device: AndroidDeviceItem, entry: DeviceFileItem?) {
        workspace(device)?.let { state ->
            setSelection(
                state = state,
                selectedPaths = entry?.let(::buildEntryFullPath)?.let(::setOf).orEmpty(),
                primaryPath = entry?.let(::buildEntryFullPath),
                anchorPath = entry?.let(::buildEntryFullPath)
            )
        }
    }

    fun selectedEntryOf(device: AndroidDeviceItem) = workspace(device)?.selectedEntry
    fun selectedEntriesOf(device: AndroidDeviceItem) = workspace(device)?.let(::selectedEntriesOfState) ?: emptyEntries
    fun selectedEntryPathsOf(device: AndroidDeviceItem) = workspace(device)?.selectedEntryPaths?.toSet().orEmpty()

    fun hasMultipleSelectedEntries(device: AndroidDeviceItem) = selectedEntriesOf(device).size > 1
    fun isEntrySelected(device: AndroidDeviceItem, entry: DeviceFileItem): Boolean {
        val path = buildEntryFullPath(entry)
        return workspace(device)?.selectedEntryPaths?.contains(path) == true
    }

    fun entriesOf(device: AndroidDeviceItem) = workspace(device)?.currentEntries ?: emptyEntries
    fun directoryChangeVersionOf(device: AndroidDeviceItem) = workspace(device)?.directoryChangeVersion ?: 0
    fun listScrollIndexOf(device: AndroidDeviceItem) = workspace(device)?.listScrollIndex ?: 0
    fun listScrollOffsetOf(device: AndroidDeviceItem) = workspace(device)?.listScrollOffset ?: 0
    fun listHorizontalScrollOffsetOf(device: AndroidDeviceItem) = workspace(device)?.listHorizontalScrollOffset ?: 0
    fun iconScrollRowIndexOf(device: AndroidDeviceItem) = workspace(device)?.iconScrollRowIndex ?: 0
    fun iconScrollRowOffsetOf(device: AndroidDeviceItem) = workspace(device)?.iconScrollRowOffset ?: 0

    /** Persists vertical list scroll position so switching devices or panes can restore it. */
    fun updateListScrollState(device: AndroidDeviceItem, index: Int, offset: Int) {
        workspace(device)?.let {
            it.listScrollIndex = index.coerceAtLeast(0)
            it.listScrollOffset = offset.coerceAtLeast(0)
        }
    }

    /** Persists horizontal header/content scroll position for list view. */
    fun updateListHorizontalScrollState(device: AndroidDeviceItem, offset: Int) {
        workspace(device)?.listHorizontalScrollOffset = offset.coerceAtLeast(0)
    }

    /** Persists grid scroll position so icon view can be restored per device. */
    fun updateIconScrollState(device: AndroidDeviceItem, rowIndex: Int, rowOffset: Int) {
        workspace(device)?.let {
            it.iconScrollRowIndex = rowIndex.coerceAtLeast(0)
            it.iconScrollRowOffset = rowOffset.coerceAtLeast(0)
        }
    }

    fun pathInputOf(device: AndroidDeviceItem) = workspace(device)?.pathInput ?: fallbackPathInput
    fun breadcrumbsOf(device: AndroidDeviceItem) = workspace(device)?.pathBreadcrumbSegments ?: emptyBreadcrumbSegments
    fun fileListHintOf(device: AndroidDeviceItem) = workspace(device)?.fileListHint ?: FileListHint.None

    fun canShowBlankFileContextMenu(device: AndroidDeviceItem) = fileListHintOf(device).let {
        it == FileListHint.None || it == FileListHint.EmptyFolder
    }
    fun canShowCurrentDirectoryProperties(device: AndroidDeviceItem) =
        device.isOnline && normalizePath(workspace(device)?.currentPath ?: "/") != "/"
    fun canNavigateBack(device: AndroidDeviceItem) = (workspace(device)?.navigationIndex ?: 0) > 0
    fun canNavigateForward(device: AndroidDeviceItem) =
        workspace(device)?.let { it.navigationIndex < it.navigationHistory.size - 1 } == true
    fun canNavigateUp(device: AndroidDeviceItem) = workspace(device)?.currentPath?.let { it != "/" } ?: false

    /** Clears selection and resets the double-open suppression flag for one workspace. */
    fun clearSelectedEntries(device: AndroidDeviceItem) {
        workspace(device)?.let {
            it.suppressNextDoubleOpen = false
            setSelection(it, emptySet())
        }
    }

    /**
     * Moves selection with arrow-key semantics.
     *
     * When multiple entries are selected, the first key press collapses the selection to a single
     * edge item in the requested direction. This mirrors common desktop file manager behavior and
     * prevents an arrow key from unexpectedly navigating/opening the wrong entry.
     */
    fun navigateSelection(
        device: AndroidDeviceItem,
        direction: NavigationDirection,
        gridColumnCount: Int = 1
    ): Int? {
        val state = workspace(device) ?: return null
        if (state.currentEntries.isEmpty()) return null

        val columns = gridColumnCount.coerceAtLeast(1)
        val selectedIndices = state.currentEntries
            .mapIndexedNotNull { index, entry ->
                index.takeIf { buildEntryFullPath(entry) in state.selectedEntryPaths }
            }

        if (selectedIndices.isEmpty()) {
            val targetIndex = when (direction) {
                NavigationDirection.Up,
                NavigationDirection.Left -> state.currentEntries.lastIndex
                NavigationDirection.Down,
                NavigationDirection.Right -> 0
            }
            selectSingleEntryAtIndex(state, targetIndex)
            return targetIndex
        }

        if (selectedIndices.size > 1) {
            val targetIndex = resolveSelectionCollapseIndex(selectedIndices, direction, columns)
            selectSingleEntryAtIndex(state, targetIndex)
            return targetIndex
        }

        val currentIndex = selectedIndices.first()
        val targetIndex = resolveDirectionalTargetIndex(
            currentIndex = currentIndex,
            direction = direction,
            itemCount = state.currentEntries.size,
            gridColumnCount = columns
        ) ?: currentIndex

        selectSingleEntryAtIndex(state, targetIndex)
        return targetIndex
    }

    /**
     * Applies click selection semantics for list/icon entries.
     *
     * The double-open suppression flag exists because a single click is delivered before a double
     * click. When multi-selection collapses to a single item on the first click, the follow-up
     * double click would otherwise immediately open the entry.
     */
    fun selectEntryByGesture(
        device: AndroidDeviceItem,
        entry: DeviceFileItem,
        appendSelection: Boolean,
        rangeSelection: Boolean
    ) {
        val state = workspace(device) ?: return
        val entryPath = buildEntryFullPath(entry)

        when {
            rangeSelection -> {
                state.suppressNextDoubleOpen = false
                selectRangeToEntry(state, entry, additive = appendSelection)
            }
            appendSelection -> {
                state.suppressNextDoubleOpen = false
                toggleEntrySelection(state, entry)
            }
            else -> {
                // If the entry is already the only selected entry,
                // do not clear and reselect it to avoid suppressing double open.
                if (state.selectedEntryPaths.size == 1 &&
                    state.selectedEntryPaths.firstOrNull() == entryPath &&
                    state.selectedEntry?.let(::buildEntryFullPath) == entryPath
                ) {
                    state.suppressNextDoubleOpen = false
                    return
                }
                state.suppressNextDoubleOpen = state.selectedEntryPaths.size > 1

                setSelection(
                    state = state,
                    selectedPaths = setOf(entryPath),
                    primaryPath = entryPath,
                    anchorPath = entryPath
                )
            }
        }
    }

    /**
     * Ensures the right-click target is selected before the entry menu opens.
     *
     * If the clicked entry already belongs to an existing multi-selection, that multi-selection is
     * preserved. This matches desktop file managers where right-click should not unexpectedly throw
     * away the current multi-selection.
     */
    fun ensureEntrySelectedForContextMenu(device: AndroidDeviceItem, entry: DeviceFileItem) {
        val state = workspace(device) ?: return

        val entryPath = buildEntryFullPath(entry)
        if (state.selectedEntryPaths.size > 1 && entryPath in state.selectedEntryPaths) {
            state.suppressNextDoubleOpen = false
            state.selectedEntry = state.currentEntries.firstOrNull { buildEntryFullPath(it) == entryPath }
            if (state.selectionAnchorPath == null) state.selectionAnchorPath = entryPath
            return
        }

        state.suppressNextDoubleOpen = false
        setSelection(
            state = state,
            selectedPaths = setOf(entryPath),
            primaryPath = entryPath,
            anchorPath = entryPath
        )
    }

    /** Consumes the transient guard that prevents an unintended open after selection collapse. */
    fun consumeDoubleOpenSuppression(device: AndroidDeviceItem, entry: DeviceFileItem): Boolean {
        val state = workspace(device) ?: return false
        val entryPath = buildEntryFullPath(entry)

        val shouldSuppress = state.suppressNextDoubleOpen &&
            state.selectedEntryPaths.size == 1 &&
            state.selectedEntryPaths.firstOrNull() == entryPath

        state.suppressNextDoubleOpen = false
        return shouldSuppress
    }

    /**
     * Updates drag / marquee selection using only entries currently known to the workspace.
     *
     * Candidate paths are filtered against the live entry list because drag gestures can outlive a
     * directory refresh, and we must not keep references to entries that no longer exist.
     */
    fun updateDragSelection(
        device: AndroidDeviceItem,
        candidatePaths: Set<String>,
        additive: Boolean,
        initialSelectionPaths: Set<String>
    ) {
        val state = workspace(device) ?: return
        state.suppressNextDoubleOpen = false

        val validCandidatePaths = candidatePaths.filterTo(linkedSetOf()) { path ->
            state.currentEntries.any { buildEntryFullPath(it) == path }
        }
        val finalSelection = if (additive)
            (initialSelectionPaths - validCandidatePaths) + (validCandidatePaths - initialSelectionPaths)
        else validCandidatePaths
        val primaryPath = state.currentEntries.firstOrNull {
            buildEntryFullPath(it) in validCandidatePaths
        }?.let(::buildEntryFullPath) ?: state.currentEntries.firstOrNull {
            buildEntryFullPath(it) in finalSelection
        }?.let(::buildEntryFullPath)

        setSelection(
            state = state,
            selectedPaths = finalSelection,
            primaryPath = primaryPath,
            anchorPath = state.selectionAnchorPath ?: primaryPath
        )
    }

    /** Loads permission details for the properties dialog. */
    fun loadPermission(snapshot: FileEntrySnapshot) = runBlocking {
        permissionService.getPermission(snapshot.device, snapshot.fullPath)
    }

    /**
     * Applies a chmod-style permission change and re-reads the resulting state from backend.
     *
     * The extra read is intentional because backend normalization may change the returned symbolic
     * value, and the dialog should reflect the real final permission instead of a locally inferred one.
     */
    fun applyPermission(
        snapshot: FileEntrySnapshot,
        modeText: String,
        reportStatus: Boolean = true
    ): OperationResult<FilePermission.Info> {
        val mode = FilePermission.parseMode(modeText)
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

            // History must be pushed after refresh succeeds because `refreshEntriesInternal`
            // updates `state.currentPath`. Pushing too early would record the old path again.
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

    private fun refreshEntriesAndClearSelection(
        device: AndroidDeviceItem,
        requestedPath: String? = null,
        useRememberedPathWhenRequestedPathIsNull: Boolean = false,
        onCompleted: (Boolean) -> Unit = {}
    ) {
        clearSelectedEntries(device)
        refreshEntriesAsync(
            device = device,
            requestedPath = requestedPath,
            useRememberedPathWhenRequestedPathIsNull = useRememberedPathWhenRequestedPathIsNull,
            onCompleted = onCompleted
        )
    }

    private suspend fun refreshEntriesInternal(
        state: DeviceWorkspaceState,
        device: AndroidDevice,
        requestedPath: String? = null,
        useRememberedPathWhenRequestedPathIsNull: Boolean = false
    ): Boolean {
        val targetPath = resolveTargetPath(state, device, requestedPath, useRememberedPathWhenRequestedPathIsNull)
        val previousPath = state.currentPath
        val result = fileSystemService.list(device, targetPath.path)

        // Apply the requested path immediately so the address bar and breadcrumb stay in sync
        // with the navigation intent even if the backend call fails, and we end up showing a hint.
        applyPath(state, targetPath.path)

        if (result.isOk) {
            fillEntries(state, result.data.orEmpty())
            if (targetPath.restoredFromRememberedPath)
                prewarmCurrentDirectorySnapshotFromParent(state, device, targetPath.path)
            state.fileListHint = if (state.currentEntries.isEmpty()) FileListHint.EmptyFolder else FileListHint.None
            if (state.currentPath != previousPath) {
                // This version counter is consumed by the UI to reset scroll position only when
                // the directory actually changes, not when the same directory is reloaded.
                state.directoryChangeVersion += 1
                statusMessage = StatusMessage.None
            }
            persistCurrentPath(state, device)
        } else {
            fillEntries(state, emptyList())
            state.fileListHint = resolveFailureHint(result.errorMessage)
            if (state.fileListHint.shouldSuppressStatusBarError())
                statusMessage = StatusMessage.None
            else setErrorStatus(result.errorMessage)
        }

        return true
    }

    private fun resolveTargetPath(
        state: DeviceWorkspaceState,
        device: AndroidDevice,
        requestedPath: String?,
        useRememberedPathWhenRequestedPathIsNull: Boolean
    ): TargetPathResolution {
        if (!requestedPath.isNullOrBlank())
            return TargetPathResolution(
                path = normalizePath(requestedPath),
                restoredFromRememberedPath = false
            )

        if (useRememberedPathWhenRequestedPathIsNull &&
            settingsService.current.rememberLastDevicePath
        ) deviceLastPaths(device)?.let {
            return TargetPathResolution(
                path = normalizePath(it),
                restoredFromRememberedPath = true
            )
        }

        return TargetPathResolution(
            path = normalizePath(state.currentPath),
            restoredFromRememberedPath = false
        )
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

    /**
     * Builds a folder snapshot for the currently visible directory.
     *
     * Most calls should hit the per-workspace cache that is primed while browsing parent folders.
     * A backend fallback is kept only for cold-start paths (for example, a remembered path opened
     * before its parent has been listed in this session).
     */
    private fun buildCurrentDirectorySnapshot(): FileEntrySnapshot? {
        val state = activeWorkspace ?: return null
        val device = selectedDevice?.toDomain() ?: return null
        val fullPath = currentPath
        if (fullPath == "/") return null

        state.directorySnapshots[fullPath]?.let { return it }

        val snapshot = findCurrentDirectoryEntry(device, fullPath)?.let {
            snapshotOf(device, it)
        } ?: return null
        state.directorySnapshots[fullPath] = snapshot
        return snapshot
    }

    private fun findCurrentDirectoryEntry(device: AndroidDevice, fullPath: String): DeviceFileEntry? {
        if (fullPath == "/") return null

        val parentPath = fullPath.substringBeforeLast('/', "/").ifBlank { "/" }
        val result = runBlocking { fileSystemService.list(device, parentPath) }
        if (!result.isOk) return null

        return result.data.orEmpty().firstOrNull { entry ->
            normalizePath(buildEntryFullPath(entry)) == fullPath
        }
    }

    private fun updateEntryPermission(fullPath: String, symbolicPermission: String) {
        if (fullPath.isBlank() || symbolicPermission.isBlank()) return

        val state = activeWorkspace ?: return
        val index = state.currentEntries.indexOfFirst { buildEntryFullPath(it) == fullPath }
        if (index < 0) return

        val old = state.currentEntries[index]
        state.currentEntries[index] = old.copy(permission = symbolicPermission)
        updateCachedDirectoryPermission(state, fullPath, symbolicPermission)

        if (state.selectedEntry?.let(::buildEntryFullPath) == fullPath)
            state.selectedEntry = state.currentEntries[index]
    }

    private fun fillEntries(state: DeviceWorkspaceState, entries: List<DeviceFileEntry>) {
        val showHidden = settingsService.current.showHiddenFiles
        val selectedPaths = state.selectedEntryPaths.toSet()
        val primaryPath = state.selectedEntry?.let(::buildEntryFullPath)
        val anchorPath = state.selectionAnchorPath
        state.hiddenEntryCount = if (showHidden) 0 else entries.count { isHiddenEntryName(it.name) }

        state.currentEntries.clear()
        state.currentEntries += entries
            .asSequence()
            .filter { showHidden || !isHiddenEntryName(it.name) }
            .map { DeviceFileItem.from(it) }
            .toList()

        cacheDirectorySnapshots(state, entries)

        applySort(state)

        // Reconcile selection after every reload because sorting and hidden-file filtering can
        // invalidate previously selected paths or reorder which entry should be primary.
        setSelection(
            state = state,
            selectedPaths = selectedPaths,
            primaryPath = primaryPath,
            anchorPath = anchorPath
        )
    }

    /**
     * Directory properties are opened from the active path rather than from a selected child item,
     * so we cache directory snapshots while listing their parent folder. That removes an extra
     * backend `list(parent)` round-trip every time the user opens "Properties" on the current path.
     */
    private fun cacheDirectorySnapshots(state: DeviceWorkspaceState, entries: List<DeviceFileEntry>) {
        val device = state.device.toDomain()
        entries.asSequence()
            .filter { it.isDirectory }
            .forEach { entry ->
                val snapshot = snapshotOf(device, entry)
                state.directorySnapshots[snapshot.fullPath] = snapshot
            }
    }

    /**
     * Remembered-path restoration can land directly inside a deep folder before its parent has ever
     * been opened in this session. We prewarm just that one current-directory snapshot so opening
     * "Properties" right after startup does not need to synchronously fetch the parent again.
     */
    private suspend fun prewarmCurrentDirectorySnapshotFromParent(
        state: DeviceWorkspaceState,
        device: AndroidDevice,
        fullPath: String
    ) {
        if (fullPath == "/" || state.directorySnapshots.containsKey(fullPath)) return

        val parentPath = fullPath.substringBeforeLast('/', "/").ifBlank { "/" }
        val result = fileSystemService.list(device, parentPath)
        if (!result.isOk) return

        result.data.orEmpty()
            .firstOrNull { entry -> normalizePath(buildEntryFullPath(entry)) == fullPath }
            ?.let { state.directorySnapshots[fullPath] = snapshotOf(device, it) }
    }

    private fun updateCachedDirectoryPermission(
        state: DeviceWorkspaceState,
        fullPath: String,
        symbolicPermission: String
    ) {
        val cached = state.directorySnapshots[fullPath] ?: return
        state.directorySnapshots[fullPath] = cached.copy(symbolicPermission = symbolicPermission)
    }

    private fun applySort(state: DeviceWorkspaceState) {
        val sortMode = selectedSortMode?.key ?: "name"
        val foldersFirst = settingsService.current.foldersFirst

        // Sorting is applied on the already-filtered UI entry list, not directly on backend data.
        // That keeps selection reconciliation and view restoration operating on the same ordering
        // the user actually sees on screen.
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

    private fun selectedEntriesOfState(state: DeviceWorkspaceState): List<DeviceFileItem> {
        if (state.selectedEntryPaths.isEmpty()) return emptyList()
        val selectedPaths = state.selectedEntryPaths.toSet()

        return state.currentEntries.filter { buildEntryFullPath(it) in selectedPaths }
    }

    private fun setSelection(
        state: DeviceWorkspaceState,
        selectedPaths: Set<String>,
        primaryPath: String? = null,
        anchorPath: String? = null
    ) {
        // Only keep paths that still exist in the live entry list. Selection can outlive refreshes,
        // sorting changes, hidden-file toggles, and drag-selection updates.
        val validPaths = state.currentEntries
            .map(::buildEntryFullPath)
            .filterTo(linkedSetOf()) { it in selectedPaths }

        state.selectedEntryPaths.clear()
        state.selectedEntryPaths += validPaths

        val resolvedPrimaryPath = primaryPath
            ?.takeIf { it in validPaths }
            ?: validPaths.firstOrNull()
        state.selectedEntry = resolvedPrimaryPath?.let { path ->
            state.currentEntries.firstOrNull { buildEntryFullPath(it) == path }
        }

        state.selectionAnchorPath = anchorPath
            ?.takeIf { it in validPaths }
            ?: resolvedPrimaryPath
    }

    private fun selectSingleEntryAtIndex(state: DeviceWorkspaceState, index: Int) {
        val entry = state.currentEntries.getOrNull(index) ?: return
        val entryPath = buildEntryFullPath(entry)

        setSelection(
            state = state,
            selectedPaths = setOf(entryPath),
            primaryPath = entryPath,
            anchorPath = entryPath
        )
    }

    private fun resolveSelectionCollapseIndex(
        selectedIndices: List<Int>,
        direction: NavigationDirection,
        gridColumnCount: Int
    ): Int {
        fun row(index: Int) = index / gridColumnCount
        fun column(index: Int) = index % gridColumnCount

        // When a multi-selection receives an arrow key, collapse toward the visual edge in that
        // direction instead of arbitrarily keeping the primary selection.
        return when (direction) {
            NavigationDirection.Up ->
                selectedIndices.minWithOrNull(compareBy({ row(it) }, { column(it) }, { it }))
            NavigationDirection.Down ->
                selectedIndices.maxWithOrNull(compareBy({ row(it) }, { column(it) }, { it }))
            NavigationDirection.Left ->
                selectedIndices.minWithOrNull(compareBy({ column(it) }, { row(it) }, { it }))
            NavigationDirection.Right ->
                selectedIndices.maxWithOrNull(compareBy({ column(it) }, { row(it) }, { it }))
        } ?: selectedIndices.first()
    }

    private fun resolveDirectionalTargetIndex(
        currentIndex: Int,
        direction: NavigationDirection,
        itemCount: Int,
        gridColumnCount: Int
    ): Int? {
        val lastIndex = itemCount - 1

        return when (direction) {
            NavigationDirection.Up -> (currentIndex - gridColumnCount).takeIf { it >= 0 }
            NavigationDirection.Down -> (currentIndex + gridColumnCount).takeIf { it <= lastIndex }
            NavigationDirection.Left -> (currentIndex - 1).takeIf { it >= 0 }
            NavigationDirection.Right -> (currentIndex + 1).takeIf { it <= lastIndex }
        }
    }

    private fun toggleEntrySelection(state: DeviceWorkspaceState, entry: DeviceFileItem) {
        val entryPath = buildEntryFullPath(entry)
        val current = state.selectedEntryPaths.toMutableSet()
        if (!current.add(entryPath)) current.remove(entryPath)

        val primaryPath = when {
            entryPath in current -> entryPath
            state.selectedEntry?.let(::buildEntryFullPath) in current -> state.selectedEntry?.let(::buildEntryFullPath)
            else -> state.currentEntries.firstOrNull { buildEntryFullPath(it) in current }?.let(::buildEntryFullPath)
        }
        val anchorPath = when {
            entryPath in current -> entryPath
            state.selectionAnchorPath in current -> state.selectionAnchorPath
            else -> primaryPath
        }

        setSelection(
            state = state,
            selectedPaths = current,
            primaryPath = primaryPath,
            anchorPath = anchorPath
        )
    }

    private fun selectRangeToEntry(
        state: DeviceWorkspaceState,
        entry: DeviceFileItem,
        additive: Boolean
    ) {
        val targetIndex = state.currentEntries.indexOfFirst { buildEntryFullPath(it) == buildEntryFullPath(entry) }
        if (targetIndex < 0) return

        val anchorPath = state.selectionAnchorPath
            ?.takeIf { path -> state.currentEntries.any { buildEntryFullPath(it) == path } }
            ?: state.selectedEntry?.let(::buildEntryFullPath)
            ?: buildEntryFullPath(entry)
        val anchorIndex = state.currentEntries.indexOfFirst { buildEntryFullPath(it) == anchorPath }.coerceAtLeast(0)

        // Shift-selection works on the current visual order after sorting/filtering, which matches
        // user expectations better than trying to preserve an older backend order.
        val range = state.currentEntries
            .subList(minOf(anchorIndex, targetIndex), maxOf(anchorIndex, targetIndex) + 1)
            .mapTo(linkedSetOf(), ::buildEntryFullPath)
        val selectedPaths = if (additive) state.selectedEntryPaths.toSet() + range else range

        setSelection(
            state = state,
            selectedPaths = selectedPaths,
            primaryPath = buildEntryFullPath(entry),
            anchorPath = anchorPath
        )
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
                    // Background prebuild is best-effort only. Failures should not steal focus from
                    // the active device or overwrite the status bar with noise the user did not ask for.
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

    /**
     * Applies a refreshed device selection and eagerly prepares the visible workspace.
     *
     * Refresh-driven selection is different from manual clicking: the stage already decided which
     * device should be active, so this helper only focuses on synchronizing the right pane. The
     * chosen workspace is hydrated immediately because it is visible right away, while the other
     * workspaces keep their cheaper best-effort prebuild path in the background.
     */
    private suspend fun applyDeviceSelectionAfterRefresh(targetDevice: AndroidDeviceItem) {
        selectedDevice = targetDevice

        val selectedState = ensureWorkspace(targetDevice)
        if (!selectedState.prebuilt && 
            refreshEntriesInternal(selectedState, targetDevice.toDomain(), useRememberedPathWhenRequestedPathIsNull = true)
        ) {
            setHistoryToCurrentPath(selectedState)
            selectedState.prebuilt = true
        }

        prebuildWorkspacesInBackground(skipDevice = targetDevice)
    }

    private fun selectDefaultDevice() = if (settingsService.current.rememberLastDevice)
        rememberedSelectedDevice() ?: devices.firstOrNull()
    else devices.firstOrNull()

    private fun applyFileViewModePreference() {
        val target = if (settingsService.current.rememberLastFileViewMode)
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
                // Busy state is reference-counted because multiple async operations can overlap
                // (for example, a device observer refresh while a file operation is still running).
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

        // Match browser/file-manager history behavior: navigating after going back drops the
        // forward branch instead of keeping a tree of alternate futures.
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
        if (!settingsService.current.rememberLastDevicePath) return

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

        // Breadcrumbs are rebuilt from the normalized path instead of incremental mutation so the
        // UI never accumulates stale segments after direct path edits or failed navigations.
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

    private fun snapshotOf(device: AndroidDevice, entry: DeviceFileEntry) = FileEntrySnapshot(
        device = device,
        name = entry.name,
        fullPath = normalizePath(buildEntryFullPath(entry)),
        isDirectory = entry.isDirectory,
        isSymlink = entry.isSymlink,
        sizeBytes = entry.size,
        modifiedAt = entry.modifiedAt,
        symbolicPermission = entry.permission
    )

    private fun buildEntryFullPath(entry: DeviceFileItem) = if (entry.path == "/")
        "/${entry.name}"
    else "${entry.path.trimEnd('/')}/${entry.name}"

    private fun buildEntryFullPath(entry: DeviceFileEntry) = if (entry.path == "/")
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

        // Backend error messages are not fully normalized yet, so this intentionally relies on
        // tolerant substring checks instead of exact string matching.
        return when {
            "device unauthorized" in message -> FileListHint.DeviceUnauthorized
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

    /**
     * Some transport-level states are already represented by the dedicated empty-state panel in the
     * file area. Repeating them in the status bar only adds noise, so those hints intentionally
     * suppress the raw backend error text there.
     */
    private fun FileListHint.shouldSuppressStatusBarError() = when (this) {
        FileListHint.DeviceOffline,
        FileListHint.DeviceUnauthorized -> true
        else -> false
    }

    private fun ensureDeviceSelected(): Boolean {
        if (selectedDevice != null) return true

        setStatus(StatusMessage.Key.SelectDeviceFirst)
        return false
    }

    private fun isHiddenEntryName(name: String) = name.isNotBlank() && name.startsWith('.')

    private fun setStatus(key: StatusMessage.Key, vararg args: String) {
        statusMessage = StatusMessage.Res(key = key, args = args.toList())
    }

    private fun setErrorStatus(message: String?) {
        statusMessage = message
            ?.takeIf { it.isNotBlank() }
            ?.let { StatusMessage.Raw(it) }
            ?: StatusMessage.Res(StatusMessage.Key.CommonUnknownError)
    }

    private fun applyPendingSelectionStatus(
        selection: PendingDeviceSelectionCoordinator.PendingSelection,
        device: AndroidDeviceItem
    ) = when (selection.reason) {
        PendingDeviceSelectionCoordinator.Reason.Connected -> setStatus(StatusMessage.Key.DeviceConnected, device.brandModel)
    }

    private fun String?.orUnknownErrorToken() = this?.takeIf { it.isNotBlank() } ?: UNKNOWN_ERROR_TOKEN
}