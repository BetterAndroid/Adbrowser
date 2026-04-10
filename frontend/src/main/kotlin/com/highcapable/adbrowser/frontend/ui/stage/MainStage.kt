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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
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
import com.highcapable.adbrowser.frontend.ui.geometry.intersects
import com.highcapable.adbrowser.frontend.ui.geometry.normalizedRect
import com.highcapable.adbrowser.frontend.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.utils.extension.formatWithArgs
import com.highcapable.adbrowser.frontend.ui.vm.MainStageModel
import com.highcapable.adbrowser.frontend.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.shared.utils.BuildVersion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val focusRequester = remember(device) { FocusRequester() }
    val entryPositionController = remember(device) { EntryPositionController() }
    val entries = viewModel.entriesOf(device)
    val selectedEntry = viewModel.selectedEntryOf(device)

    LaunchedEffect(device, viewModel.selectedViewMode) {
        val selectedIndex = selectedEntryIndex(entries, selectedEntry)
        if (selectedIndex >= 0)
            entryPositionController.request(index = selectedIndex, forceScroll = false)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (viewModel.isListViewMode)
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

    val entries = viewModel.entriesOf(device)
    val canShowBlankContextMenu = viewModel.canShowBlankFileContextMenu(device)

    LaunchedEffect(entryPositionController.currentRequest, entries) {
        val request = entryPositionController.currentRequest ?: return@LaunchedEffect
        if (request.index !in entries.indices) {
            entryPositionController.consume(request)
            return@LaunchedEffect
        }

        repeat(2) { withFrameNanos { } }
        if (request.forceScroll || !listState.isIndexVisible(request.index))
            listState.scrollToItem(request.index)
        entryPositionController.consume(request)
    }
    LaunchedEffect(device, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        interactionState.dismissContextMenu()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                handleFileAreaShortcut(viewModel, device, it) { navigateChar ->
                    val targetIndex = findEntryIndexByInitialChar(entries, navigateChar)
                    if (targetIndex < 0) return@handleFileAreaShortcut false

                    val targetEntry = entries[targetIndex]
                    viewModel.setSelectedEntry(device, targetEntry)
                    entryPositionController.request(index = targetIndex, forceScroll = true)
                    true
                }
            }
            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
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
                .nestedScroll(interactionState.nestedScrollConnection)
                .onGloballyPositioned { interactionState.contentCoordinates = it }
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
                        interactionState.cumulativeScrollY += consumed
                    }
                )
                .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                    if (canShowBlankContextMenu)
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
                        selected = viewModel.isEntrySelected(device, entry),
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
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            interactionState.visibleEntryBounds[entry] = kotlin.collections.listOf(bounds)
                        },
                        overlay = {
                            FileEntryContextMenuPopup(
                                viewModel = viewModel,
                                device = device,
                                item = entry,
                                state = interactionState.contextMenuState,
                                onDismissRequest = interactionState::dismissContextMenu,
                                onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextBlankPress
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
                onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextBlankPress
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

    val entries = viewModel.entriesOf(device)
    val canShowBlankContextMenu = viewModel.canShowBlankFileContextMenu(device)

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

        repeat(4) { withFrameNanos {} }
        if (request.forceScroll || !gridState.isIndexVisible(request.index))
            gridState.scrollToItem(request.index)
        entryPositionController.consume(request)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                handleFileAreaShortcut(viewModel, device, it) { navigateChar ->
                    val targetIndex = findEntryIndexByInitialChar(entries, navigateChar)
                    if (targetIndex < 0) return@handleFileAreaShortcut false

                    val targetEntry = entries[targetIndex]
                    viewModel.setSelectedEntry(device, targetEntry)
                    entryPositionController.request(index = targetIndex, forceScroll = true)
                    true
                }
            }
            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
                focusRequester.requestFocus()
            }
            .nestedScroll(interactionState.nestedScrollConnection)
            .onGloballyPositioned { interactionState.contentCoordinates = it }
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
                    interactionState.cumulativeScrollY += consumed
                }
            )
            .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                if (canShowBlankContextMenu)
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
                    onDispose { interactionState.visibleEntryBounds.remove(entry) }
                }
                FileIconItem(
                    item = entry,
                    selected = viewModel.isEntrySelected(device, entry),
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
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = DefaultFileItemOuterPadding),
                    onHitBoundsChanged = { bounds ->
                        interactionState.visibleEntryBounds[entry] = bounds
                    },
                    overlay = {
                        FileEntryContextMenuPopup(
                            viewModel = viewModel,
                            device = device,
                            item = entry,
                            state = interactionState.contextMenuState,
                            onDismissRequest = interactionState::dismissContextMenu,
                            onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextBlankPress
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
        interactionState.selectionRect?.let { rect ->
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
            onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextBlankPress
        )
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
    if (!viewModel.canShowBlankFileContextMenu(device)) return

    val blankState = state as? FileContextMenuState.Blank ?: return

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
    }
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
    if (!isMultiSelection) {
        separator()
        selectableItem(
            selected = false,
            iconKey = AllIconsKeys.Actions.Properties,
            onClick = { perform(viewModel::showSelectedEntryProperties) }
        ) { Text(strings.menuProperties) }
    }
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
                message = if (state.entryCount > 1)
                    strings.dialogDeleteConfirmMultiple.formatWithArgs(state.entryCount)
                else strings.dialogDeleteConfirm,
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
            MainStageModel.StatusMessage.Key.EntryDeletedMultiple -> strings.statusEntryDeletedMultiple
            MainStageModel.StatusMessage.Key.Copied -> strings.statusCopied
            MainStageModel.StatusMessage.Key.CopiedMultiple -> strings.statusCopiedMultiple
            MainStageModel.StatusMessage.Key.Cut -> strings.statusCut
            MainStageModel.StatusMessage.Key.CutMultiple -> strings.statusCutMultiple
            MainStageModel.StatusMessage.Key.ClipboardEmpty -> strings.statusClipboardEmpty
            MainStageModel.StatusMessage.Key.CrossDevicePasteNotSupported -> strings.statusCrossDevicePasteNotSupported
            MainStageModel.StatusMessage.Key.Pasted -> strings.statusPasted
            MainStageModel.StatusMessage.Key.PastedMultiple -> strings.statusPastedMultiple
            MainStageModel.StatusMessage.Key.DialogPropertiesInvalidPermission -> strings.dialogPropertiesInvalidPermission
            MainStageModel.StatusMessage.Key.DialogPropertiesPermissionUpdated -> strings.dialogPropertiesPermissionUpdated
        }
        template.formatWithArgs(*status.args.toTypedArray())
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
    private var consumeNextBlankPrimaryPress by mutableStateOf(false)

    var contextMenuState by mutableStateOf<FileContextMenuState?>(null)
    var selectionRect by mutableStateOf<Rect?>(null)
    val visibleEntryBounds = mutableStateMapOf<DeviceFileItem, List<Rect>>()
    var contentCoordinates by mutableStateOf<LayoutCoordinates?>(null)

    var cumulativeScrollY = 0f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            cumulativeScrollY -= consumed.y
            return Offset.Zero
        }
    }

    fun openBlankContextMenu(position: Offset) {
        consumeNextBlankPrimaryPress = false
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Blank(position, contextMenuRequestId)
    }

    fun openEntryContextMenu(entry: DeviceFileItem, position: Offset) {
        consumeNextBlankPrimaryPress = false
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Entry(entry, position, contextMenuRequestId)
    }

    fun dismissContextMenu() {
        contextMenuState = null
    }

    fun dismissContextMenuConsumingNextBlankPress() {
        contextMenuState = null
        consumeNextBlankPrimaryPress = true
    }

    fun dismissSelectionRect() {
        selectionRect = null
    }

    fun consumePendingBlankPrimaryPress(): Boolean {
        if (!consumeNextBlankPrimaryPress) return false

        consumeNextBlankPrimaryPress = false
        return true
    }

    fun hasContextMenu() = contextMenuState != null

    fun clearSelectionIfBlank(position: Offset, onBlankAreaPressed: () -> Unit) {
        if (contextMenuState != null) {
            contextMenuState = null
            return
        }

        val rootPosition = contentCoordinates?.localToRoot(position) ?: position

        if (visibleEntryBounds.values.none { regions -> regions.any { it.contains(rootPosition) } })
            onBlankAreaPressed()
    }

    fun isBlankArea(position: Offset): Boolean {
        val rootPosition = contentCoordinates?.localToRoot(position) ?: position
        return visibleEntryBounds.values.none { regions -> regions.any { it.contains(rootPosition) } }
    }

    private fun viewportRectInRoot(): Rect? {
        val coords = contentCoordinates ?: return null
        return normalizedRect(
            coords.localToRoot(Offset.Zero),
            coords.localToRoot(Offset(coords.size.width.toFloat(), coords.size.height.toFloat()))
        )
    }

    private fun buildEntryPath(entry: DeviceFileItem) =
        if (entry.path == "/") "/${entry.name}"
        else "${entry.path.trimEnd('/')}/${entry.name}"

    fun entryPathsIntersecting(localRect: Rect): Set<String> {
        val coords = contentCoordinates ?: return emptySet()
        val rootRect = normalizedRect(
            coords.localToRoot(localRect.topLeft),
            coords.localToRoot(localRect.bottomRight)
        )
        val viewport = viewportRectInRoot() ?: return emptySet()

        return visibleEntryBounds
            .filterValues { regions ->
                regions.any { it.intersects(viewport) } && regions.any { it.intersects(rootRect) }
            }
            .keys.mapTo(linkedSetOf()) { buildEntryPath(it) }
    }

    fun viewportEntryPaths(): Set<String> {
        val viewport = viewportRectInRoot() ?: return emptySet()
        return visibleEntryBounds
            .filterValues { regions -> regions.any { it.intersects(viewport) } }
            .keys.mapTo(linkedSetOf()) { buildEntryPath(it) }
    }
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
) = pointerInput(interactionState, showSelectionRect) {
    val inputScope = this

    // Shared drag state between gesture handler and auto-scroll coroutine
    // Thread-safe: both coroutines run on the single-threaded UI dispatcher
    val drag = object {
        var startX = 0f
        var startY = 0f
        var currentX = 0f
        var currentY = 0f
        var startScrollY = 0f
        var dragging = false
        var active = false
        var additive = false
        var initialSelectionPaths: Set<String> = emptySet()
        var accumulatedPaths = linkedSetOf<String>()
    }

    fun performSelectionUpdate() {
        val scrollDelta = interactionState.cumulativeScrollY - drag.startScrollY
        val adjustedStart = Offset(drag.startX, drag.startY - scrollDelta)
        val current = Offset(drag.currentX, drag.currentY)
        val dragRect = normalizedRect(adjustedStart, current)
        val currentIntersecting = interactionState.entryPathsIntersecting(dragRect)
        val viewportPaths = interactionState.viewportEntryPaths()

        interactionState.selectionRect = if (showSelectionRect) dragRect else null
        drag.accumulatedPaths = ((drag.accumulatedPaths - viewportPaths) + currentIntersecting).toCollection(linkedSetOf())

        onSelectionChanged(drag.accumulatedPaths, drag.additive, drag.initialSelectionPaths)
    }

    coroutineScope {
        // Auto-scroll and scroll-change observer coroutine
        launch {
            var lastObservedScrollY = interactionState.cumulativeScrollY
            while (isActive) {
                delay(16)

                if (!drag.active || !drag.dragging) {
                    lastObservedScrollY = interactionState.cumulativeScrollY
                    continue
                }

                // Auto-scroll when cursor moves outside the content area
                if (autoScrollBy != null) {
                    val y = drag.currentY
                    val height = inputScope.size.height.toFloat()
                    val maxScrollSpeed = 18f
                    val maxOvershoot = 150f
                    val scrollAmount = when {
                        y < 0f -> -maxScrollSpeed * (minOf(-y, maxOvershoot) / maxOvershoot)
                        y > height -> maxScrollSpeed * (minOf(y - height, maxOvershoot) / maxOvershoot)
                        else -> 0f
                    }

                    if (scrollAmount != 0f) autoScrollBy(scrollAmount)
                }

                // Recompute selection when scroll offset changed (from mouse wheel or auto-scroll)
                val currentScrollY = interactionState.cumulativeScrollY
                if (currentScrollY != lastObservedScrollY) {
                    lastObservedScrollY = currentScrollY
                    performSelectionUpdate()
                }
            }
        }

        // Main gesture processing loop
        inputScope.awaitEachGesture {
            var downPosition: Offset? = null
            drag.additive = false

            while (downPosition == null) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (!event.buttons.isPrimaryPressed) continue

                val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue
                downPosition = change.position
                drag.additive = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
            }

            val start = downPosition
            if (!interactionState.isBlankArea(start)) {
                interactionState.dismissSelectionRect()
                return@awaitEachGesture
            }

            if (interactionState.consumePendingBlankPrimaryPress()) {
                interactionState.dismissSelectionRect()
                return@awaitEachGesture
            }

            if (interactionState.hasContextMenu()) {
                interactionState.dismissContextMenu()
                interactionState.dismissSelectionRect()
                return@awaitEachGesture
            }

            interactionState.dismissContextMenu()
            interactionState.dismissSelectionRect()

            if (!drag.additive) onClearSelection()

            drag.initialSelectionPaths = selectedPathsProvider()
            drag.startX = start.x
            drag.startY = start.y
            drag.currentX = start.x
            drag.currentY = start.y
            drag.startScrollY = interactionState.cumulativeScrollY
            drag.accumulatedPaths = linkedSetOf()
            drag.dragging = false
            drag.active = true

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val primaryChange = event.changes.firstOrNull() ?: continue
                val current = primaryChange.position

                if (primaryChange.pressed) {
                    val scrollDelta = interactionState.cumulativeScrollY - drag.startScrollY
                    val adjustedStart = Offset(start.x, start.y - scrollDelta)

                    drag.currentX = current.x
                    drag.currentY = current.y

                    val dragRect = normalizedRect(adjustedStart, current)
                    if (!drag.dragging && maxOf(dragRect.width, dragRect.height) >= 4f) drag.dragging = true
                    if (drag.dragging) performSelectionUpdate()
                    continue
                }

                drag.active = false
                interactionState.dismissSelectionRect()
                break
            }
        }
    }
}

