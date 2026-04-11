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
package com.highcapable.adbrowser.app.ui.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.LogViewerStageModel
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
fun FrameWindowScope.LogViewerStage(
    viewModel: LogViewerStageModel,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultButton(onClick = viewModel::clear) { Text(strings.logsRefresh) }
            OutlinedButton(onClick = viewModel::export) { Text(strings.logsRefresh) }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(viewModel.entries) {
                Text(it, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}