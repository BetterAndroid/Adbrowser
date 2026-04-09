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
@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH", "AssignedValueIsNeverRead")

package com.highcapable.adbrowser.frontend.ui.stage

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import androidx.compose.ui.zIndex
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.frontend.ui.assets.AppIcons
import com.highcapable.adbrowser.frontend.ui.component.DevicePanePanel
import com.highcapable.adbrowser.frontend.ui.component.FileIconItem
import com.highcapable.adbrowser.frontend.ui.component.FileListHeader
import com.highcapable.adbrowser.frontend.ui.component.FileListHint
import com.highcapable.adbrowser.frontend.ui.component.FileListRow
import com.highcapable.adbrowser.frontend.ui.component.FileNavigationBar
import com.highcapable.adbrowser.frontend.ui.component.FixedWidthHorizontalSplitLayout
import com.highcapable.adbrowser.frontend.ui.component.PanelSurface
import com.highcapable.adbrowser.frontend.ui.component.PathBreadcrumbBar
import com.highcapable.adbrowser.frontend.ui.component.StatusBar
import com.highcapable.adbrowser.frontend.ui.dialog.ConfirmDialog
import com.highcapable.adbrowser.frontend.ui.dialog.FilePropertiesDialog
import com.highcapable.adbrowser.frontend.ui.dialog.SimpleInputDialog
import com.highcapable.adbrowser.frontend.ui.foundation.isIndexVisible
import com.highcapable.adbrowser.frontend.ui.interaction.onBlankPrimaryPress
import com.highcapable.adbrowser.frontend.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.utils.extension.formatWithArgs
import com.highcapable.adbrowser.frontend.ui.vm.MainStageModel
import com.highcapable.adbrowser.frontend.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.shared.utils.BuildVersion
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.CopyMenuItemOptionAction
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.CutMenuItemOptionAction
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.PasteMenuItemOptionAction
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.SelectAllMenuItemOptionAction
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun FrameWindowScope.MainStage(
    viewModel: MainStageModel,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    RenderContent(
        viewModel = viewModel,
        modifier = modifier
    )
    RenderDialogs(viewModel)
}

@Composable
private fun RenderContent(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
    ) {
        FixedWidthHorizontalSplitLayout(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp),
            firstPaneWidth = viewModel.devicePaneWidthDp.dp,
            onFirstPaneWidthChange = { viewModel.setDevicePaneWidth(it.value) },
            onFirstPaneWidthChangeFinished = viewModel::persistDevicePaneWidth,
            firstPaneMinWidth = 240.dp,
            secondPaneMinWidth = 560.dp,
            dividerWidth = 10.dp,
            first = {
                DevicePane(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            },
            second = {
                FilePaneHost(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        if (viewModel.isStatusBarVisible) {
            StatusBar(
                text = StatusMessageText(viewModel.statusMessage).ifBlank { strings.mainStatusReady },
                versionText = BuildVersion.TEXT
            )
        }
    }
}

@Composable
private fun DevicePane(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    DevicePanePanel(
        devices = viewModel.devices,
        selectedDevice = viewModel.selectedDevice,
        listState = listState,
        title = strings.mainDevicesTitle,
        refreshDescription = strings.mainRefreshDeviceDescription,
        noDeviceMessage = strings.mainDeviceListHintNoDevice,
        onRefresh = viewModel::refreshDevices,
        onDeviceClick = viewModel::selectDevice,
        modifier = modifier
    )
}

@Composable
private fun FilePaneHost(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        viewModel.deviceWorkspaces.forEach { workspace ->
            key(workspace.device) {
                val isSelected = workspace.device == viewModel.selectedDevice
                FilePane(
                    viewModel = viewModel,
                    workspace = workspace,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isSelected) 1f else 0f)
                        .alpha(if (isSelected) 1f else 0f)
                )
            }
        }
    }
}

