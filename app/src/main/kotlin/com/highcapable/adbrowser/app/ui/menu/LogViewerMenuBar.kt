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
 * This file is created by fankes on 2026/4/16.
 */
package com.highcapable.adbrowser.app.ui.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.input.KeyShortcut
import com.highcapable.adbrowser.app.ui.vm.LogViewerStageModel
import com.highcapable.adbrowser.core.logging.LogLevel

@Composable
fun FrameWindowScope.LogViewerMenuBar(
    viewModel: LogViewerStageModel,
    onCloseRequest: () -> Unit
) {
    val exportDialogTitle = strings.logsExportDialogTitle

    MenuBar {
        Menu(strings.menuFile) {
            Item(
                text = strings.logsMenuExport,
                mnemonic = 'E',
                enabled = viewModel.hasVisibleEntries,
                shortcut = KeyShortcut(Key.E),
                onClick = { viewModel.export(window, exportDialogTitle) }
            )
            Item(
                text = strings.logsMenuClear,
                enabled = viewModel.hasEntries,
                onClick = viewModel::clearEntries
            )
        }
        Menu(strings.menuView) {
            Menu(
                text = strings.logsMenuLevel,
                mnemonic = 'L'
            ) {
                CheckboxItem(
                    text = strings.logsLevelTrace,
                    checked = viewModel.isLevelVisible(LogLevel.Trace),
                    onCheckedChange = { viewModel.setLevelVisible(LogLevel.Trace, it) }
                )
                CheckboxItem(
                    text = strings.logsLevelInformation,
                    checked = viewModel.isLevelVisible(LogLevel.Information),
                    onCheckedChange = { viewModel.setLevelVisible(LogLevel.Information, it) }
                )
                CheckboxItem(
                    text = strings.logsLevelWarning,
                    checked = viewModel.isLevelVisible(LogLevel.Warning),
                    onCheckedChange = { viewModel.setLevelVisible(LogLevel.Warning, it) }
                )
                CheckboxItem(
                    text = strings.logsLevelError,
                    checked = viewModel.isLevelVisible(LogLevel.Error),
                    onCheckedChange = { viewModel.setLevelVisible(LogLevel.Error, it) }
                )
            }
            Separator()
            Item(
                text = strings.logsMenuResetColumnWidths,
                mnemonic = 'R',
                shortcut = KeyShortcut(Key.R),
                onClick = viewModel::resetColumnWidths
            )
        }
    }
}