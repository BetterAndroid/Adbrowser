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

package com.highcapable.adbrowser.app.ui.stage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import androidx.compose.ui.zIndex
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.component.DevicePanePanel
import com.highcapable.adbrowser.app.ui.component.FileIconItem
import com.highcapable.adbrowser.app.ui.component.FileListHeader
import com.highcapable.adbrowser.app.ui.component.FileListHint
import com.highcapable.adbrowser.app.ui.component.FileListRow
import com.highcapable.adbrowser.app.ui.component.FileNavigationBar
import com.highcapable.adbrowser.app.ui.component.FixedWidthHorizontalSplitLayout
import com.highcapable.adbrowser.app.ui.component.PanelSurface
import com.highcapable.adbrowser.app.ui.component.PathBreadcrumbBar
import com.highcapable.adbrowser.app.ui.component.StatusBar
import com.highcapable.adbrowser.app.ui.dialog.ConfirmDialog
import com.highcapable.adbrowser.app.ui.dialog.ConfirmDialogIcon
import com.highcapable.adbrowser.app.ui.dialog.DeviceConnectDialog
import com.highcapable.adbrowser.app.ui.dialog.DevicePairDialog
import com.highcapable.adbrowser.app.ui.dialog.FilePropertiesDialog
import com.highcapable.adbrowser.app.ui.foundation.isIndexFullyVisible
import com.highcapable.adbrowser.app.ui.foundation.revealIndexBySingleStep
import com.highcapable.adbrowser.app.ui.interaction.SelectionAreaState
import com.highcapable.adbrowser.app.ui.interaction.blankAreaDragSelection
import com.highcapable.adbrowser.app.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.MainStageModel
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.app.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.app.ui.vm.model.MenuShortcut
import com.highcapable.adbrowser.app.ui.vm.model.type.FileViewMode
import com.highcapable.adbrowser.core.common.utils.BuildVersion
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.common.utils.extension.formatWithArgs
import kotlinx.coroutines.flow.distinctUntilChanged
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
    RenderDialogs(viewModel = viewModel)
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
            firstPaneMinWidth = FirstPaneMinWidth,
            secondPaneMinWidth = SecondPaneMinWidth,
            dividerWidth = PaneDividerWidth,
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
        if (viewModel.isStatusBarVisible)
            StatusBar(
                text = MainStatusBarText(viewModel),
                versionText = BuildVersion.TEXT
            )
    }
}

@Composable
private fun DevicePane(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val interactionState = remember { DevicePaneInteractionState() }
    var popupHostCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { popupHostCoordinates = it }
    ) {
        DevicePanePanel(
            devices = viewModel.devices,
            selectedDevice = viewModel.selectedDevice,
            listState = listState,
            title = strings.mainDevicesTitle,
            noDeviceMessage = strings.mainDeviceListHintNoDevice,
            onOpenActionMenu = interactionState::openActionMenu,
            onRefresh = viewModel::refreshDevices,
            onDeviceClick = viewModel::selectDevice,
            popupHostCoordinates = { popupHostCoordinates },
            onDeviceSecondaryClick = { device, position ->
                viewModel.selectDevice(device)

                if (viewModel.canDisconnectDevice(device))
                    interactionState.openContextMenu(device, position)
            },
            modifier = Modifier.fillMaxSize()
        )
        DeviceActionMenuPopup(
            state = interactionState.actionMenuState,
            onPairNewDevice = {
                interactionState.dismissActionMenu()
                viewModel.pairNewDevice()
            },
            onConnectToDevice = {
                interactionState.dismissActionMenu()
                viewModel.connectToDevice()
            },
            onDismissRequest = interactionState::dismissActionMenu
        )
        DeviceContextMenuPopup(
            state = interactionState.contextMenuState,
            onDisconnect = viewModel::disconnectDevice,
            onDismissRequest = interactionState::dismissContextMenu
        )
    }
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
                // Every device keeps its own pane instance alive. We only hide/show them so
                // switching devices can preserve selection, scroll state, and cached entries.
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
        selectedViewMode = viewModel.selectedViewMode,
        onViewModeSelected = viewModel::onViewModeSelected,
        selectedSortMode = viewModel.selectedSortMode,
        onSortModeSelected = viewModel::onSortModeSelected
    )
}

