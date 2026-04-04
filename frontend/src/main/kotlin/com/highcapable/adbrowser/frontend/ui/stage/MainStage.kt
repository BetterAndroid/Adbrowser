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
package com.highcapable.adbrowser.frontend.ui.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.backend.utils.OsType
import com.highcapable.adbrowser.frontend.ui.assets.AppIcons
import com.highcapable.adbrowser.frontend.ui.component.ContentIcon
import com.highcapable.adbrowser.frontend.ui.component.ContentIconButton
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.MainStageModel
import com.highcapable.adbrowser.frontend.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import com.highcapable.adbrowser.frontend.ui.vm.model.PathBreadcrumbSegment
import com.highcapable.adbrowser.frontend.ui.window.manager.AppWindow
import com.highcapable.adbrowser.frontend.ui.window.manager.LocalWindowManager
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState

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
                text = strings.menuNewFolder,
                onClick = viewModel::createNewFolder,
                shortcut = createShortcut(Key.N)
            )
            Separator()
            Item(
                text = strings.menuRename,
                onClick = viewModel::renameSelectedEntry,
                shortcut = createShortcut(Key.F2)
            )
            Item(
                text = strings.menuDelete,
                onClick = viewModel::deleteSelectedEntry,
                shortcut = createShortcut(Key.Delete)
            )
            Item(
                text = strings.menuProperties,
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
                onClick = viewModel::cutSelectedEntry,
                shortcut = createShortcut(Key.X)
            )
            Item(
                text = strings.menuCopy,
                onClick = viewModel::copySelectedEntry,
                shortcut = createShortcut(Key.C)
            )
            Item(
                text = strings.menuPaste,
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
                onClick = viewModel::navigateForward,
                shortcut = createShortcut(Key.DirectionRight, alt = true)
            )
            Item(
                text = strings.menuBack,
                onClick = viewModel::navigateBack,
                shortcut = createShortcut(Key.DirectionLeft, alt = true)
            )
            Separator()
            Item(
                text = strings.menuUp,
                onClick = viewModel::navigateUp,
                shortcut = createShortcut(Key.DirectionUp, alt = true)
            )
            Item(
                text = strings.menuRoot,
                onClick = viewModel::navigateRoot,
                shortcut = createShortcut(Key.R, shift = true)
            )
            Item(
                text = strings.menuHome,
                onClick = viewModel::navigateHome,
                shortcut = createShortcut(Key.H, shift = true)
            )
            Separator()
            Item(
                text = strings.menuSearch,
                onClick = viewModel::searchInCurrentPath,
                shortcut = createShortcut(Key.F)
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
    val splitState = rememberSplitLayoutState(initialSplitFraction = 300f / 1300f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
    ) {
        HorizontalSplitLayout(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp),
            state = splitState,
            firstPaneMinWidth = 240.dp,
            secondPaneMinWidth = 560.dp,
            dividerStyle = AdbrowserTheme.dividerStyle(10.dp),
            first = {
                DevicePane(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            },
            second = {
                FilePane(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )

        if (viewModel.isStatusBarVisible)
            StatusBar(viewModel = viewModel)
    }
}

@Composable
private fun DevicePane(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier
            .background(colors.panelBackground, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.mainDevicesTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            ContentIconButton(
                key = AppIcons.Refresh,
                contentDescription = strings.mainRefreshDeviceDescription,
                onClick = viewModel::refreshDevices
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(viewModel.devices) { device ->
                DeviceRow(
                    item = device,
                    selected = viewModel.selectedDevice == device,
                    onClick = { viewModel.selectedDevice = device }
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    item: AndroidDeviceItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AdbrowserTheme.colors
    val background = if (selected) colors.primaryAccent else Color.Transparent
    val foreground = if (selected) Color.White else Color.Unspecified
    val statusColor = if (item.isOnline) Color(0xFF2DB455) else Color(0xFFE46868)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(item.name, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(item.model, color = foreground, fontSize = 12.sp)
        Text(item.serial, color = foreground, fontSize = 11.sp)
    }
}

@Composable
private fun FilePane(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier
            .background(colors.panelBackground, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        NavigationBar(viewModel = viewModel)
        Spacer(Modifier.height(12.dp))
        FileListArea(viewModel = viewModel, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(8.dp))
        PathBreadcrumbBar(viewModel = viewModel)
    }
}

@Composable
private fun NavigationBar(viewModel: MainStageModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContentIconButton(
            key = AppIcons.ArrowLeft,
            contentDescription = strings.menuBack,
            enabled = viewModel.canNavigateBack,
            outlined = true,
            onClick = viewModel::navigateBack
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.ArrowRight,
            contentDescription = strings.menuForward,
            enabled = viewModel.canNavigateForward,
            outlined = true,
            onClick = viewModel::navigateForward
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.ArrowUp,
            contentDescription = strings.menuUp,
            enabled = viewModel.canNavigateUp,
            outlined = true,
            onClick = viewModel::navigateUp
        )
        Spacer(Modifier.width(6.dp))
        ContentIconButton(
            key = AppIcons.Home,
            contentDescription = strings.menuHome,
            outlined = true,
            onClick = viewModel::navigateHome
        )
        Spacer(Modifier.width(6.dp))
        TextField(
            state = viewModel.pathInput,
            modifier = Modifier
                .weight(1f)
                .height(AdbrowserTheme.DefaultTextFieldHeight)
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
                    onClick = { viewModel.selectedViewMode = option }
                ) { Text(option.label) }
            }
        }
    ) {
        Text(selected?.label ?: "")
    }
}

@Composable
private fun SortModeDropdown(viewModel: MainStageModel, modifier: Modifier = Modifier) {
    val selected = viewModel.selectedSortMode
    Dropdown(
        modifier = modifier
            .width(110.dp)
            .height(AdbrowserTheme.DefaultTextFieldHeight),
        menuContent = {
            viewModel.sortModes.forEach { option ->
                selectableItem(
                    selected = option == selected,
                    onClick = { viewModel.selectedSortMode = option }
                ) { Text(option.label) }
            }
        }
    ) {
        Text(selected?.label ?: "")
    }
}

@Composable
private fun FileListArea(
    viewModel: MainStageModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (viewModel.isListViewMode) 
            FileListView(viewModel = viewModel)
        else FileIconView(viewModel = viewModel)
 
        if (viewModel.fileListHintMessage.isNotBlank())
            FileListHint(
                message = viewModel.fileListHintMessage,
                modifier = Modifier.align(Alignment.Center)
            )
    }
}

@Composable
private fun FileListView(viewModel: MainStageModel) {
    val colors = AdbrowserTheme.colors

    Column(modifier = Modifier.fillMaxSize()) {
        FileListHeader()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(viewModel.currentEntries) { entry ->
                FileListRow(
                    item = entry,
                    selected = viewModel.selectedEntry == entry,
                    onClick = { viewModel.selectedEntry = entry },
                    onDoubleClick = { viewModel.openEntry(entry) }
                )
            }
        }
    }
}

@Composable
private fun FileListHeader() {
    val colors = AdbrowserTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.subtleControlBackground, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = strings.mainHeaderName,
            modifier = Modifier.weight(0.42f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = strings.mainHeaderSize,
            modifier = Modifier.weight(0.16f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = strings.mainHeaderModified,
            modifier = Modifier.weight(0.26f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = strings.mainHeaderPermission,
            modifier = Modifier.weight(0.16f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FileListRow(
    item: DeviceFileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val colors = AdbrowserTheme.colors
    val background = if (selected) colors.primaryAccent else Color.Transparent
    val foreground = if (selected) Color.White else Color.Unspecified

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(0.42f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                key = when {
                    item.isDirectory && item.isSymbolicLink -> AppIcons.LinkedFolder
                    item.isDirectory -> AppIcons.Folder
                    item.isSymbolicLink -> AppIcons.LinkedFile
                    else -> AppIcons.File
                },
                contentDescription = item.name,
                modifier = Modifier.size(16.dp)
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
        Text(
            text = item.size,
            modifier = Modifier.weight(0.16f),
            color = foreground,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.modified,
            modifier = Modifier.weight(0.26f),
            color = foreground,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.permission,
            modifier = Modifier.weight(0.16f),
            color = foreground,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileIconView(viewModel: MainStageModel) {
    val colors = AdbrowserTheme.colors

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        val entries = viewModel.currentEntries
        val chunked = entries.chunked(6)
        items(chunked) { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { entry ->
                    FileIconItem(
                        item = entry,
                        selected = viewModel.selectedEntry == entry,
                        onClick = { viewModel.selectedEntry = entry },
                        modifier = Modifier
                            .width(120.dp)
                            .height(104.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileIconItem(
    item: DeviceFileItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val background = if (selected) colors.primaryAccent else Color.Transparent
    val foreground = if (selected) Color.White else Color.Unspecified

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            key = when {
                item.isDirectory && item.isSymbolicLink -> AppIcons.LinkedFolder
                item.isDirectory -> AppIcons.Folder
                item.isSymbolicLink -> AppIcons.LinkedFile
                else -> AppIcons.File
            },
            contentDescription = item.name,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.name,
            color = foreground,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun FileListHint(
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            key = AppIcons.Folder,
            contentDescription = message,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            color = colors.fileHintForeground,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun PathBreadcrumbBar(viewModel: MainStageModel) {
    val colors = AdbrowserTheme.colors
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.hintBackground, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbRootButton(
            onClick = { viewModel.navigateToBreadcrumb("/") }
        )

        viewModel.pathBreadcrumbSegments.forEach { segment ->
            BreadcrumbSegment(
                segment = segment,
                onClick = { viewModel.navigateToBreadcrumb(segment.fullPath) }
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
            modifier = Modifier.size(14.dp).alpha(0.5f)
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
                modifier = Modifier.size(14.dp)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.hintBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = viewModel.statusMessage.ifBlank { strings.mainStatusReady },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // TODO: replace with actual commit id from build info.
        Text(
            text = "Build: TODO",
            color = colors.pathBreadcrumbForeground,
            fontSize = 12.sp
        )
    }
}