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
 * This file is created by fankes on 2026/4/7.
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.InitialSetupStageModel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun FrameWindowScope.InitialSetupStage(
    viewModel: InitialSetupStageModel,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val statusText = StatusMessage(viewModel.status)
    val isContinueEnabled = !viewModel.isBusy && viewModel.adbExecPath.text.toString().trim().isNotBlank()
    val selectAdbExecutableText = strings.setupSelectAdbExecutable

    val textStyle = JewelTheme.defaultTextStyle
    val lineHeight = textStyle.lineHeight.value.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .padding(28.dp)
    ) {
        Text(
            text = strings.setupHeaderTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.setupDescription,
            color = colors.pathBreadcrumbForeground,
            // Reserve enough space for the description up front so the footer row does not jump
            // vertically when localized text wraps differently across languages.
            modifier = Modifier.heightIn(min = lineHeight * 2)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) { 
            Text(
                text = strings.setupAdbPath,
                fontWeight = FontWeight.SemiBold
            )
            TextField(
                state = viewModel.adbExecPath,
                enabled = !viewModel.isBusy,
                modifier = Modifier
                    .weight(1f)
                    .height(AdbrowserTheme.DefaultTextFieldHeight),
                placeholder = { Text(strings.preferencesAdbPathPlaceholder) }
            )
            OutlinedButton(
                enabled = !viewModel.isBusy,
                onClick = { viewModel.browseAdbPath(window, selectAdbExecutableText) },
                modifier = Modifier.height(AdbrowserTheme.DefaultTextFieldHeight)
            ) {
                Text(strings.preferencesBrowse)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                color = StatusErrorColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )
            OutlinedButton(
                enabled = !viewModel.isBusy,
                onClick = onCancel
            ) {
                Text(strings.dialogCommonCancel)
            }
            Spacer(Modifier.width(10.dp))
            DefaultButton(
                enabled = isContinueEnabled,
                // The stage only advances after validation and persistence succeed.
                onClick = { viewModel.continueSetup(onSuccess = onContinue) }
            ) {
                Text(strings.setupContinue)
            }
        }
    }
}

@Composable
private fun StatusMessage(status: InitialSetupStageModel.Status) = when (status) {
    InitialSetupStageModel.Status.None -> ""
    InitialSetupStageModel.Status.AdbPathNotFound -> strings.preferencesStatusAdbPathNotFound
    InitialSetupStageModel.Status.AdbExecutableInvalid -> strings.preferencesStatusAdbExecutableInvalid
    InitialSetupStageModel.Status.AdbPathEmpty -> strings.preferencesStatusAdbPathEmpty
    is InitialSetupStageModel.Status.Failed -> {
        val reason = status.reason?.takeIf { it.isNotBlank() } ?: strings.commonUnknownError
        "${strings.preferencesStatusFailedPrefix}$reason"
    }
}

private val StatusErrorColor = Color(0xFFB94747)