@Composable
private fun FileListArea(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember(device) { FocusRequester() }
    val entryPositionController = remember(device) { EntryPositionController() }
    val entries = viewModel.entriesOf(device)
    val selectedEntry = viewModel.selectedEntryOf(device)
    val pendingRevealEntryPath = viewModel.pendingRevealEntryPathOf(device)

    LaunchedEffect(device, viewModel.selectedViewMode) {
        val selectedIndex = selectedEntryIndex(entries, selectedEntry)
        // When switching between list and icon view, keep the current selection in sight if
        // possible instead of resetting to the top. The request is not force-scrolled so the
        // target remains still when it is already visible in the new layout.
        if (selectedIndex >= 0) entryPositionController.request(index = selectedIndex, forceScroll = false)
    }
    LaunchedEffect(device, pendingRevealEntryPath, entries) {
        val targetPath = pendingRevealEntryPath ?: return@LaunchedEffect
        val targetIndex = entries.indexOfFirst { viewModel.entryFullPathOf(it) == targetPath }
        if (targetIndex < 0) return@LaunchedEffect

        entryPositionController.request(index = targetIndex, forceScroll = true)
        viewModel.consumePendingRevealEntryPath(device, targetPath)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (viewModel.selectedViewMode == FileViewMode.List)
            FileListView(
                viewModel = viewModel,
                device = device,
                focusRequester = focusRequester,
                entryPositionController = entryPositionController
            )
        else FileIconView(
            viewModel = viewModel,
            device = device,
            focusRequester = focusRequester,
            entryPositionController = entryPositionController
        )

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
private fun FileListView(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    focusRequester: FocusRequester,
    entryPositionController: EntryPositionController
) {
    val interactionState = rememberFileAreaInteractionState(device)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.listScrollIndexOf(device),
        initialFirstVisibleItemScrollOffset = viewModel.listScrollOffsetOf(device)
    )
    val horizontalScrollState = rememberScrollState(viewModel.listHorizontalScrollOffsetOf(device))
    val directoryChangeVersion = viewModel.directoryChangeVersionOf(device)
    var handledDirectoryChangeVersion by remember(device) { mutableStateOf(directoryChangeVersion) }
    val inlineRenameActive = viewModel.isInlineRenameActive(device)
    var previousInlineRenameActive by remember(device) { mutableStateOf(inlineRenameActive) }

    val entries = viewModel.entriesOf(device)

    LaunchedEffect(entryPositionController.currentRequest, entries) {
        val request = entryPositionController.currentRequest ?: return@LaunchedEffect
        if (request.index !in entries.indices) {
            entryPositionController.consume(request)
            return@LaunchedEffect
        }

        // Lazy list layout info is not immediately valid after data or mode changes.
        // Waiting a couple of frames avoids scrolling against stale measurement data.
        repeat(2) { withFrameNanos {} }
        when {
            request.forceScroll -> listState.scrollToItem(request.index)
            !listState.isIndexFullyVisible(request.index) ->
                if (request.preferSingleStepReveal)
                    listState.revealIndexBySingleStep(request.index)
                else listState.scrollToItem(request.index)
        }
        entryPositionController.consume(request)
    }
    LaunchedEffect(device, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        interactionState.dismissContextMenu()

        // Directory changes intentionally reset both axes. A same-path refresh does not bump the
        // version, so manual refresh can preserve the current viewport when desired.
        listState.scrollToItem(0)
        horizontalScrollState.scrollTo(0)
    }
    LaunchedEffect(device, listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.updateListScrollState(device, index, offset)
            }
    }
    LaunchedEffect(device, horizontalScrollState) {
        snapshotFlow { horizontalScrollState.value }
            .distinctUntilChanged()
            .collect { offset ->
                viewModel.updateListHorizontalScrollState(device, offset)
            }
    }
    LaunchedEffect(device, inlineRenameActive) {
        // Enter/Escape closes the inline editor by removing the focused composable. We explicitly
        // return focus to the file area so arrow keys and shortcuts keep working immediately.
        if (previousInlineRenameActive && !inlineRenameActive) focusRequester.requestFocus()
        previousInlineRenameActive = inlineRenameActive
    }
    val newFolderBaseName = strings.mainFileListNewFolderName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (inlineRenameActive) return@onPreviewKeyEvent false

                handleFileAreaShortcut(
                    viewModel = viewModel,
                    device = device,
                    event = it,
                    newFolderBaseName = newFolderBaseName,
                    onBeginInlineRename = interactionState::dismissContextMenu,
                    onNavigateSelection = { direction ->
                        val targetIndex = when (direction) {
                            MainStageModel.NavigationDirection.Up,
                            MainStageModel.NavigationDirection.Down -> {
                                viewModel.navigateSelection(device, direction)
                            }
                            MainStageModel.NavigationDirection.Left,
                            MainStageModel.NavigationDirection.Right -> null
                        } ?: return@handleFileAreaShortcut false

                        entryPositionController.request(
                            index = targetIndex,
                            forceScroll = false,
                            preferSingleStepReveal = true
                        )
                        true
                    },
                    onNavigateByInitialChar = { navigateChar ->
                        val targetIndex = findEntryIndexByInitialChar(entries, navigateChar)
                        if (targetIndex < 0) return@handleFileAreaShortcut false

                        val targetEntry = entries[targetIndex]
                        viewModel.setSelectedEntry(device, targetEntry)
                        entryPositionController.request(index = targetIndex, forceScroll = true)
                        true
                    }
                )
            }
            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
                // File area shortcuts should work immediately after a click, so focus is claimed
                // on pointer press instead of waiting for child composable to become focused.
                if (inlineRenameActive) return@onPointerEvent
                focusRequester.requestFocus()
            }
    ) {
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
                .nestedScroll(interactionState.selectionAreaState.nestedScrollConnection)
                .onGloballyPositioned { interactionState.selectionAreaState.contentCoordinates = it }
                .fileAreaBlankSelection(
                    interactionState = interactionState,
                    showSelectionRect = false,
                    selectedPathsProvider = { viewModel.selectedEntryPathsOf(device) },
                    onClearSelection = { viewModel.clearSelectedEntries(device) },
                    onSelectionChanged = { candidatePaths, additive, initialSelectionPaths ->
                        viewModel.updateDragSelection(device, candidatePaths, additive, initialSelectionPaths)
                    },
                    autoScrollBy = { delta ->
                        val consumed = listState.scrollBy(delta)
                        interactionState.selectionAreaState.cumulativeScrollY += consumed
                    }
                )
                .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                    if (viewModel.commitInlineRenameOnFocusLoss(device))
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
                        onDispose { interactionState.selectionAreaState.removeVisibleItemBounds(entry) }
                    }
                    FileListRow(
                        horizontalScrollState = horizontalScrollState,
                        item = entry,
                        displayName = viewModel.displayNameOf(device, entry),
                        selected = viewModel.isEntrySelected(device, entry),
                        canTapLabelToRename = viewModel.isEntrySelected(device, entry) &&
                            !viewModel.hasMultipleSelectedEntries(device),
                        isInlineRenaming = viewModel.isEntryInlineRenaming(device, entry),
                        inlineRenameInput = viewModel.inlineRenameInputOf(device),
                        nameWidth = viewModel.fileColumnWidthNamePx.dp,
                        sizeWidth = viewModel.fileColumnWidthSizePx.dp,
                        modifiedWidth = viewModel.fileColumnWidthModifiedPx.dp,
                        permissionWidth = viewModel.fileColumnWidthPermissionPx.dp,
                        onPrimaryClick = { appendSelection, rangeSelection ->
                            viewModel.selectEntryByGesture(device, entry, appendSelection, rangeSelection)
                        },
                        onDoubleClick = {
                            if (viewModel.consumeDoubleOpenSuppression(device, entry)) return@FileListRow
                            if (viewModel.hasMultipleSelectedEntries(device)) return@FileListRow

                            viewModel.openEntry(device, entry)
                        },
                        onSecondaryClick = { position ->
                            viewModel.ensureEntrySelectedForContextMenu(device, entry)
                            interactionState.openEntryContextMenu(entry, position)
                        },
                        shouldHandlePrimaryInteraction = interactionState::prepareEntryPrimaryInteraction,
                        onBeginInlineRename = {
                            interactionState.dismissContextMenu()
                            viewModel.beginInlineRename(device, entry)
                        },
                        onConfirmInlineRename = {
                            viewModel.confirmInlineRename(device)
                        },
                        onCancelInlineRename = {
                            viewModel.cancelInlineRename(device)
                        },
                        shouldRestoreFocusOnInlineRenameFailure = {
                            viewModel.isEntryInlineRenaming(device, entry)
                        },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            // Visible entry bounds drive blank-area hit testing and marquee
                            // selection, so they must track the actual composed coordinates.
                            val bounds = coordinates.boundsInRoot()
                            interactionState.selectionAreaState.updateVisibleItemBounds(entry, listOf(bounds))
                        },
                        overlay = {
                            FileEntryContextMenuPopup(
                                viewModel = viewModel,
                                device = device,
                                item = entry,
                                state = interactionState.contextMenuState,
                                onDismissRequest = interactionState::dismissContextMenu,
                                onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextPrimaryPress
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
                onDismissRequest = interactionState::dismissContextMenu,
                onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextPrimaryPress
            )
        }
    }
}

