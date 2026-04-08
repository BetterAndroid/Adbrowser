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
@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.frontend.ui.stage

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import androidx.compose.ui.zIndex
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.frontend.ui.assets.AppIcons
import com.highcapable.adbrowser.frontend.ui.component.ContentIcon
import com.highcapable.adbrowser.frontend.ui.component.ContentIconButton
import com.highcapable.adbrowser.frontend.ui.component.FixedWidthHorizontalSplitLayout
import com.highcapable.adbrowser.frontend.ui.dialog.ConfirmDialog
import com.highcapable.adbrowser.frontend.ui.dialog.FilePropertiesDialog
import com.highcapable.adbrowser.frontend.ui.dialog.SimpleInputDialog
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserColorScheme
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.MainStageModel
import com.highcapable.adbrowser.frontend.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.frontend.ui.vm.model.PathBreadcrumbSegment
import com.highcapable.adbrowser.frontend.ui.window.manager.AppWindow
import com.highcapable.adbrowser.frontend.ui.window.manager.LocalWindowManager
import com.highcapable.adbrowser.shared.utils.BuildVersion
import com.highcapable.adbrowser.shared.utils.OsType
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.CopyMenuItemOptionAction
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.CutMenuItemOptionAction
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.PasteMenuItemOptionAction
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.SelectAllMenuItemOptionAction
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icon.IconKey

@Composable
fun FrameWindowScope.MainMenuBar(
    viewModel: MainStageModel,
    onCloseRequest: () -> Unit
) {
    val windowManager = LocalWindowManager.current
    val toggleStatusBarText = if (viewModel.isStatusBarVisible)
        strings.menuHideStatusBar
    else strings.menuShowStatusBar
    val openPreferences = { windowManager.open(AppWindow.Preferences) }

    MenuBar {
        Menu(strings.menuFile) {
            Item(
                text = strings.menuOpen,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::openSelectedEntry
            )
            Item(
                text = strings.menuOpenWith,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::openSelectedEntryWith
            )
            Separator()
            Item(
                text = strings.menuNewFolder,
                onClick = viewModel::createNewFolder,
                shortcut = createShortcut(Key.N)
            )
            Separator()
            Item(
                text = strings.menuRename,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::renameSelectedEntry,
                shortcut = createShortcut(Key.F2)
            )
            Item(
                text = strings.menuDelete,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::deleteSelectedEntry,
                shortcut = createShortcut(Key.Delete)
            )
            Item(
                text = strings.menuProperties,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::showSelectedEntryProperties,
                shortcut = createShortcut(Key.I)
            )
            if (!OsType.isMacOS) {
                Separator()
                Item(
                    text = strings.menuPreferences,
                    onClick = openPreferences,
                    shortcut = createShortcut(Key.Comma)
                )
                Separator()
                Item(
                    text = strings.menuExit,
                    onClick = onCloseRequest,
                    shortcut = createShortcut(Key.Q)
                )
            }
        }
        Menu(strings.menuEdit) {
            Item(
                text = strings.menuCut,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::cutSelectedEntry,
                shortcut = createShortcut(Key.X)
            )
            Item(
                text = strings.menuCopy,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::copySelectedEntry,
                shortcut = createShortcut(Key.C)
            )
            Item(
                text = strings.menuPaste,
                enabled = viewModel.canPasteEntry,
                onClick = viewModel::pasteToCurrentPath,
                shortcut = createShortcut(Key.V)
            )
            Separator()
            Item(strings.menuSelectAll,
                onClick = viewModel::selectAllEntries,
                shortcut = createShortcut(Key.A)
            )
            Item(
                strings.menuInverseSelect,
                onClick = viewModel::inverseSelectEntries,
                shortcut = createShortcut(Key.A, shift = true)
            )
        }
        Menu(strings.menuView) {
            Item(
                text = strings.menuRefresh,
                onClick = viewModel::refreshEntries,
                shortcut = createShortcut(Key.R))
            Item(
                text = strings.menuViewMode,
                onClick = viewModel::openViewModeMenu,
                shortcut = createShortcut(Key.M))
            Item(
                text = strings.menuSortMode,
                onClick = viewModel::openSortModeMenu,
                shortcut = createShortcut(Key.O)
            )
            Separator()
            Item(
                text = toggleStatusBarText,
                onClick = viewModel::toggleStatusBar,
                shortcut = createShortcut(Key.B)
            )
            Item(
                text = strings.menuAppLogs,
                onClick = {
                    windowManager.open(AppWindow.LogViewer)
                },
                shortcut = createShortcut(Key.L)
            )
        }
        Menu(strings.menuGo) {
            Item(
                text = strings.menuForward,
                enabled = viewModel.canNavigateForward,
                onClick = viewModel::navigateForward,
                shortcut = createShortcut(Key.DirectionRight, alt = true)
            )
            Item(
                text = strings.menuBack,
                enabled = viewModel.canNavigateBack,
                onClick = viewModel::navigateBack,
                shortcut = createShortcut(Key.DirectionLeft, alt = true)
            )
            Separator()
            Item(
                text = strings.menuUp,
                enabled = viewModel.canNavigateUp,
                onClick = viewModel::navigateUp,
                shortcut = createShortcut(Key.DirectionUp, alt = true)
            )
            Item(
                text = strings.menuRoot,
                enabled = viewModel.canNavigateRoot,
                onClick = viewModel::navigateRoot,
                shortcut = createShortcut(Key.R, shift = true)
            )
            Item(
                text = strings.menuHome,
                onClick = viewModel::navigateHome,
                shortcut = createShortcut(Key.H, shift = true)
            )
        }
        Menu(strings.menuHelp) {
            Item(
                text = strings.menuComingSoon,
                enabled = false,
                onClick = {}
            )
        }
    }
}

