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
 * This file is created by fankes on 2026/4/9.
 */
package com.highcapable.adbrowser.app.ui.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.input.KeyShortcut
import com.highcapable.adbrowser.app.ui.vm.MainStageModel
import com.highcapable.adbrowser.app.ui.window.manager.AppWindow
import com.highcapable.adbrowser.app.ui.window.manager.LocalWindowManager
import com.highcapable.adbrowser.core.common.utils.OsType

@Composable
fun FrameWindowScope.MainMenuBar(
    viewModel: MainStageModel,
    onCloseRequest: () -> Unit
) {
    val windowManager = LocalWindowManager.current
    val openPreferences = { windowManager.open(AppWindow.Preferences) }

    MenuBar {
        Menu(strings.menuFile) {
            Item(
                text = strings.menuOpen,
                enabled = viewModel.hasSingleSelectedEntry,
                onClick = viewModel::openSelectedEntry
            )
            if (!viewModel.selectedEntryIsDirectory)
                Item(
                    text = strings.menuOpenWith,
                    enabled = viewModel.hasSingleSelectedEntry,
                    onClick = viewModel::openSelectedEntryWith
                )
            Separator()
            Item(
                text = strings.menuNewFolder,
                enabled = viewModel.canShowBlankFileContextMenu,
                onClick = viewModel::createNewFolder,
                shortcut = KeyShortcut(Key.N)
            )
            Separator()
            Item(
                text = strings.menuRename,
                enabled = viewModel.hasSingleSelectedEntry,
                onClick = viewModel::renameSelectedEntry,
                shortcut = KeyShortcut(Key.F2)
            )
            Item(
                text = strings.menuDelete,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::deleteSelectedEntry,
                shortcut = KeyShortcut(Key.Delete)
            )
            Item(
                text = strings.menuProperties,
                enabled = viewModel.canShowFileProperties,
                onClick = viewModel::showProperties,
                shortcut = KeyShortcut(Key.I)
            )
            if (!OsType.isMacOS) {
                Separator()
                Item(
                    text = strings.menuPreferences,
                    onClick = openPreferences,
                    shortcut = KeyShortcut(Key.Comma)
                )
                Separator()
                Item(
                    text = strings.menuExit,
                    onClick = onCloseRequest,
                    shortcut = KeyShortcut(Key.Q)
                )
            }
        }
        Menu(strings.menuEdit) {
            Item(
                text = strings.menuCut,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::cutSelectedEntry,
                shortcut = KeyShortcut(Key.X)
            )
            Item(
                text = strings.menuCopy,
                enabled = viewModel.hasSelectedEntry,
                onClick = viewModel::copySelectedEntry,
                shortcut = KeyShortcut(Key.C)
            )
            Item(
                text = strings.menuPaste,
                enabled = viewModel.canShowBlankFileContextMenu && viewModel.canPasteEntry,
                onClick = viewModel::pasteToCurrentPath,
                shortcut = KeyShortcut(Key.V)
            )
            Separator()
            Item(
                strings.menuSelectAll,
                enabled = viewModel.canShowBlankFileContextMenu && viewModel.currentEntries.isNotEmpty(),
                onClick = viewModel::selectAllEntries,
                shortcut = KeyShortcut(Key.A)
            )
            Item(
                strings.menuInverseSelect,
                enabled = viewModel.canShowBlankFileContextMenu && viewModel.currentEntries.isNotEmpty(),
                onClick = viewModel::inverseSelectEntries,
                shortcut = KeyShortcut(Key.A, shift = true)
            )
        }
        Menu(strings.menuView) {
            Item(
                text = strings.menuRefresh,
                onClick = viewModel::refreshEntries,
                shortcut = KeyShortcut(Key.F5)
            )
            Separator()
            Item(
                text = strings.menuViewMode,
                onClick = viewModel::openViewModeMenu,
                shortcut = KeyShortcut(Key.M)
            )
            Item(
                text = strings.menuSortMode,
                onClick = viewModel::openSortModeMenu,
                shortcut = KeyShortcut(Key.O)
            )
            Separator()
            CheckboxItem(
                text = strings.menuShowStatusBar,
                checked = viewModel.isStatusBarVisible,
                onCheckedChange = { viewModel.toggleStatusBar() },
                shortcut = KeyShortcut(Key.B)
            )
            Separator()
            Item(
                text = strings.menuAppLogs,
                onClick = { windowManager.open(AppWindow.LogViewer) },
                shortcut = KeyShortcut(Key.L)
            )
        } 
        Menu(strings.menuDevice) {
            Item(
                text = strings.menuPairNewDevice,
                onClick = viewModel::pairNewDevice
            )
            Item(
                text = strings.menuConnectToDevice,
                onClick = viewModel::connectToDevice
            )
            Separator()
            Item(
                text = strings.menuRefreshDevices,
                onClick = viewModel::refreshDevices
            )
        }
        Menu(strings.menuGo) {
            Item(
                text = strings.menuForward,
                enabled = viewModel.canNavigateForward,
                onClick = viewModel::navigateForward,
                shortcut = KeyShortcut(Key.DirectionRight, alt = true)
            )
            Item(
                text = strings.menuBack,
                enabled = viewModel.canNavigateBack,
                onClick = viewModel::navigateBack,
                shortcut = KeyShortcut(Key.DirectionLeft, alt = true)
            )
            Separator()
            Item(
                text = strings.menuUp,
                enabled = viewModel.canNavigateUp,
                onClick = viewModel::navigateUp,
                shortcut = KeyShortcut(Key.DirectionUp, alt = true)
            )
            Item(
                text = strings.menuRoot,
                enabled = viewModel.canNavigateRoot,
                onClick = viewModel::navigateRoot,
                shortcut = KeyShortcut(Key.R, shift = true)
            )
            Item(
                text = strings.menuHome,
                onClick = viewModel::navigateHome,
                shortcut = KeyShortcut(Key.H, shift = true)
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