@Composable
private fun FileIconView(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    focusRequester: FocusRequester,
    entryPositionController: EntryPositionController
) {
    val interactionState = rememberFileAreaInteractionState(device)
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.iconScrollRowIndexOf(device),
        initialFirstVisibleItemScrollOffset = viewModel.iconScrollRowOffsetOf(device)
    )

    val directoryChangeVersion = viewModel.directoryChangeVersionOf(device)
    var handledDirectoryChangeVersion by remember(device) { mutableStateOf(directoryChangeVersion) }
    val inlineRenameActive = viewModel.isInlineRenameActive(device)
    var previousInlineRenameActive by remember(device) { mutableStateOf(inlineRenameActive) }

    val entries = viewModel.entriesOf(device)

    LaunchedEffect(device, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        interactionState.dismissContextMenu()
        gridState.scrollToItem(0)
    }
    LaunchedEffect(device, gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.updateIconScrollState(device, index, offset)
            }
    }
    LaunchedEffect(entryPositionController.currentRequest, entries) {
        val request = entryPositionController.currentRequest ?: return@LaunchedEffect
        if (request.index !in entries.indices) {
            entryPositionController.consume(request)
            return@LaunchedEffect
        }

        // Grid layout typically needs a little more time to stabilize than the list variant
        // because adaptive columns and card heights both influence the final row geometry.
        repeat(4) { withFrameNanos {} }
        if (request.forceScroll) {
            gridState.scrollToItem(request.index)
        } else if (!gridState.isIndexFullyVisible(request.index)) {
            if (request.preferSingleStepReveal)
                gridState.revealIndexBySingleStep(request.index)
            else gridState.scrollToItem(request.index)
        }
        entryPositionController.consume(request)
    }
    LaunchedEffect(device, inlineRenameActive) {
        if (previousInlineRenameActive && !inlineRenameActive) focusRequester.requestFocus()
        previousInlineRenameActive = inlineRenameActive
    }
    val newFolderBaseName = strings.mainFileListNewFolderName

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val gridColumnCount = (maxWidth / DefaultFileItemMinWidth).toInt().coerceAtLeast(1)

        // Keyboard left/right navigation depends on the current adaptive column count.
        // Recomputing from constraints keeps the navigation model aligned with the visible grid.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent {
                    if (inlineRenameActive) return@onPreviewKeyEvent false

                    handleFileAreaShortcut(
                        viewModel = viewModel,
                        device = device,
                        event = it,
                        newFolderBaseName = newFolderBaseName,
                        onBeginInlineRename = interactionState::dismissContextMenu,
                        onNavigateSelection = { direction ->
                            val targetIndex = viewModel.navigateSelection(
                                device = device,
                                direction = direction,
                                gridColumnCount = gridColumnCount
                            ) ?: return@handleFileAreaShortcut false

                            entryPositionController.request(
                                index = targetIndex,
                                forceScroll = false,
                                preferSingleStepReveal = true
                            )
                            true
                        },
                        onNavigateByInitialChar = { navigateChar ->
                            val targetIndex = findEntryIndexByInitialChar(entries, navigateChar)
                            if (targetIndex < 0) return@handleFileAreaShortcut false

                            val targetEntry = entries[targetIndex]
                            viewModel.setSelectedEntry(device, targetEntry)
                            entryPositionController.request(index = targetIndex, forceScroll = true)
                            true
                        }
                    )
                }
                .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
                    if (inlineRenameActive) return@onPointerEvent
                    focusRequester.requestFocus()
                }
                .nestedScroll(interactionState.selectionAreaState.nestedScrollConnection)
                .onGloballyPositioned { interactionState.selectionAreaState.contentCoordinates = it }
                .fileAreaBlankSelection(
                    interactionState = interactionState,
                    showSelectionRect = true,
                    selectedPathsProvider = { viewModel.selectedEntryPathsOf(device) },
                    onClearSelection = { viewModel.clearSelectedEntries(device) },
                    onSelectionChanged = { candidatePaths, additive, initialSelectionPaths ->
                        viewModel.updateDragSelection(device, candidatePaths, additive, initialSelectionPaths)
                    },
                    autoScrollBy = { delta ->
                        val consumed = gridState.scrollBy(delta)
                        interactionState.selectionAreaState.cumulativeScrollY += consumed
                    }
                )
                .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                    if (viewModel.commitInlineRenameOnFocusLoss(device))
                        interactionState.openBlankContextMenu(position)
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = DefaultFileItemMinWidth),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(entries) { entry ->
                    DisposableEffect(entry) {
                        onDispose { interactionState.selectionAreaState.removeVisibleItemBounds(entry) }
                    }
                    FileIconItem(
                        item = entry,
                        displayName = viewModel.displayNameOf(device, entry),
                        selected = viewModel.isEntrySelected(device, entry),
                        canTapLabelToRename = viewModel.isEntrySelected(device, entry) &&
                            !viewModel.hasMultipleSelectedEntries(device),
                        isInlineRenaming = viewModel.isEntryInlineRenaming(device, entry),
                        inlineRenameInput = viewModel.inlineRenameInputOf(device),
                        onPrimaryClick = { appendSelection, rangeSelection ->
                            viewModel.selectEntryByGesture(device, entry, appendSelection, rangeSelection)
                        },
                        onDoubleClick = {
                            if (viewModel.consumeDoubleOpenSuppression(device, entry)) return@FileIconItem
                            if (viewModel.hasMultipleSelectedEntries(device)) return@FileIconItem

                            viewModel.openEntry(device, entry)
                        },
                        onSecondaryClick = { position ->
                            viewModel.ensureEntrySelectedForContextMenu(device, entry)
                            interactionState.openEntryContextMenu(entry, position)
                        },
                        shouldHandlePrimaryInteraction = interactionState::prepareEntryPrimaryInteraction,
                        onBeginInlineRename = {
                            interactionState.dismissContextMenu()
                            viewModel.beginInlineRename(device, entry)
                        },
                        onConfirmInlineRename = {
                            viewModel.confirmInlineRename(device)
                        },
                        onCancelInlineRename = {
                            viewModel.cancelInlineRename(device)
                        },
                        shouldRestoreFocusOnInlineRenameFailure = {
                            viewModel.isEntryInlineRenaming(device, entry)
                        },
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = DefaultFileItemOuterPadding),
                        onHitBoundsChanged = { bounds ->
                            // Icon hit targets are split across icon/text regions, so one entry can
                            // contribute multiple rectangles to blank-area and drag-selection logic.
                            interactionState.selectionAreaState.updateVisibleItemBounds(entry, bounds)
                        },
                        overlay = {
                            FileEntryContextMenuPopup(
                                viewModel = viewModel,
                                device = device,
                                item = entry,
                                state = interactionState.contextMenuState,
                                onDismissRequest = interactionState::dismissContextMenu,
                                onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextPrimaryPress
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
            interactionState.selectionAreaState.selectionRect?.let { rect ->
                val accentColor = AdbrowserTheme.colors.primaryAccent

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = accentColor.copy(alpha = 0.14f),
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                    drawRect(
                        color = accentColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            FileBlankContextMenuPopup(
                viewModel = viewModel,
                device = device,
                state = interactionState.contextMenuState,
                onDismissRequest = interactionState::dismissContextMenu,
                onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextPrimaryPress
            )
        }
    }
}

@Composable
private fun FileBlankContextMenuPopup(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    state: FileContextMenuState?,
    onDismissRequest: () -> Unit,
    onDismissByOutsidePress: () -> Unit
) {
    val blankState = state as? FileContextMenuState.Blank ?: return
    val newFolderBaseName = strings.mainFileListNewFolderName

    PopupMenu(
        onDismissRequest = {
            onDismissByOutsidePress()
            true
        },
        popupPositionProvider = rememberPopupPositionProviderAtPosition(blankState.position),
        popupProperties = PopupProperties(focusable = false)
    ) {
        blankFileContextMenu(
            viewModel = viewModel,
            device = device,
            newFolderBaseName = newFolderBaseName,
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
    onDismissRequest: () -> Unit,
    onDismissByOutsidePress: () -> Unit
) {
    val entryState = state as? FileContextMenuState.Entry ?: return
    if (entryState.item != item) return

    PopupMenu(
        onDismissRequest = { onDismissByOutsidePress(); true },
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
    val isMultiSelection = viewModel.hasMultipleSelectedEntries(device) && viewModel.isEntrySelected(device, item)

    fun perform(action: () -> Unit) {
        onDismissRequest()
        viewModel.ensureEntrySelectedForContextMenu(device, item)
        action()
    }

    if (!isMultiSelection) {
        selectableItem(
            selected = false,
            keybinding = viewModel.menuShortcut(MenuShortcut.Action.Open).toMenuKeybinding(),
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
            keybinding = viewModel.menuShortcut(MenuShortcut.Action.Rename).toMenuKeybinding(),
            onClick = { perform(viewModel::renameSelectedEntry) }
        ) { Text(strings.menuRename) }
    }
    selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.Copy,
        keybinding = viewModel.menuShortcut(MenuShortcut.Action.Copy).toMenuKeybinding(),
        onClick = { perform(viewModel::copySelectedEntry) }
    ) { Text(strings.menuCopy) }
    selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.MenuCut,
        keybinding = viewModel.menuShortcut(MenuShortcut.Action.Cut).toMenuKeybinding(),
        onClick = { perform(viewModel::cutSelectedEntry) }
    ) { Text(strings.menuCut) }
    selectableItem(
        selected = false,
        iconKey = AllIconsKeys.General.Delete,
        keybinding = viewModel.menuShortcut(MenuShortcut.Action.Delete).toMenuKeybinding(),
        onClick = { perform(viewModel::deleteSelectedEntry) }
    ) { Text(strings.menuDelete) }
    if (!isMultiSelection) {
        separator()
        selectableItem(
            selected = false,
            iconKey = AllIconsKeys.Actions.Properties,
            keybinding = viewModel.menuShortcut(MenuShortcut.Action.Properties).toMenuKeybinding(),
            onClick = { perform(viewModel::showSelectedEntryProperties) }
        ) { Text(strings.menuProperties) }
    }
}

private fun MenuScope.blankFileContextMenu(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    newFolderBaseName: String,
    onDismissRequest: () -> Unit
) {
    val canShowBlankFileContextMenu = viewModel.canShowBlankFileContextMenu(device)
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
        keybinding = viewModel.menuShortcut(MenuShortcut.Action.Refresh).toMenuKeybinding(),
        onClick = { perform(viewModel::refreshEntries) }
    ) { Text(strings.menuRefresh) }
    if (canShowBlankFileContextMenu) {
        separator()
        selectableItem(
            selected = false,
            enabled = hasSelectedDevice,
            iconKey = AllIconsKeys.Actions.NewFolder,
            keybinding = viewModel.menuShortcut(MenuShortcut.Action.NewFolder).toMenuKeybinding(),
            onClick = { perform { viewModel.createNewFolder(newFolderBaseName) } }
        ) { Text(strings.menuNewFolder) }
        separator()
        if (viewModel.canPasteEntry) {
            selectableItem(
                selected = false,
                iconKey = AllIconsKeys.Actions.MenuPaste,
                keybinding = viewModel.menuShortcut(MenuShortcut.Action.Paste).toMenuKeybinding(),
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
        if (viewModel.canShowCurrentDirectoryProperties(device)) {
            separator()
            selectableItem(
                selected = false,
                enabled = hasSelectedDevice,
                iconKey = AllIconsKeys.Actions.Properties,
                keybinding = viewModel.menuShortcut(MenuShortcut.Action.Properties).toMenuKeybinding(),
                onClick = { perform(viewModel::showCurrentDirectoryProperties) }
            ) { Text(strings.menuProperties) }
        }
    }
}

@Composable
private fun FrameWindowScope.RenderDialogs(viewModel: MainStageModel) {
    when (val state = viewModel.dialogState) {
        MainStageModel.DialogState.None -> Unit
        MainStageModel.DialogState.DeviceConnect ->
            DeviceConnectDialog(
                onCloseRequest = viewModel::dismissDialog,
                ownerWindow = window
            )
        MainStageModel.DialogState.DevicePair ->
            DevicePairDialog(
                onCloseRequest = viewModel::dismissDialog,
                ownerWindow = window
            )
        is MainStageModel.DialogState.DeleteConfirm ->
            ConfirmDialog(
                icon = ConfirmDialogIcon.Question,
                title = strings.dialogDeleteTitle,
                message = if (state.entryCount > 1)
                    strings.dialogDeleteConfirmMultiple.formatWithArgs(state.entryCount)
                else strings.dialogDeleteConfirm,
                confirmText = strings.dialogDeleteConfirmButton,
                cancelText = strings.dialogCommonCancel,
                onCloseRequest = viewModel::dismissDialog,
                ownerWindow = window,
                onConfirm = viewModel::confirmDeleteSelectedEntry
            )
        is MainStageModel.DialogState.FileOperationFailure ->
            ConfirmDialog(
                icon = ConfirmDialogIcon.Warning,
                title = strings.dialogFileOperationFailureTitle,
                message = FileOperationFailureMessage(state),
                confirmText = strings.dialogCommonRetry,
                cancelText = strings.dialogCommonSkip,
                tertiaryText = strings.dialogCommonCancel,
                checkboxText = strings.dialogFileOperationFailureApplyToSubsequent
                    .takeIf { state.canApplyToSubsequent },
                checkboxChecked = state.applyToSubsequent,
                onCheckboxCheckedChange = viewModel::setFileOperationFailureApplyToSubsequent
                    .takeIf { state.canApplyToSubsequent },
                onCloseRequest = viewModel::cancelFileOperationFailure,
                onCancel = viewModel::skipFileOperationFailure,
                onTertiary = viewModel::cancelFileOperationFailure,
                ownerWindow = window,
                onConfirm = {
                    viewModel.retryFileOperationFailure()
                    false
                }
            )
        is MainStageModel.DialogState.RenameError ->
            ConfirmDialog(
                icon = ConfirmDialogIcon.Warning,
                title = strings.dialogRenameErrorTitle,
                message = RenameErrorMessage(state),
                confirmText = strings.dialogCommonOk,
                onCloseRequest = viewModel::dismissDialog,
                ownerWindow = window,
                onConfirm = { true }
            )
        is MainStageModel.DialogState.Properties ->
            FilePropertiesDialog(
                snapshot = state.snapshot,
                onCloseRequest = viewModel::dismissDialog,
                loadPermission = { viewModel.loadPermission(state.snapshot) },
                applyPermission = { viewModel.applyPermission(state.snapshot, it) },
                ownerWindow = window
            )
        is MainStageModel.DialogState.CrossDevicePasteNotSupported ->
            ConfirmDialog(
                title = strings.dialogCommonTitle,
                message = strings.dialogCrossDevicePasteNotSupported,
                confirmText = strings.dialogCommonOk,
                onCloseRequest = viewModel::dismissDialog,
                ownerWindow = window,
                onConfirm = { true }
            )
    }
}

@Composable
private fun StatusMessageText(status: MainStageModel.StatusMessage): String = when (status) {
    MainStageModel.StatusMessage.None -> ""
    is MainStageModel.StatusMessage.Raw -> status.message
    is MainStageModel.StatusMessage.Res -> {
        val template = when (status.key) {
            MainStageModel.StatusMessage.Key.CommonUnknownError -> strings.commonUnknownError
            MainStageModel.StatusMessage.Key.DevicesUpdated -> strings.statusDevicesUpdated
            MainStageModel.StatusMessage.Key.DeviceConnectionPending -> strings.statusDeviceConnectionPending
            MainStageModel.StatusMessage.Key.DeviceConnected -> strings.statusDeviceConnected
            MainStageModel.StatusMessage.Key.DeviceDisconnected -> strings.statusDeviceDisconnected
        }
        template.formatWithArgs(*status.args.toTypedArray())
    }
}

@Composable
private fun FileOperationFailureMessage(state: MainStageModel.DialogState.FileOperationFailure): String {
    val detail = state.rawMessage
        ?.takeIf { it.isNotBlank() && it != MainStageModel.UNKNOWN_ERROR_TOKEN }
        ?: strings.commonUnknownError

    // TODO: Use more specific messages for different failure types, e.g. permission denied, target already exists, etc.
    return when (state.type) {
        MainStageModel.FileOperationType.Delete -> strings.dialogFileOperationFailureDelete.formatWithArgs(state.itemName, detail)
        MainStageModel.FileOperationType.Copy -> strings.dialogFileOperationFailureCopy.formatWithArgs(state.itemName, detail)
        MainStageModel.FileOperationType.Cut -> strings.dialogFileOperationFailureCut.formatWithArgs(state.itemName, detail)
    }
}

@Composable
private fun RenameErrorMessage(state: MainStageModel.DialogState.RenameError): String {
    val detail = state.rawMessage
        ?.takeIf { it.isNotBlank() && it != MainStageModel.UNKNOWN_ERROR_TOKEN }
        ?: strings.commonUnknownError

    return when (state.kind) {
        MainStageModel.DialogState.RenameErrorKind.AlreadyExists -> strings.dialogRenameErrorAlreadyExists
        MainStageModel.DialogState.RenameErrorKind.Generic -> strings.dialogRenameErrorGeneric.formatWithArgs(detail)
    }
}

@Composable
private fun DeviceActionMenuPopup(
    state: DeviceActionMenuState?,
    onPairNewDevice: () -> Unit,
    onConnectToDevice: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val popupState = state as? DeviceActionMenuState.Popup ?: return

    PopupMenu(
        onDismissRequest = { onDismissRequest(); true },
        popupPositionProvider = rememberPopupPositionProviderAtPosition(popupState.position),
        popupProperties = PopupProperties(focusable = false)
    ) {
        selectableItem(
            selected = false,
            onClick = onPairNewDevice
        ) { Text(strings.menuPairNewDevice) }
        selectableItem(
            selected = false,
            onClick = onConnectToDevice
        ) { Text(strings.menuConnectToDevice) }
    }
}

@Composable
private fun DeviceContextMenuPopup(
    state: DeviceContextMenuState?,
    onDisconnect: (AndroidDeviceItem) -> Unit,
    onDismissRequest: () -> Unit
) {
    val entryState = state as? DeviceContextMenuState.Entry ?: return

    PopupMenu(
        onDismissRequest = { onDismissRequest(); true },
        popupPositionProvider = rememberPopupPositionProviderAtPosition(entryState.position),
        popupProperties = PopupProperties(focusable = false)
    ) {
        selectableItem(
            selected = false,
            iconKey = AllIconsKeys.Actions.Cancel,
            onClick = {
                onDismissRequest()
                onDisconnect(entryState.device)
            }
        ) { Text(strings.menuDisconnect) }
    }
}

@Composable
private fun MainStatusBarText(viewModel: MainStageModel): String {
    val selectedCount = viewModel.selectedEntries.size
    val totalCount = viewModel.currentEntries.size
    val hiddenCount = viewModel.hiddenEntryCount
    val explicitStatus = StatusMessageText(viewModel.statusMessage)
    val hiddenSuffix = hiddenCount
        .takeIf { !viewModel.isShowingHiddenFiles && it > 0 }
        ?.let { count -> HiddenItemsText(count) }
        .orEmpty()

    return when {
        selectedCount > 0 -> SelectedItemsText(selectedCount, totalCount, hiddenSuffix)
        explicitStatus.isNotBlank() -> explicitStatus
        viewModel.selectedDevice != null -> ItemsText(totalCount, hiddenSuffix)
        else -> strings.mainStatusReady
    }
}

private sealed interface DeviceContextMenuState {

    data class Entry(
        val device: AndroidDeviceItem,
        val position: Offset,
        val requestId: Long
    ) : DeviceContextMenuState
}

private sealed interface DeviceActionMenuState {

    data class Popup(
        val position: Offset,
        val requestId: Long
    ) : DeviceActionMenuState
}

private class DevicePaneInteractionState {

    private var contextMenuRequestId by mutableStateOf(0L)
    private var actionMenuRequestId by mutableStateOf(0L)

    var contextMenuState by mutableStateOf<DeviceContextMenuState?>(null)
    var actionMenuState by mutableStateOf<DeviceActionMenuState?>(null)

    fun openContextMenu(device: AndroidDeviceItem, position: Offset) {
        actionMenuState = null
        contextMenuRequestId += 1L
        contextMenuState = DeviceContextMenuState.Entry(device, position, contextMenuRequestId)
    }

    fun dismissContextMenu() {
        contextMenuState = null
    }

    fun openActionMenu(position: Offset) {
        contextMenuState = null
        actionMenuRequestId += 1L
        actionMenuState = DeviceActionMenuState.Popup(position, actionMenuRequestId)
    }

    fun dismissActionMenu() {
        actionMenuState = null
    }
}

@Composable
private fun ItemsText(count: Int, suffix: String = ""): String {
    val suffixText = suffix.takeIf(String::isNotEmpty)?.let { " ($it)" } ?: ""

    return when (count) {
        1 -> strings.mainStatusItemSingular.formatWithArgs(suffixText)
        else -> strings.mainStatusItemPlural.formatWithArgs(count, suffixText)
    }
}

@Composable
private fun SelectedItemsText(selectedCount: Int, totalCount: Int, suffix: String = ""): String {
    val suffixText = suffix.takeIf(String::isNotEmpty)?.let { "${strings.comma}$it" } ?: ""

    return when (selectedCount) {
        1 -> strings.mainStatusSelectedItemSingular.formatWithArgs(totalCount, suffixText)
        else -> strings.mainStatusSelectedItemPlural.formatWithArgs(selectedCount, totalCount, suffixText)
    }
}

@Composable
private fun HiddenItemsText(count: Int) = when (count) {
    1 -> strings.mainStatusHiddenItemSingular
    else -> strings.mainStatusHiddenItemPlural.formatWithArgs(count)
}

@Composable
private fun FileListHintIcon(hint: MainStageModel.FileListHint) = when (hint) {
    MainStageModel.FileListHint.None -> null
    MainStageModel.FileListHint.EmptyFolder -> AppIcons.Folder
    MainStageModel.FileListHint.DeviceOffline,
    MainStageModel.FileListHint.DeviceUnauthorized,
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
    MainStageModel.FileListHint.DeviceUnauthorized -> strings.mainFileListHintDeviceUnauthorized
    MainStageModel.FileListHint.DeviceNotFound -> strings.mainFileListHintDeviceNotFound
    MainStageModel.FileListHint.PathNotFound -> strings.mainFileListHintPathNotFound
    MainStageModel.FileListHint.PermissionDenied -> strings.mainFileListHintPermissionDenied
    MainStageModel.FileListHint.LoadFailed -> strings.mainFileListHintLoadFailed
}

private sealed interface FileContextMenuState {
    data class Entry(val item: DeviceFileItem, val position: Offset, val requestId: Long) : FileContextMenuState
    data class Blank(val position: Offset, val requestId: Long) : FileContextMenuState
}

/**
 * Tracks transient pointer interaction state for one file area.
 *
 * This class centralizes a few non-obvious interaction rules that are hard to express directly in
 * child composable:
 * - clicking blank space after dismissing a context menu should not immediately clear selection
 * - drag selection must survive auto-scroll
 * - blank hit testing must work for both list rows and icon cards with custom hit regions
 */
private class FileAreaInteractionState {

    private var contextMenuRequestId by mutableStateOf(0L)
    private var consumeNextPrimaryPress by mutableStateOf(false)

    val selectionAreaState = SelectionAreaState<DeviceFileItem>()
    var contextMenuState by mutableStateOf<FileContextMenuState?>(null)

    fun openBlankContextMenu(position: Offset) {
        consumeNextPrimaryPress = false
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Blank(position, contextMenuRequestId)
    }

    fun openEntryContextMenu(entry: DeviceFileItem, position: Offset) {
        consumeNextPrimaryPress = false
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Entry(entry, position, contextMenuRequestId)
    }

    fun dismissContextMenu() {
        contextMenuState = null
        consumeNextPrimaryPress = false
    }

    fun dismissContextMenuConsumingNextPrimaryPress() {
        if (contextMenuState == null) {
            consumeNextPrimaryPress = false
            return
        }

        // The next primary press is consumed so closing a context menu does not also trigger
        // selection or inline-rename activation in the same gesture sequence.
        contextMenuState = null
        consumeNextPrimaryPress = true
    }

    fun dismissSelectionRect() {
        selectionAreaState.dismissSelectionRect()
    }

    private fun consumePendingPrimaryPress(): Boolean {
        if (!consumeNextPrimaryPress) return false

        consumeNextPrimaryPress = false
        return true
    }

    fun hasContextMenu() = contextMenuState != null

    /**
     * Normalizes the "first click after a context menu" rule for entry surfaces.
     *
     * Popup dismissal and the underlying item gesture can arrive in the same sequence, so rows,
     * icon cards, and rename labels all ask the interaction state whether this primary action
     * should still be honored.
     */
    fun prepareEntryPrimaryInteraction() = when {
        consumePendingPrimaryPress() -> false
        contextMenuState != null -> {
            dismissContextMenuConsumingNextPrimaryPress()
            false
        }
        else -> true
    }

    fun clearSelectionIfBlank(position: Offset, onBlankAreaPressed: () -> Unit) {
        if (contextMenuState != null) {
            contextMenuState = null
            return
        }

        if (selectionAreaState.isBlankArea(position))
            onBlankAreaPressed()
    }

    private fun buildEntryPath(entry: DeviceFileItem) =
        if (entry.path == "/") "/${entry.name}"
        else "${entry.path.trimEnd('/')}/${entry.name}"

    fun entryPathOf(entry: DeviceFileItem) = buildEntryPath(entry)
}

@Composable
private fun rememberFileAreaInteractionState(device: AndroidDeviceItem) = remember(device) {
    FileAreaInteractionState()
}

private fun Modifier.fileAreaBlankSelection(
    interactionState: FileAreaInteractionState,
    showSelectionRect: Boolean,
    selectedPathsProvider: () -> Set<String>,
    onClearSelection: () -> Unit,
    onSelectionChanged: (candidatePaths: Set<String>, additive: Boolean, initialSelectionPaths: Set<String>) -> Unit,
    autoScrollBy: (suspend (Float) -> Unit)? = null
) = blankAreaDragSelection(
    selectionState = interactionState.selectionAreaState,
    showSelectionRect = showSelectionRect,
    selectedKeysProvider = selectedPathsProvider,
    onClearSelection = onClearSelection,
    onSelectionChanged = onSelectionChanged,
    keyOfItem = interactionState::entryPathOf,
    prepareBlankGesture = {
        if (interactionState.prepareEntryPrimaryInteraction()) {
            interactionState.dismissContextMenu()
            true
        } else false
    },
    autoScrollBy = autoScrollBy
)

private fun handleFileAreaShortcut(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    event: KeyEvent,
    newFolderBaseName: String,
    onBeginInlineRename: () -> Unit = {},
    onNavigateSelection: (MainStageModel.NavigationDirection) -> Boolean = { false },
    onNavigateByInitialChar: (Char) -> Boolean = { false }
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    // Keep this routing close to the file area instead of a global window handler so shortcuts
    // only trigger when the file area actually owns focus.
    return when {
        primaryShortcutMatches(event, Key.A) && !event.isShiftPressed -> {
            viewModel.selectAllEntries()
            true
        }
        event.key == Key.A && event.isShiftPressed -> {
            viewModel.inverseSelectEntries()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Copy).matches(event) && viewModel.hasSelectedEntry -> {
            viewModel.copySelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Cut).matches(event) && viewModel.hasSelectedEntry -> {
            viewModel.cutSelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.NewFolder).matches(event) &&
            viewModel.canShowBlankFileContextMenu(device) -> {
            viewModel.createNewFolder(newFolderBaseName)
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Paste).matches(event) &&
            viewModel.canShowBlankFileContextMenu(device) &&
            viewModel.canPasteEntry -> {
            viewModel.pasteToCurrentPath()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Delete).matches(event) && viewModel.hasSelectedEntry -> {
            viewModel.deleteSelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Rename).matches(event) && viewModel.hasSingleSelectedEntry -> {
            onBeginInlineRename()
            viewModel.renameSelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Open).matches(event) && viewModel.hasSingleSelectedEntry -> {
            viewModel.openSelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Properties).matches(event) && viewModel.canShowFileProperties -> {
            viewModel.showProperties()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Open).keyCode == Key.Enter.keyCode &&
            (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
            viewModel.hasSingleSelectedEntry -> {
            viewModel.openSelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Rename).keyCode == Key.Enter.keyCode &&
            event.key == Key.NumPadEnter &&
            viewModel.hasSingleSelectedEntry -> {
            onBeginInlineRename()
            viewModel.renameSelectedEntry()
            true
        }
        viewModel.menuShortcut(MenuShortcut.Action.Refresh).matches(event) &&
            viewModel.canRefresh -> {
            viewModel.refreshEntries()
            true
        }
        event.key == Key.Escape && viewModel.selectedEntriesOf(device).isNotEmpty() -> {
            viewModel.clearSelectedEntries(device)
            true
        }
        event.key == Key.DirectionUp -> onNavigateSelection(MainStageModel.NavigationDirection.Up)
        event.key == Key.DirectionDown -> onNavigateSelection(MainStageModel.NavigationDirection.Down)
        event.key == Key.DirectionLeft -> onNavigateSelection(MainStageModel.NavigationDirection.Left)
        event.key == Key.DirectionRight -> onNavigateSelection(MainStageModel.NavigationDirection.Right)
        else -> extractInitialNavigationChar(event)?.let(onNavigateByInitialChar) == true
    }
}

private fun extractInitialNavigationChar(event: KeyEvent): Char? {
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return null

    val codePoint = event.utf16CodePoint
    if (codePoint <= 0) return null

    val keyChar = codePoint.toChar()
    if (keyChar.isISOControl() || keyChar.isWhitespace())
        return null

    return keyChar.lowercaseChar()
}

private fun primaryShortcutMatches(event: KeyEvent, key: Key) = event.key == key &&
    event.type == KeyEventType.KeyDown &&
    ((OsType.isMacOS && event.isMetaPressed && !event.isCtrlPressed) ||
        (!OsType.isMacOS && event.isCtrlPressed && !event.isMetaPressed)) &&
    !event.isAltPressed

private fun selectedEntryIndex(
    entries: List<DeviceFileItem>,
    selectedEntry: DeviceFileItem?
) = selectedEntry?.let(entries::indexOf) ?: -1

private fun findEntryIndexByInitialChar(entries: List<DeviceFileItem>, char: Char) =
    entries.indexOfFirst { entry ->
        entry.name.startsWith(char.toString(), ignoreCase = true)
    }

private data class EntryPositionRequest(
    val index: Int,
    val forceScroll: Boolean,
    val preferSingleStepReveal: Boolean,
    val requestId: Long
)

/**
 * Small request queue for cross-view item positioning.
 *
 * A monotonically increasing request id prevents a previous request from clearing a newer one
 * when multiple selection/scroll updates happen in quick succession.
 */
private class EntryPositionController {

    private var nextRequestId = 0L

    var currentRequest by mutableStateOf<EntryPositionRequest?>(null)
        private set

    fun request(index: Int, forceScroll: Boolean, preferSingleStepReveal: Boolean = false) {
        if (index < 0) return

        nextRequestId += 1L
        currentRequest = EntryPositionRequest(
            index = index,
            forceScroll = forceScroll,
            preferSingleStepReveal = preferSingleStepReveal,
            requestId = nextRequestId
        )
    }

    fun consume(request: EntryPositionRequest) {
        if (currentRequest?.requestId == request.requestId)
            currentRequest = null
    }
}

private val FirstPaneMinWidth = 240.dp
private val SecondPaneMinWidth = 560.dp
private val PaneDividerWidth = 10.dp

private val DefaultFileItemMinWidth = 105.dp
private val DefaultFileItemOuterPadding = 8.dp