private fun createShortcut(
    key: Key,
    alt: Boolean = false,
    shift: Boolean = false
) = KeyShortcut(
    key = key,
    ctrl = !OsType.isMacOS,
    meta = OsType.isMacOS,
    alt = alt,
    shift = shift
)

@Composable
fun FrameWindowScope.MainStage(
    viewModel: MainStageModel,
    onCloseRequest: () -> Unit,
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

        if (viewModel.isStatusBarVisible)
            StatusBar(viewModel = viewModel)
    }

    MainStageDialogs(viewModel)
}

@Composable
private fun DevicePane(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .border(1.dp, colors.panelBorder, RoundedCornerShape(8.dp))
            .background(colors.panelBackground, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.mainDevicesTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (viewModel.devices.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(${viewModel.devices.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ContentIconButton(
                key = AppIcons.Refresh,
                outlined = true,
                contentDescription = strings.mainRefreshDeviceDescription,
                onClick = viewModel::refreshDevices
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, colors.panelBorder, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(viewModel.devices) { device ->
                    DeviceRow(
                        item = device,
                        selected = viewModel.selectedDevice == device,
                        onClick = { viewModel.selectDevice(device) }
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
            if (viewModel.devices.isEmpty())
                DeviceListHint(
                    message = strings.mainDeviceListHintNoDevice,
                    modifier = Modifier.align(Alignment.Center)
                )
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun DeviceRow(
    item: AndroidDeviceItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AdbrowserTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )
    val foreground = if (selected) Color.White else Color.Unspecified
    val statusColor = if (item.isOnline) OnlineStatusColor else OfflineStatusColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .hoverable(interactionSource = interactionSource)
            .pointerInput(item.serial) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onClick()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(8.dp))
            ContentIcon(
                key = AppIcons.Device,
                selected = selected,
                contentDescription = "Device Icon",
                tint = colors.primaryAccent
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.brandModel,
                color = foreground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.alpha(0.75f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.systemVersion.isNotBlank()) {
                    Text(
                        text = item.systemVersion,
                        color = foreground,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "|",
                        color = foreground,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .alpha(0.75f)
                            .padding(horizontal = 3.dp)
                    )
                }
                Text(
                    text = item.serial,
                    color = foreground,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FilePaneHost(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val selectedSerial = viewModel.selectedDevice?.serial

    Box(modifier = modifier) {
        viewModel.deviceWorkspaces.forEach { workspace ->
            key(workspace.serial) {
                val isSelected = workspace.serial == selectedSerial
                FilePane(
                    viewModel = viewModel,
                    serial = workspace.serial,
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
    serial: String,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier
            .border(1.dp, colors.panelBorder, RoundedCornerShape(8.dp))
            .background(colors.panelBackground, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        NavigationBar(viewModel = viewModel, serial = serial)
        Spacer(Modifier.height(12.dp))
        FilePaneContent(viewModel = viewModel, serial = serial, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FilePaneContent(
    viewModel: MainStageModel,
    serial: String,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.panelBorder, shape)
            .clip(shape)
    ) {
        FileListArea(
            viewModel = viewModel,
            serial = serial,
            modifier = Modifier.weight(1f)
        )
        PathBreadcrumbBar(viewModel = viewModel, serial = serial)
    }
}

@Composable
private fun NavigationBar(viewModel: MainStageModel, serial: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContentIconButton(
            key = AppIcons.ArrowLeft,
            contentDescription = strings.menuBack,
            enabled = viewModel.canNavigateBack(serial),
            outlined = true,
            onClick = { viewModel.navigateBack(serial) }
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.ArrowRight,
            contentDescription = strings.menuForward,
            enabled = viewModel.canNavigateForward(serial),
            outlined = true,
            onClick = { viewModel.navigateForward(serial) }
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.ArrowUp,
            contentDescription = strings.menuUp,
            enabled = viewModel.canNavigateUp(serial),
            outlined = true,
            onClick = { viewModel.navigateUp(serial) }
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.Home,
            contentDescription = strings.menuHome,
            outlined = true,
            onClick = { viewModel.navigateHome(serial) }
        )
        Spacer(Modifier.width(6.dp))
        TextField(
            state = viewModel.pathInputOf(serial),
            modifier = Modifier
                .weight(1f)
                .height(AdbrowserTheme.DefaultTextFieldHeight)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                        viewModel.openPathFromInput(serial)
                        true
                    } else false
                }
        )
        Spacer(Modifier.width(6.dp))
        ViewModeDropdown(viewModel = viewModel)
        Spacer(Modifier.width(6.dp))
        SortModeDropdown(viewModel = viewModel)
    }
}

@Composable
private fun ViewModeDropdown(viewModel: MainStageModel, modifier: Modifier = Modifier) {
    val selected = viewModel.selectedViewMode
    Dropdown(
        modifier = modifier
            .width(110.dp)
            .height(AdbrowserTheme.DefaultTextFieldHeight),
        menuContent = {
            viewModel.viewModes.forEach { option ->
                selectableItem(
                    selected = option == selected,
                    onClick = { viewModel.onViewModeSelected(option) }
                ) { Text(viewModeLabel(option.key)) }
            }
        }
    ) {
        Text(viewModeLabel(selected?.key))
    }
}

@Composable
private fun SortModeDropdown(viewModel: MainStageModel, modifier: Modifier = Modifier) {
    val selected = viewModel.selectedSortMode
    Dropdown(
        modifier = modifier
            .width(130.dp)
            .height(AdbrowserTheme.DefaultTextFieldHeight),
        menuContent = {
            viewModel.sortModes.forEach { option ->
                selectableItem(
                    selected = option == selected,
                    onClick = { viewModel.onSortModeSelected(option) }
                ) { Text(sortModeLabel(option.key)) }
            }
        }
    ) {
        Text(sortModeLabel(selected?.key))
    }
}

@Composable
private fun FileListArea(
    viewModel: MainStageModel,
    serial: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (viewModel.isListViewMode)
            FileListView(viewModel = viewModel, serial = serial)
        else FileIconView(viewModel = viewModel, serial = serial)

        val hint = viewModel.fileListHintOf(serial)
        val hintIcon = fileListHintIcon(hint)
        val hintMessage = fileListHintMessage(hint)

        if (hintIcon != null && hintMessage.isNotBlank())
            FileListHint(
                iconKey = hintIcon,
                message = hintMessage,
                modifier = Modifier.align(Alignment.Center)
            )
    }
}

private sealed interface FileContextMenuState {
    data class Entry(val item: DeviceFileItem, val position: Offset, val requestId: Long) : FileContextMenuState
    data class Blank(val position: Offset, val requestId: Long) : FileContextMenuState
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun FileListView(viewModel: MainStageModel, serial: String) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.listScrollIndexOf(serial),
        initialFirstVisibleItemScrollOffset = viewModel.listScrollOffsetOf(serial)
    )
    val horizontalScrollState = rememberScrollState(viewModel.listHorizontalScrollOffsetOf(serial))
    val directoryChangeVersion = viewModel.directoryChangeVersionOf(serial)
    var handledDirectoryChangeVersion by remember(serial) { mutableStateOf(directoryChangeVersion) }
    val entries = viewModel.entriesOf(serial)
    val selectedEntry = viewModel.selectedEntryOf(serial)
    var contextMenuState by remember(serial) { mutableStateOf<FileContextMenuState?>(null) }
    var contextMenuRequestId by remember(serial) { mutableStateOf(0L) }

    fun openBlankContextMenu(position: Offset) {
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Blank(position, contextMenuRequestId)
    }

    fun openEntryContextMenu(entry: DeviceFileItem, position: Offset) {
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Entry(entry, position, contextMenuRequestId)
    }

    LaunchedEffect(serial) {
        val selectedIndex = selectedEntry?.let { entries.indexOf(it) } ?: -1
        if (selectedIndex < 0) return@LaunchedEffect

        repeat(2) { withFrameNanos { } }
        if (!isLazyListIndexVisible(listState, selectedIndex))
            listState.scrollToItem(selectedIndex)
    }

    LaunchedEffect(serial, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        contextMenuState = null
        listState.scrollToItem(0)
        horizontalScrollState.scrollTo(0)
    }

    LaunchedEffect(serial, listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.updateListScrollState(serial, index, offset)
            }
    }

    LaunchedEffect(serial, horizontalScrollState) {
        snapshotFlow { horizontalScrollState.value }
            .distinctUntilChanged()
            .collect { offset ->
                viewModel.updateListHorizontalScrollState(serial, offset)
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FileListHeader(
            viewModel = viewModel,
            horizontalScrollState = horizontalScrollState
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .onBlankPrimaryPress {
                    contextMenuState = null
                    viewModel.setSelectedEntry(serial, null)
                }
                .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                    openBlankContextMenu(position)
                }
        ) {
            val showHorizontalScrollbar = horizontalScrollState.maxValue > 0
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(viewModel.entriesOf(serial)) { entry ->
                    FileListRow(
                        viewModel = viewModel,
                        serial = serial,
                        horizontalScrollState = horizontalScrollState,
                        item = entry,
                        selected = viewModel.selectedEntryOf(serial) == entry,
                        onClick = { viewModel.setSelectedEntry(serial, entry) },
                        onDoubleClick = { viewModel.openEntry(serial, entry) },
                        onSecondaryClick = { position ->
                            viewModel.setSelectedEntry(serial, entry)
                            openEntryContextMenu(entry, position)
                        },
                        contextMenuState = contextMenuState,
                        onDismissContextMenu = { contextMenuState = null }
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
                serial = serial,
                state = contextMenuState,
                onDismissRequest = { contextMenuState = null }
            )
        }
    }
}

@Composable
private fun FileListHeader(
    viewModel: MainStageModel,
    horizontalScrollState: ScrollState
) {
    val colors = AdbrowserTheme.colors
    val nameWidth = viewModel.fileColumnWidthNamePx.dp
    val sizeWidth = viewModel.fileColumnWidthSizePx.dp
    val modifiedWidth = viewModel.fileColumnWidthModifiedPx.dp
    val permissionWidth = viewModel.fileColumnWidthPermissionPx.dp
    val contentWidth = nameWidth + sizeWidth + modifiedWidth + permissionWidth + 30.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.subtleControlBackground, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .horizontalScroll(horizontalScrollState)
        ) {
            Row(
                modifier = Modifier
                    .width(contentWidth)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.mainHeaderName,
                    modifier = Modifier.width(nameWidth),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                FileColumnSplitter(
                    onDragDelta = viewModel::resizeNameAndSizeColumns,
                    onDragStopped = viewModel::persistFileColumnWidths
                )
                Text(
                    text = strings.mainHeaderSize,
                    modifier = Modifier.width(sizeWidth),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                FileColumnSplitter(
                    onDragDelta = viewModel::resizeSizeAndModifiedColumns,
                    onDragStopped = viewModel::persistFileColumnWidths
                )
                Text(
                    text = strings.mainHeaderModified,
                    modifier = Modifier.width(modifiedWidth),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                FileColumnSplitter(
                    onDragDelta = viewModel::resizeModifiedAndPermissionColumns,
                    onDragStopped = viewModel::persistFileColumnWidths
                )
                Text(
                    text = strings.mainHeaderPermission,
                    modifier = Modifier.width(permissionWidth),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun FileListRow(
    viewModel: MainStageModel,
    serial: String,
    horizontalScrollState: ScrollState,
    item: DeviceFileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    contextMenuState: FileContextMenuState?,
    onDismissContextMenu: () -> Unit
) {
    val colors = AdbrowserTheme.colors
    val nameWidth = viewModel.fileColumnWidthNamePx.dp
    val sizeWidth = viewModel.fileColumnWidthSizePx.dp
    val modifiedWidth = viewModel.fileColumnWidthModifiedPx.dp
    val permissionWidth = viewModel.fileColumnWidthPermissionPx.dp
    val contentWidth = nameWidth + sizeWidth + modifiedWidth + permissionWidth + 30.dp
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )
    val foreground = if (selected) Color.White else Color.Unspecified

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSecondaryPress(pass = PointerEventPass.Initial, onSecondaryPress = onSecondaryClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .hoverable(interactionSource = interactionSource)
                .pointerInput(item) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            onClick()
                            tryAwaitRelease()
                            pressed = false
                        },
                        onDoubleTap = { onDoubleClick() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Row(
                    modifier = Modifier
                        .width(contentWidth)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(nameWidth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContentIcon(
                            key = when {
                                item.isDirectory && item.isSymbolicLink -> AppIcons.LinkedFolder
                                item.isDirectory -> AppIcons.Folder
                                item.isSymbolicLink -> AppIcons.LinkedFile
                                else -> AppIcons.File
                            },
                            selected = selected,
                            contentDescription = item.name,
                            modifier = Modifier.size(16.dp),
                            tint = colors.primaryAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            color = foreground,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.sizeText,
                        modifier = Modifier.width(sizeWidth),
                        color = foreground,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.modifiedText,
                        modifier = Modifier.width(modifiedWidth),
                        color = foreground,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.permission,
                        modifier = Modifier.width(permissionWidth),
                        color = foreground,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        FileEntryContextMenuPopup(
            viewModel = viewModel,
            serial = serial,
            item = item,
            state = contextMenuState,
            onDismissRequest = onDismissContextMenu
        )
    }
}

@Composable
private fun FileColumnSplitter(
    onDragDelta: (Float) -> Unit,
    onDragStopped: () -> Unit
) {
    val colors = AdbrowserTheme.colors
    val density = LocalDensity.current
    val dragState = rememberDraggableState { deltaPx ->
        onDragDelta(with(density) { deltaPx.toDp().value })
    }

    Box(
        modifier = Modifier
            .width(10.dp)
            .height(20.dp)
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = { onDragStopped() }
            )
    )
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun FileIconView(viewModel: MainStageModel, serial: String) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.iconScrollRowIndexOf(serial),
        initialFirstVisibleItemScrollOffset = viewModel.iconScrollRowOffsetOf(serial)
    )
    val itemMinWidth = 120.dp
    val directoryChangeVersion = viewModel.directoryChangeVersionOf(serial)
    var handledDirectoryChangeVersion by remember(serial) { mutableStateOf(directoryChangeVersion) }
    val entries = viewModel.entriesOf(serial)
    val selectedEntry = viewModel.selectedEntryOf(serial)
    var contextMenuState by remember(serial) { mutableStateOf<FileContextMenuState?>(null) }
    var contextMenuRequestId by remember(serial) { mutableStateOf(0L) }

    fun openBlankContextMenu(position: Offset) {
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Blank(position, contextMenuRequestId)
    }

    fun openEntryContextMenu(entry: DeviceFileItem, position: Offset) {
        contextMenuRequestId += 1L
        contextMenuState = FileContextMenuState.Entry(entry, position, contextMenuRequestId)
    }

    LaunchedEffect(serial, directoryChangeVersion) {
        if (directoryChangeVersion == handledDirectoryChangeVersion) return@LaunchedEffect
        handledDirectoryChangeVersion = directoryChangeVersion
        contextMenuState = null
        gridState.scrollToItem(0)
    }

    LaunchedEffect(serial, gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.updateIconScrollState(serial, index, offset)
            }
    }

    LaunchedEffect(serial) {
        val selectedIndex = selectedEntry?.let { entries.indexOf(it) } ?: -1
        if (selectedIndex < 0) return@LaunchedEffect

        repeat(2) { withFrameNanos {} }
        if (!isLazyGridIndexVisible(gridState, selectedIndex))
            gridState.scrollToItem(selectedIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onBlankPrimaryPress {
                contextMenuState = null
                viewModel.setSelectedEntry(serial, null)
            }
            .onSecondaryPress(pass = PointerEventPass.Main) { position ->
                openBlankContextMenu(position)
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = itemMinWidth),
            state = gridState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(entries) { entry ->
                FileIconItem(
                    serial = serial,
                    viewModel = viewModel,
                    item = entry,
                    selected = viewModel.selectedEntryOf(serial) == entry,
                    onClick = { viewModel.setSelectedEntry(serial, entry) },
                    onDoubleClick = { viewModel.openEntry(serial, entry) },
                    onSecondaryClick = { position ->
                        viewModel.setSelectedEntry(serial, entry)
                        openEntryContextMenu(entry, position)
                    },
                    contextMenuState = contextMenuState,
                    onDismissContextMenu = { contextMenuState = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .padding(4.dp)
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
            serial = serial,
            state = contextMenuState,
            onDismissRequest = { contextMenuState = null }
        )
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun FileIconItem(
    serial: String,
    viewModel: MainStageModel,
    item: DeviceFileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    contextMenuState: FileContextMenuState?,
    onDismissContextMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )
    val foreground = if (selected) Color.White else Color.Unspecified

    Box(
        modifier = modifier
            .onSecondaryPress(pass = PointerEventPass.Initial, onSecondaryPress = onSecondaryClick)
    ) {
        Column(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .hoverable(interactionSource = interactionSource)
                .pointerInput(item) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            onClick()
                            tryAwaitRelease()
                            pressed = false
                        },
                        onDoubleTap = { onDoubleClick() }
                    )
                }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ContentIcon(
                key = when {
                    item.isDirectory && item.isSymbolicLink -> AppIcons.LinkedFolder
                    item.isDirectory -> AppIcons.Folder
                    item.isSymbolicLink -> AppIcons.LinkedFile
                    else -> AppIcons.File
                },
                selected = selected,
                contentDescription = item.name,
                modifier = Modifier.size(34.dp),
                tint = colors.primaryAccent
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.name,
                color = foreground,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        FileEntryContextMenuPopup(
            viewModel = viewModel,
            serial = serial,
            item = item,
            state = contextMenuState,
            onDismissRequest = onDismissContextMenu
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileBlankContextMenuPopup(
    viewModel: MainStageModel,
    serial: String,
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
            serial = serial,
            onDismissRequest = onDismissRequest
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileEntryContextMenuPopup(
    viewModel: MainStageModel,
    serial: String,
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
            serial = serial,
            item = item,
            onDismissRequest = onDismissRequest
        )
    }
}

private fun MenuScope.entryFileContextMenu(
    viewModel: MainStageModel,
    serial: String,
    item: DeviceFileItem,
    onDismissRequest: () -> Unit
) {
    fun perform(action: () -> Unit) {
        onDismissRequest()
        viewModel.setSelectedEntry(serial, item)
        action()
    }

    selectableItem(
        selected = false,
        onClick = { perform { viewModel.openEntry(serial, item) } }
    ) { Text(strings.menuOpen) }
    selectableItem(
        selected = false,
        onClick = { perform { viewModel.openEntryWith(serial, item) } }
    ) { Text(strings.menuOpenWith) }
    separator()
    selectableItem(
        selected = false,
        onClick = { perform(viewModel::renameSelectedEntry) }
    ) { Text(strings.menuRename) }
    selectableItemWithActionType(
        selected = false,
        actionType = CopyMenuItemOptionAction,
        onClick = { perform(viewModel::copySelectedEntry) }
    ) { Text(strings.menuCopy) }
    selectableItemWithActionType(
        selected = false,
        actionType = CutMenuItemOptionAction,
        onClick = { perform(viewModel::cutSelectedEntry) }
    ) { Text(strings.menuCut) }
    selectableItem(
        selected = false,
        onClick = { perform(viewModel::deleteSelectedEntry) }
    ) { Text(strings.menuDelete) }
    separator()
    selectableItem(
        selected = false,
        onClick = { perform(viewModel::showSelectedEntryProperties) }
    ) { Text(strings.menuProperties) }
}

private fun MenuScope.blankFileContextMenu(
    viewModel: MainStageModel,
    serial: String,
    onDismissRequest: () -> Unit
) {
    val hasEntries = viewModel.entriesOf(serial).isNotEmpty()
    val hasSelectedDevice = viewModel.isSelectedWorkspace(serial)

    fun perform(action: () -> Unit) {
        onDismissRequest()
        action()
    }

    selectableItem(
        selected = false,
        enabled = hasSelectedDevice,
        onClick = { perform(viewModel::refreshEntries) }
    ) { Text(strings.menuRefresh) }
    separator()
    selectableItem(
        selected = false,
        enabled = hasSelectedDevice,
        onClick = { perform(viewModel::createNewFolder) }
    ) { Text(strings.menuNewFolder) }
    separator()
    if (viewModel.canPasteEntry) {
        selectableItemWithActionType(
            selected = false,
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

private fun isLazyListIndexVisible(state: LazyListState, index: Int): Boolean {
    if (index < 0) return false
    return state.layoutInfo.visibleItemsInfo.any { it.index == index }
}

private fun isLazyGridIndexVisible(state: LazyGridState, index: Int): Boolean {
    if (index < 0) return false
    return state.layoutInfo.visibleItemsInfo.any { it.index == index }
}

private fun resolveListItemBackground(
    colors: AdbrowserColorScheme,
    selected: Boolean,
    hovered: Boolean,
    pressed: Boolean
) = when {
    selected && pressed -> colors.primaryAccentPressed
    selected && hovered -> colors.primaryAccentHover
    selected -> colors.primaryAccent
    pressed -> colors.panelBorder
    hovered -> colors.subtleControlBackground
    else -> Color.Transparent
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.onSecondaryPress(
    pass: PointerEventPass,
    onSecondaryPress: (Offset) -> Unit
) = onPointerEvent(PointerEventType.Press, pass = pass) { event ->
    if (!event.buttons.isSecondaryPressed) return@onPointerEvent
    if (event.changes.any { it.isConsumed }) return@onPointerEvent

    val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
    onSecondaryPress(change.position)
    event.changes.forEach { it.consume() }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.onBlankPrimaryPress(
    onPrimaryPress: () -> Unit
) = onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Final) { event ->
    if (!event.buttons.isPrimaryPressed) return@onPointerEvent
    if (event.changes.any { it.isConsumed }) return@onPointerEvent
    if (event.changes.none { it.changedToDownIgnoreConsumed() }) return@onPointerEvent
    onPrimaryPress()
}

@Composable
private fun FileListHint(
    iconKey: IconKey,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier.alpha(0.5f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContentIcon(
            key = iconKey,
            contentDescription = message,
            modifier = Modifier.size(60.dp),
            tint = colors.primaryAccent
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = colors.primaryAccent,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun DeviceListHint(
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier.alpha(0.5f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContentIcon(
            key = AppIcons.Devices,
            contentDescription = message,
            modifier = Modifier.size(60.dp),
            tint = colors.primaryAccent
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = colors.primaryAccent,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun PathBreadcrumbBar(viewModel: MainStageModel, serial: String) {
    val colors = AdbrowserTheme.colors
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.hintBackground)
            .edgeBorder(
                color = colors.panelBorder,
                top = true,
                bottom = false,
                start = false,
                end = false
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbRootButton(
            onClick = { viewModel.navigateToBreadcrumb(serial, "/") }
        )

        viewModel.breadcrumbsOf(serial).forEach { segment ->
            BreadcrumbSegment(
                segment = segment,
                onClick = { viewModel.navigateToBreadcrumb(serial, segment.fullPath) }
            )
        }
    }
}

@Composable
private fun BreadcrumbRootButton(onClick: () -> Unit) {
    val colors = AdbrowserTheme.colors

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        ContentIcon(
            key = AppIcons.FilePathArrow,
            contentDescription = "/",
            modifier = Modifier
                .size(14.dp)
                .alpha(0.5f)
        )
    }
}

@Composable
private fun BreadcrumbSegment(
    segment: PathBreadcrumbSegment,
    onClick: () -> Unit
) {
    val colors = AdbrowserTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        if (segment.showLeadingArrow) {
            ContentIcon(
                key = AppIcons.FilePathArrow,
                contentDescription = ">",
                modifier = Modifier
                    .size(14.dp)
                    .alpha(0.5f)
            )
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(2.dp)
        ) {
            Text(
                text = segment.displayName,
                fontSize = 13.sp,
                color = colors.pathBreadcrumbForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusBar(viewModel: MainStageModel) {
    val colors = AdbrowserTheme.colors
    val statusText = statusMessageText(viewModel.statusMessage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.hintBackground)
            .edgeBorder(
                color = colors.hintBorder,
                top = true,
                bottom = false,
                start = false,
                end = false
            )
            .height(34.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusText.ifBlank { strings.mainStatusReady },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = BuildVersion.TEXT,
            color = colors.pathBreadcrumbForeground,
            fontSize = 12.sp
        )
    }
}

private fun Modifier.edgeBorder(
    color: Color,
    top: Boolean = true,
    bottom: Boolean = true,
    start: Boolean = true,
    end: Boolean = true
): Modifier = drawBehind {
    val strokeWidth = 1.dp.toPx()
    val halfStroke = strokeWidth / 2f

    if (top)
        drawLine(
            color = color,
            start = Offset(halfStroke, halfStroke),
            end = Offset(size.width - halfStroke, halfStroke),
            strokeWidth = strokeWidth
        )
    if (bottom)
        drawLine(
            color = color,
            start = Offset(halfStroke, size.height - halfStroke),
            end = Offset(size.width - halfStroke, size.height - halfStroke),
            strokeWidth = strokeWidth
        )
    if (start)
        drawLine(
            color = color,
            start = Offset(halfStroke, halfStroke),
            end = Offset(halfStroke, size.height - halfStroke),
            strokeWidth = strokeWidth
        )
    if (end)
        drawLine(
            color = color,
            start = Offset(size.width - halfStroke, halfStroke),
            end = Offset(size.width - halfStroke, size.height - halfStroke),
            strokeWidth = strokeWidth
        )
}

@Composable
private fun viewModeLabel(modeKey: String?): String = when (modeKey) {
    "icons" -> strings.mainViewModeIcons
    "list" -> strings.mainViewModeList
    else -> ""
}

@Composable
private fun sortModeLabel(modeKey: String?): String = when (modeKey) {
    "size" -> strings.mainSortModeSize
    "modified" -> strings.mainSortModeModified
    "name" -> strings.mainSortModeName
    else -> ""
}

@Composable
private fun statusMessageText(status: MainStageModel.StatusMessage): String = when (status) {
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
        formatWithArgs(template, status.args)
    }
}

private fun formatWithArgs(template: String, args: List<String>): String {
    var result = template
    args.forEachIndexed { index, value ->
        result = result.replace("{$index}", value)
    }
    return result
}

@Composable
private fun fileListHintIcon(hint: MainStageModel.FileListHint) = when (hint) {
    MainStageModel.FileListHint.None -> null
    MainStageModel.FileListHint.EmptyFolder -> AppIcons.Folder
    MainStageModel.FileListHint.DeviceOffline,
    MainStageModel.FileListHint.DeviceNotFound -> AppIcons.DeletedFolder
    MainStageModel.FileListHint.PermissionDenied -> AppIcons.BlockedFolder
    MainStageModel.FileListHint.PathNotFound,
    MainStageModel.FileListHint.LoadFailed -> AppIcons.ErrorFolder
}

@Composable
private fun fileListHintMessage(hint: MainStageModel.FileListHint) = when (hint) {
    MainStageModel.FileListHint.None -> ""
    MainStageModel.FileListHint.EmptyFolder -> strings.mainFileListHintEmptyFolder
    MainStageModel.FileListHint.DeviceOffline -> strings.mainFileListHintDeviceOffline
    MainStageModel.FileListHint.DeviceNotFound -> strings.mainFileListHintDeviceNotFound
    MainStageModel.FileListHint.PathNotFound -> strings.mainFileListHintPathNotFound
    MainStageModel.FileListHint.PermissionDenied -> strings.mainFileListHintPermissionDenied
    MainStageModel.FileListHint.LoadFailed -> strings.mainFileListHintLoadFailed
}

@Composable
private fun MainStageDialogs(viewModel: MainStageModel) {
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

private val OnlineStatusColor = Color(0xFF2DB455)
private val OfflineStatusColor = Color(0xFFE46868)