private fun handleFileAreaShortcut(
    viewModel: MainStageModel,
    device: AndroidDeviceItem,
    event: KeyEvent,
    onNavigateByInitialChar: (Char) -> Boolean = { false }
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    val isPrimaryShortcutPressed = event.isCtrlPressed || event.isMetaPressed
    return when {
        isPrimaryShortcutPressed && event.key == Key.A && !event.isShiftPressed -> {
            viewModel.selectAllEntries()
            true
        }
        event.key == Key.A && event.isShiftPressed -> {
            viewModel.inverseSelectEntries()
            true
        }
        isPrimaryShortcutPressed && event.key == Key.C && viewModel.hasSelectedEntry -> {
            viewModel.copySelectedEntry()
            true
        }
        isPrimaryShortcutPressed && event.key == Key.X && viewModel.hasSelectedEntry -> {
            viewModel.cutSelectedEntry()
            true
        }
        isPrimaryShortcutPressed &&
            event.key == Key.V &&
            viewModel.canShowBlankFileContextMenu(device) &&
            viewModel.canPasteEntry -> {
            viewModel.pasteToCurrentPath()
            true
        }
        event.key == Key.Delete && viewModel.hasSelectedEntry -> {
            viewModel.deleteSelectedEntry()
            true
        }
        event.key == Key.F2 && viewModel.hasSingleSelectedEntry -> {
            viewModel.renameSelectedEntry()
            true
        }
        (event.key == Key.Enter || event.key == Key.NumPadEnter) && viewModel.hasSingleSelectedEntry -> {
            viewModel.openSelectedEntry()
            true
        }
        event.key == Key.Escape && viewModel.selectedEntriesOf(device).isNotEmpty() -> {
            viewModel.clearSelectedEntries(device)
            true
        }
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

private fun findEntryIndexByInitialChar(entries: List<DeviceFileItem>, char: Char): Int =
    entries.indexOfFirst { entry ->
        entry.name.startsWith(char.toString(), ignoreCase = true)
    }

private fun selectedEntryIndex(
    entries: List<DeviceFileItem>,
    selectedEntry: DeviceFileItem?
): Int = selectedEntry?.let(entries::indexOf) ?: -1

private data class EntryPositionRequest(
    val index: Int,
    val forceScroll: Boolean,
    val requestId: Long
)

private class EntryPositionController {

    private var nextRequestId = 0L

    var currentRequest by mutableStateOf<EntryPositionRequest?>(null)
        private set

    fun request(index: Int, forceScroll: Boolean) {
        if (index < 0) return
        nextRequestId += 1L
        currentRequest = EntryPositionRequest(
            index = index,
            forceScroll = forceScroll,
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