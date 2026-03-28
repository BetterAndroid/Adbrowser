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
package com.highcapable.adbrowser.frontend.ui.window

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.backend.domain.DeviceConnectionState
import com.highcapable.adbrowser.backend.domain.FsEntry
import com.highcapable.adbrowser.backend.domain.FsEntryType
import com.highcapable.adbrowser.frontend.cl.LocalAppState
import com.highcapable.adbrowser.frontend.state.FileSortMode
import com.highcapable.adbrowser.frontend.state.FileViewMode
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.window.manager.AppWindow
import com.highcapable.adbrowser.frontend.ui.window.manager.LocalWindowManager
import com.highcapable.adbrowser.generated.AdbrowserProperties

@Composable
fun MainWindow(onCloseRequest: () -> Unit) {
    Window(
        onCloseRequest = onCloseRequest,
        title = AdbrowserProperties.PROJECT_NAME,
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        AdbrowserTheme {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrameWindowScope.MainScreen() {
    val appState = LocalAppState.current
    val windowManager = LocalWindowManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appTitle) },
                actions = {
                    TextButton(onClick = { windowManager.open(AppWindow.Preferences) }) {
                        Text(strings.preferences)
                    }
                    TextButton(onClick = { windowManager.open(AppWindow.LogViewer) }) {
                        Text(strings.logViewer)
                    }
                }
            )
        },
        bottomBar = {
            BottomStatusBar()
        }
    ) { innerPaddings ->
        Row(
            modifier = Modifier
                .padding(innerPaddings)
                .fillMaxSize()
        ) {
            DeviceSidebar(
                modifier = Modifier.width(280.dp).fillMaxHeight()
            )
            HorizontalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
            FileBrowserPane(
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun BottomStatusBar() {
    val appState = LocalAppState.current
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.status}: ${appState.statusMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "v${AdbrowserProperties.PROJECT_APP_VERSION}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceSidebar(modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val devices = appState.devices

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.devicePanelTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { appState.refreshDevices() }) { Text(strings.refresh) }
        }

        if (devices.isEmpty()) {
            Text(strings.noDevices, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                items(devices, key = { it.id }) { device ->
                    val selected = appState.selectedDeviceId == device.id
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (selected) 2.dp else 0.dp,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().clickable { appState.selectDevice(device.id) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${device.model} • ${device.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (device.state == DeviceConnectionState.Online) "online" else "offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (device.state == DeviceConnectionState.Online) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileBrowserPane(modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val entries = appState.visibleEntries()
    var showViewMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var createFolderName by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { appState.goUp() }) { Text(strings.up) }
            Button(onClick = { appState.openPath("/") }) { Text(strings.root) }
            Button(onClick = {
                appState.selectedDeviceId?.let { id ->
                    val home = appState.preferences.deviceHomePaths[id] ?: "/"
                    appState.openPath(home)
                }
            }) { Text(strings.home) }
            Spacer(Modifier.weight(1f))

            Box {
                TextButton(onClick = { showViewMenu = true }) { Text(strings.viewMode) }
                DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                    DropdownMenuItem(text = { Text(strings.listView) }, onClick = {
                        appState.viewMode = FileViewMode.List
                        showViewMenu = false
                    })
                    DropdownMenuItem(text = { Text(strings.gridView) }, onClick = {
                        appState.viewMode = FileViewMode.Grid
                        showViewMenu = false
                    })
                }
            }

            Box {
                TextButton(onClick = { showSortMenu = true }) { Text(strings.sortMode) }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text(strings.nameSort) }, onClick = {
                        appState.sortMode = FileSortMode.Name
                        showSortMenu = false
                    })
                    DropdownMenuItem(text = { Text(strings.sizeSort) }, onClick = {
                        appState.sortMode = FileSortMode.Size
                        showSortMenu = false
                    })
                    DropdownMenuItem(text = { Text(strings.modifiedSort) }, onClick = {
                        appState.sortMode = FileSortMode.ModifiedTime
                        showSortMenu = false
                    })
                }
            }
        }

        OutlinedTextField(
            value = appState.searchQuery,
            onValueChange = { appState.searchQuery = it },
            label = { Text(strings.search) },
            placeholder = { Text(strings.searchPlaceholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${strings.path}: ${appState.currentPath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = createFolderName,
                onValueChange = { createFolderName = it },
                label = { Text(strings.folderName) },
                singleLine = true,
                modifier = Modifier.width(220.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (createFolderName.isNotBlank()) {
                    if (appState.createFolder(createFolderName.trim())) createFolderName = ""
                }
            }) {
                Text(strings.createFolder)
            }
        }

        HorizontalDivider()

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noDevices, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries, key = { it.path }) { entry ->
                    FileRow(entry = entry, onOpen = {
                        if (entry.type == FsEntryType.Directory) appState.openPath(entry.path)
                    }, onDelete = { appState.deleteEntry(entry) })
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    entry: FsEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeLabel = when (entry.type) {
                FsEntryType.Directory -> "DIR"
                FsEntryType.File -> "FILE"
                FsEntryType.Symlink -> "LINK"
                FsEntryType.Unknown -> "UNKNOWN"
            }
            Text(typeLabel, modifier = Modifier.width(64.dp), style = MaterialTheme.typography.labelMedium)
            Text(entry.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.sizeBytes.toString(), modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}