@Composable
private fun FilePane(
    viewModel: MainStageModel,
    workspace: MainStageModel.DeviceWorkspaceState,
    modifier: Modifier = Modifier
) {
    PanelSurface(modifier = modifier, padding = PaddingValues(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavigationBar(viewModel = viewModel, device = workspace.device)
            Spacer(Modifier.height(12.dp))
            FilePaneContent(viewModel = viewModel, workspace = workspace, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FilePaneContent(
    viewModel: MainStageModel,
    workspace: MainStageModel.DeviceWorkspaceState,
    modifier: Modifier = Modifier
) {
    PanelSurface(modifier = modifier.fillMaxWidth(), clipContent = true) {
        Column(modifier = Modifier.fillMaxSize()) {
            FileListArea(
                viewModel = viewModel,
                device = workspace.device,
                modifier = Modifier.weight(1f)
            )
            PathBreadcrumbBar(
                segments = viewModel.breadcrumbsOf(workspace.device),
                scrollState = rememberScrollState(),
                onRootClick = { viewModel.navigateToBreadcrumb(workspace.device, "/") },
                onSegmentClick = { segment ->
                    viewModel.navigateToBreadcrumb(workspace.device, segment.fullPath)
                }
            )
        }
    }
}

@Composable
private fun NavigationBar(viewModel: MainStageModel, device: AndroidDeviceItem) {
    FileNavigationBar(
        pathInput = viewModel.pathInputOf(device),
        canNavigateBack = viewModel.canNavigateBack(device),
        canNavigateForward = viewModel.canNavigateForward(device),
        canNavigateUp = viewModel.canNavigateUp(device),
        onNavigateBack = { viewModel.navigateBack(device) },
        onNavigateForward = { viewModel.navigateForward(device) },
        onNavigateUp = { viewModel.navigateUp(device) },
        onNavigateHome = { viewModel.navigateHome(device) },
        onOpenPathInput = { viewModel.openPathFromInput(device) },
        backDescription = strings.menuBack,
        forwardDescription = strings.menuForward,
        upDescription = strings.menuUp,
        homeDescription = strings.menuHome,
        viewModeOptions = viewModel.viewModes,
        selectedViewMode = viewModel.selectedViewMode,
        viewModeLabelOf = ::ViewModeLabel,
        onViewModeSelected = viewModel::onViewModeSelected,
        sortModeOptions = viewModel.sortModes,
        selectedSortMode = viewModel.selectedSortMode,
        sortModeLabelOf = ::SortModeLabel,
        onSortModeSelected = viewModel::onSortModeSelected
    )
}

@Composable
private fun FileListArea(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (viewModel.isListViewMode)
            FileListView(viewModel = viewModel, device = device)
        else FileIconView(viewModel = viewModel, device = device)
        val hint = viewModel.fileListHintOf(device)
        val hintIcon = FileListHintIcon(hint)
        val hintMessage = FileListHintMessage(hint)
        if (hintIcon != null && hintMessage.isNotBlank())
            FileListHint(
                iconKey = hintIcon,
                message = hintMessage,
                modifier = Modifier.align(Alignment.Center)
            )
    }
}

@Composable
private fun FileListView(viewModel: MainStageModel, device: AndroidDeviceItem) {
    val interactionState = rememberFileAreaInteractionState(device)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.listScrollIndexOf(device),
        initialFirstVisibleItemScrollOffset = viewModel.listScrollOffsetOf(device)
    )
    val horizontalScrollState = rememberScrollState(viewModel.listHorizontalScrollOffsetOf(device))
    val directoryChangeVersion = viewModel.directoryChangeVersionOf(device)
    var handledDirectoryChangeVersion by remember(device.serial) { mutableStateOf(directoryChangeVersion) }
    val entries = viewModel.entriesOf(device)
    val selectedEntry = viewModel.selectedEntryOf(device)

    LaunchedEffect(device.serial) {
        val selectedIndex = selectedEntry?.let { entries.indexOf(it) } ?: -1
        if (selectedIndex < 0) return@LaunchedEffect
        repeat(2) { withFrameNanos { } }
        if (!listState.isIndexVisible(selectedIndex))
            listState.scrollToItem(selectedIndex)
    }
    LaunchedEffect(device.serial, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        interactionState.dismissContextMenu()
        listState.scrollToItem(0)
        horizontalScrollState.scrollTo(0)
    }
    LaunchedEffect(device.serial, listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.updateListScrollState(device, index, offset)
            }
    }
    LaunchedEffect(device.serial, horizontalScrollState) {
        snapshotFlow { horizontalScrollState.value }
            .distinctUntilChanged()
            .collect { offset ->
                viewModel.updateListHorizontalScrollState(device, offset)
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FileListHeader(
            horizontalScrollState = horizontalScrollState,
            nameWidth = viewModel.fileColumnWidthNamePx.dp,
            sizeWidth = viewModel.fileColumnWidthSizePx.dp,
            modifiedWidth = viewModel.fileColumnWidthModifiedPx.dp,
            permissionWidth = viewModel.fileColumnWidthPermissionPx.dp,
            nameLabel = strings.mainHeaderName,
            sizeLabel = strings.mainHeaderSize,
            modifiedLabel = strings.mainHeaderModified,
            permissionLabel = strings.mainHeaderPermission,
            onResizeNameAndSize = viewModel::resizeNameAndSizeColumns,
            onResizeSizeAndModified = viewModel::resizeSizeAndModifiedColumns,
            onResizeModifiedAndPermission = viewModel::resizeModifiedAndPermissionColumns,
            onResizeFinished = viewModel::persistFileColumnWidths
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { interactionState.contentCoordinates = it }
                .onBlankPrimaryPress { position ->
                    interactionState.clearSelectionIfBlank(position) {
                        viewModel.setSelectedEntry(device, null)
                    }
                }
                .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                    interactionState.openBlankContextMenu(position)
                }
        ) {
            val showHorizontalScrollbar = horizontalScrollState.maxValue > 0
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(viewModel.entriesOf(device)) { entry ->
                    DisposableEffect(entry) {
                        onDispose { interactionState.visibleEntryBounds.remove(entry) }
                    }
                    FileListRow(
                        horizontalScrollState = horizontalScrollState,
                        item = entry,
                        selected = viewModel.selectedEntryOf(device) == entry,
                        nameWidth = viewModel.fileColumnWidthNamePx.dp,
                        sizeWidth = viewModel.fileColumnWidthSizePx.dp,
                        modifiedWidth = viewModel.fileColumnWidthModifiedPx.dp,
                        permissionWidth = viewModel.fileColumnWidthPermissionPx.dp,
                        onClick = { viewModel.setSelectedEntry(device, entry) },
                        onDoubleClick = { viewModel.openEntry(device, entry) },
                        onSecondaryClick = { position ->
                            viewModel.setSelectedEntry(device, entry)
                            interactionState.openEntryContextMenu(entry, position)
                        },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            interactionState.visibleEntryBounds[entry] = coordinates.boundsInRoot()
                        },
                        overlay = {
                            FileEntryContextMenuPopup(
                                viewModel = viewModel,
                                device = device,
                                item = entry,
                                state = interactionState.contextMenuState,
                                onDismissRequest = interactionState::dismissContextMenu
                            )
                        }
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
            if (showHorizontalScrollbar) {
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(horizontalScrollState),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
            FileBlankContextMenuPopup(
                viewModel = viewModel,
                device = device,
                state = interactionState.contextMenuState,
                onDismissRequest = { interactionState.contextMenuState = null }
            )
        }
    }
}

@Composable
private fun FileIconView(viewModel: MainStageModel, device: AndroidDeviceItem) {
    val interactionState = rememberFileAreaInteractionState(device)
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.iconScrollRowIndexOf(device),
        initialFirstVisibleItemScrollOffset = viewModel.iconScrollRowOffsetOf(device)
    )
    val itemMinWidth = 120.dp
    val directoryChangeVersion = viewModel.directoryChangeVersionOf(device)
    var handledDirectoryChangeVersion by remember(device.serial) { mutableStateOf(directoryChangeVersion) }
    val entries = viewModel.entriesOf(device)
    val selectedEntry = viewModel.selectedEntryOf(device)

    LaunchedEffect(device.serial, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        interactionState.dismissContextMenu()
        gridState.scrollToItem(0)
    }
    LaunchedEffect(device.serial, gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.updateIconScrollState(device, index, offset)
            }
    }
    LaunchedEffect(device.serial) {
        val selectedIndex = selectedEntry?.let { entries.indexOf(it) } ?: -1
        if (selectedIndex < 0) return@LaunchedEffect
        repeat(2) { withFrameNanos {} }
        if (!gridState.isIndexVisible(selectedIndex))
            gridState.scrollToItem(selectedIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { interactionState.contentCoordinates = it }
            .onBlankPrimaryPress { position ->
                interactionState.clearSelectionIfBlank(position) {
                    viewModel.setSelectedEntry(device, null)
                }
            }
            .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                interactionState.openBlankContextMenu(position)
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = itemMinWidth),
            state = gridState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(entries) { entry ->
                DisposableEffect(entry) {
                    onDispose { interactionState.visibleEntryBounds.remove(entry) }
                }
                FileIconItem(
                    item = entry,
                    selected = viewModel.selectedEntryOf(device) == entry,
                    onClick = { viewModel.setSelectedEntry(device, entry) },
                    onDoubleClick = { viewModel.openEntry(device, entry) },
                    onSecondaryClick = { position ->
                        viewModel.setSelectedEntry(device, entry)
                        interactionState.openEntryContextMenu(entry, position)
                    },
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            interactionState.visibleEntryBounds[entry] = coordinates.boundsInRoot()
                        }
                        .fillMaxWidth()
                        .height(104.dp)
                        .padding(4.dp),
                    overlay = {
                        FileEntryContextMenuPopup(
                            viewModel = viewModel,
                            device = device,
                            item = entry,
                            state = interactionState.contextMenuState,
                            onDismissRequest = interactionState::dismissContextMenu
                        )
                    }
                )
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(gridState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
        FileBlankContextMenuPopup(
            viewModel = viewModel,
            device = device,
            state = interactionState.contextMenuState,
            onDismissRequest = { interactionState.contextMenuState = null }
        )
    }
}

@Composable
private fun FileBlankContextMenuPopup(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    state: FileContextMenuState?,
    onDismissRequest: () -> Unit
) {
    val blankState = state as? FileContextMenuState.Blank ?: return

    PopupMenu(
        onDismissRequest = {
            onDismissRequest()
            true
        },
        popupPositionProvider = rememberPopupPositionProviderAtPosition(blankState.position),
        popupProperties = PopupProperties(focusable = false)
    ) {
        blankFileContextMenu(
            viewModel = viewModel,
            device = device,
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
private fun FileEntryContextMenuPopup(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    item: DeviceFileItem,
    state: FileContextMenuState?,
    onDismissRequest: () -> Unit
) {
    val entryState = state as? FileContextMenuState.Entry ?: return
    if (entryState.item != item) return

    PopupMenu(
        onDismissRequest = {
            onDismissRequest()
            true
        },
        popupPositionProvider = rememberPopupPositionProviderAtPosition(entryState.position),
        popupProperties = PopupProperties(focusable = false)
    ) {
        entryFileContextMenu(
            viewModel = viewModel,
            device = device,
            item = item,
            onDismissRequest = onDismissRequest
        )
    }
}

private fun MenuScope.entryFileContextMenu(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    item: DeviceFileItem,
    onDismissRequest: () -> Unit
) {
    fun perform(action: () -> Unit) {
        onDismissRequest()
        viewModel.setSelectedEntry(device, item)
        action()
    }

    selectableItem(
        selected = false,
        onClick = { perform { viewModel.openEntry(device, item) } }
    ) { Text(strings.menuOpen) }
    if (!item.isDirectory)
        selectableItem(
            selected = false,
            onClick = { perform { viewModel.openEntryWith(device, item) } }
        ) { Text(strings.menuOpenWith) }
    separator()
    selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.Edit,
        onClick = { perform(viewModel::renameSelectedEntry) }
    ) { Text(strings.menuRename) }
    selectableItemWithActionType(
        selected = false,
        iconKey = AllIconsKeys.Actions.Copy,
        actionType = CopyMenuItemOptionAction,
        onClick = { perform(viewModel::copySelectedEntry) }
    ) { Text(strings.menuCopy) }
    selectableItemWithActionType(
        selected = false,
        iconKey = AllIconsKeys.Actions.MenuCut,
        actionType = CutMenuItemOptionAction,
        onClick = { perform(viewModel::cutSelectedEntry) }
    ) { Text(strings.menuCut) }
    selectableItem(
        selected = false,
        iconKey = AllIconsKeys.General.Delete,
        onClick = { perform(viewModel::deleteSelectedEntry) }
    ) { Text(strings.menuDelete) }
    separator()
    selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.Properties,
        onClick = { perform(viewModel::showSelectedEntryProperties) }
    ) { Text(strings.menuProperties) }
}

private fun MenuScope.blankFileContextMenu(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    onDismissRequest: () -> Unit
) {
    val hasEntries = viewModel.entriesOf(device).isNotEmpty()
    val hasSelectedDevice = viewModel.isSelectedWorkspace(device)

    fun perform(action: () -> Unit) {
        onDismissRequest()
        action()
    }

    selectableItem(
        selected = false,
        enabled = hasSelectedDevice,
        iconKey = AllIconsKeys.Actions.Refresh,
        onClick = { perform(viewModel::refreshEntries) }
    ) { Text(strings.menuRefresh) }
    separator()
    selectableItem(
        selected = false,
        enabled = hasSelectedDevice,
        iconKey = AllIconsKeys.Actions.NewFolder,
        onClick = { perform(viewModel::createNewFolder) }
    ) { Text(strings.menuNewFolder) }
    separator()
    if (viewModel.canPasteEntry) {
        selectableItemWithActionType(
            selected = false,
            iconKey = AllIconsKeys.Actions.MenuPaste,
            actionType = PasteMenuItemOptionAction,
            onClick = { perform(viewModel::pasteToCurrentPath) }
        ) { Text(strings.menuPaste) }
        separator()
    }
    selectableItemWithActionType(
        selected = false,
        enabled = hasEntries,
        actionType = SelectAllMenuItemOptionAction,
        onClick = { perform(viewModel::selectAllEntries) }
    ) { Text(strings.menuSelectAll) }
    selectableItem(
        selected = false,
        enabled = hasEntries,
        onClick = { perform(viewModel::inverseSelectEntries) }
    ) { Text(strings.menuInverseSelect) }
}

@Composable
private fun RenderDialogs(viewModel: MainStageModel) {
    when (val state = viewModel.dialogState) {
        MainStageModel.DialogState.None -> Unit
        MainStageModel.DialogState.NewFolder ->
            SimpleInputDialog(
                title = strings.dialogNewFolderTitle,
                prompt = strings.dialogNewFolderPrompt,
                confirmText = strings.dialogNewFolderCreate,
                cancelText = strings.dialogCommonCancel,
                invalidInputText = strings.dialogInputInvalid,
                onCloseRequest = viewModel::dismissDialog,
                onConfirm = viewModel::confirmCreateFolder
            )
        is MainStageModel.DialogState.Rename ->
            SimpleInputDialog(
                title = strings.dialogRenameTitle,
                prompt = strings.dialogRenamePrompt.replace("{0}", state.initialName),
                confirmText = strings.dialogRenameConfirm,
                cancelText = strings.dialogCommonCancel,
                invalidInputText = strings.dialogInputInvalid,
                onCloseRequest = viewModel::dismissDialog,
                onConfirm = viewModel::confirmRenameSelectedEntry,
                initialValue = state.initialName
            )
        is MainStageModel.DialogState.DeleteConfirm ->
            ConfirmDialog(
                title = strings.dialogDeleteTitle,
                message = strings.dialogDeleteConfirm,
                confirmText = strings.dialogDeleteConfirmButton,
                cancelText = strings.dialogCommonCancel,
                onCloseRequest = viewModel::dismissDialog,
                onConfirm = viewModel::confirmDeleteSelectedEntry
            )
        is MainStageModel.DialogState.Properties ->
            FilePropertiesDialog(
                snapshot = state.snapshot,
                onCloseRequest = viewModel::dismissDialog,
                loadPermission = { viewModel.loadPermission(state.snapshot) },
                applyPermission = { viewModel.applyPermission(state.snapshot, it, reportStatus = false) }
            )
    }
}

@Composable
private fun ViewModeLabel(modeKey: String?) = when (modeKey) {
    "icons" -> strings.mainViewModeIcons
    "list" -> strings.mainViewModeList
    else -> ""
}

@Composable
private fun SortModeLabel(modeKey: String?) = when (modeKey) {
    "size" -> strings.mainSortModeSize
    "modified" -> strings.mainSortModeModified
    "name" -> strings.mainSortModeName
    else -> ""
}

@Composable
private fun StatusMessageText(status: MainStageModel.StatusMessage): String = when (status) {
    MainStageModel.StatusMessage.None -> ""
    is MainStageModel.StatusMessage.Raw -> status.message
    is MainStageModel.StatusMessage.Res -> {
        val template = when (status.key) {
            MainStageModel.StatusMessage.Key.CommonUnknownError -> strings.commonUnknownError
            MainStageModel.StatusMessage.Key.DevicesUpdated -> strings.statusDevicesUpdated
            MainStageModel.StatusMessage.Key.SelectDeviceFirst -> strings.statusSelectDeviceFirst
            MainStageModel.StatusMessage.Key.InvalidFolderName -> strings.statusInvalidFolderName
            MainStageModel.StatusMessage.Key.FolderCreated -> strings.statusFolderCreated
            MainStageModel.StatusMessage.Key.SelectEntryFirst -> strings.statusSelectEntryFirst
            MainStageModel.StatusMessage.Key.InvalidName -> strings.statusInvalidName
            MainStageModel.StatusMessage.Key.Renamed -> strings.statusRenamed
            MainStageModel.StatusMessage.Key.EntryDeleted -> strings.statusEntryDeleted
            MainStageModel.StatusMessage.Key.Copied -> strings.statusCopied
            MainStageModel.StatusMessage.Key.Cut -> strings.statusCut
            MainStageModel.StatusMessage.Key.ClipboardEmpty -> strings.statusClipboardEmpty
            MainStageModel.StatusMessage.Key.CrossDevicePasteNotSupported -> strings.statusCrossDevicePasteNotSupported
            MainStageModel.StatusMessage.Key.Pasted -> strings.statusPasted
            MainStageModel.StatusMessage.Key.DialogPropertiesInvalidPermission -> strings.dialogPropertiesInvalidPermission
            MainStageModel.StatusMessage.Key.DialogPropertiesPermissionUpdated -> strings.dialogPropertiesPermissionUpdated
        }
        template.formatWithArgs(status.args)
    }
}

@Composable
private fun FileListHintIcon(hint: MainStageModel.FileListHint) = when (hint) {
    MainStageModel.FileListHint.None -> null
    MainStageModel.FileListHint.EmptyFolder -> AppIcons.Folder
    MainStageModel.FileListHint.DeviceOffline,
    MainStageModel.FileListHint.DeviceNotFound -> AppIcons.DeletedFolder
    MainStageModel.FileListHint.PermissionDenied -> AppIcons.BlockedFolder
    MainStageModel.FileListHint.PathNotFound,
    MainStageModel.FileListHint.LoadFailed -> AppIcons.ErrorFolder
}

@Composable
private fun FileListHintMessage(hint: MainStageModel.FileListHint) = when (hint) {
    MainStageModel.FileListHint.None -> ""
    MainStageModel.FileListHint.EmptyFolder -> strings.mainFileListHintEmptyFolder
    MainStageModel.FileListHint.DeviceOffline -> strings.mainFileListHintDeviceOffline
    MainStageModel.FileListHint.DeviceNotFound -> strings.mainFileListHintDeviceNotFound
    MainStageModel.FileListHint.PathNotFound -> strings.mainFileListHintPathNotFound
    MainStageModel.FileListHint.PermissionDenied -> strings.mainFileListHintPermissionDenied
    MainStageModel.FileListHint.LoadFailed -> strings.mainFileListHintLoadFailed
}

private sealed interface FileContextMenuState {
    data class Entry(val item: DeviceFileItem, val position: Offset, val requestId: Long) : FileContextMenuState
    data class Blank(val position: Offset, val requestId: Long) : FileContextMenuState
}

private class FileAreaInteractionState {

    private var contextMenuRequestId by mutableStateOf(0L)

    var contextMenuState by mutableStateOf<FileContextMenuState?>(null)
    val visibleEntryBounds = mutableStateMapOf<DeviceFileItem, Rect>()
    var contentCoordinates by mutableStateOf<LayoutCoordinates?>(null)

    fun openBlankContextMenu(position: Offset) {
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Blank(position, contextMenuRequestId)
    }

    fun openEntryContextMenu(entry: DeviceFileItem, position: Offset) {
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Entry(entry, position, contextMenuRequestId)
    }

    fun dismissContextMenu() {
        contextMenuState = null
    }

    fun clearSelectionIfBlank(position: Offset, onBlankAreaPressed: () -> Unit) {
        contextMenuState = null
        val rootPosition = contentCoordinates?.localToRoot(position) ?: position
        if (visibleEntryBounds.values.none { it.contains(rootPosition) }) {
            onBlankAreaPressed()
        }
    }
}

@Composable
private fun rememberFileAreaInteractionState(device: AndroidDeviceItem) = remember(device.serial) {
    FileAreaInteractionState()
}