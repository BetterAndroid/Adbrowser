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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.locale.languageOptions
import com.highcapable.adbrowser.app.ui.component.PanelSurface
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.PreferencesStageModel
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.awt.Window

@Composable
fun FrameWindowScope.PreferencesStage(
    viewModel: PreferencesStageModel,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val scrollState = rememberScrollState()
    val selectAdbExecutableText = strings.setupSelectAdbExecutable

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PreferencesTabs(viewModel)
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (viewModel.currentTab) {
                    PreferencesStageModel.Tab.General -> GeneralTab(viewModel)
                    PreferencesStageModel.Tab.Files -> FilesTab(viewModel)
                    PreferencesStageModel.Tab.Device -> DeviceTab(
                        viewModel = viewModel,
                        parentWindow = window,
                        browseDialogTitle = selectAdbExecutableText
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            val statusMessage = preferencesStatusMessage(viewModel.status)
            val statusColor = when (viewModel.statusCategory) {
                PreferencesStageModel.StatusCategory.Normal -> colors.pathBreadcrumbForeground
                PreferencesStageModel.StatusCategory.Error -> StatusErrorColor
            }

            Text(
                text = statusMessage,
                color = statusColor,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .weight(1f)
            )
            OutlinedButton(
                enabled = !viewModel.isSaving,
                onClick = {
                    viewModel.cancel()
                    onCloseRequest()
                },
                modifier = Modifier.width(80.dp)
            ) {
                Text(strings.dialogCommonCancel)
            }
            Spacer(Modifier.width(12.dp))
            DefaultButton(
                enabled = !viewModel.isSaving,
                onClick = {
                    viewModel.save(onSuccess = onCloseRequest)
                },
                modifier = Modifier.width(80.dp)
            ) {
                Text(strings.preferencesSave)
            }
        }
    }
}

@Composable
private fun PreferencesTabs(viewModel: PreferencesStageModel) {
    val tabs = listOf(
        TabData.Default(
            selected = viewModel.currentTab == PreferencesStageModel.Tab.General,
            closable = false,
            onClick = { viewModel.currentTab = PreferencesStageModel.Tab.General },
            content = { tabState ->
                SimpleTabContent(
                    state = tabState,
                    label = {
                        Text(
                            text = strings.preferencesGeneral,
                            fontSize = TabLabelFontSize,
                            fontWeight = TabLabelFontWeight
                        )
                    }
                )
            }
        ),
        TabData.Default(
            selected = viewModel.currentTab == PreferencesStageModel.Tab.Files,
            closable = false,
            onClick = { viewModel.currentTab = PreferencesStageModel.Tab.Files },
            content = { tabState ->
                SimpleTabContent(
                    state = tabState,
                    label = {
                        Text(
                            text = strings.preferencesFiles,
                            fontSize = TabLabelFontSize,
                            fontWeight = TabLabelFontWeight
                        )
                    }
                )
            }
        ),
        TabData.Default(
            selected = viewModel.currentTab == PreferencesStageModel.Tab.Device,
            closable = false,
            onClick = { viewModel.currentTab = PreferencesStageModel.Tab.Device },
            content = { tabState ->
                SimpleTabContent(
                    state = tabState,
                    label = {
                        Text(
                            text = strings.preferencesDevice,
                            fontSize = TabLabelFontSize,
                            fontWeight = TabLabelFontWeight
                        )
                    }
                )
            }
        )
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        TabStrip(
            tabs = tabs,
            style = AdbrowserTheme.defaultTabStyle,
            modifier = Modifier.fillMaxWidth()
        )
        Divider(
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth(),
            startIndent = 0.dp
        )
    }
}

@Composable
private fun GeneralTab(viewModel: PreferencesStageModel) {
    PanelSurface(
        modifier = Modifier.fillMaxWidth(),
        padding = PanelPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.preferencesUserInterface, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.preferencesLanguage,
                    modifier = Modifier.padding(end = 12.dp)
                ) 
                LanguageDropdown(viewModel)
            }
        }
    }
    PanelSurface(
        modifier = Modifier.fillMaxWidth(),
        padding = PanelPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.preferencesResetOptions, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultButton(onClick = viewModel::resetSidebarSpacing) {
                    Text(strings.preferencesResetSidebarSpacing)
                }
                DefaultButton(onClick = viewModel::resetFileColumnWidths) {
                    Text(strings.preferencesResetFileColumnWidths)
                }
                DefaultButton(onClick = viewModel::resetMainWindowBounds) {
                    Text(strings.preferencesResetMainWindowBounds)
                }
            }
        }
    }
}

@Composable
private fun FilesTab(viewModel: PreferencesStageModel) {
    PanelSurface(
        modifier = Modifier.fillMaxWidth(),
        padding = PanelPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.preferencesDisplayAndView, fontWeight = FontWeight.SemiBold)
            CheckboxRow(
                checked = viewModel.showHiddenFiles,
                onCheckedChange = { viewModel.showHiddenFiles = it }
            ) { Text(strings.preferencesShowHiddenFiles) }
            CheckboxRow(
                checked = viewModel.foldersFirst,
                onCheckedChange = { viewModel.foldersFirst = it }
            ) { Text(strings.preferencesFoldersFirst) }
            CheckboxRow(
                checked = viewModel.rememberLastFileViewMode,
                onCheckedChange = { viewModel.rememberLastFileViewMode = it }
            ) { Text(strings.preferencesRememberLastFileViewMode) }
        }
    }
    PanelSurface(
        modifier = Modifier.fillMaxWidth(),
        padding = PanelPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.preferencesPathAndNavigation, fontWeight = FontWeight.SemiBold)
            CheckboxRow(
                checked = viewModel.rememberLastDevicePath,
                onCheckedChange = { viewModel.rememberLastDevicePath = it }
            ) { Text(strings.preferencesRememberLastDevicePath) }
        }
    }
}

@Composable
private fun DeviceTab(
    viewModel: PreferencesStageModel,
    parentWindow: Window?,
    browseDialogTitle: String
) {
    PanelSurface(
        modifier = Modifier.fillMaxWidth(),
        padding = PanelPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.preferencesEnvironment, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.preferencesAdbPath,
                    modifier = Modifier.padding(end = 12.dp)
                )
                TextField(
                    state = viewModel.adbExecPath,
                    modifier = Modifier
                        .weight(1f)
                        .height(AdbrowserTheme.DefaultTextFieldHeight),
                    placeholder = { Text(strings.preferencesAdbPathPlaceholder) }
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.browseAdbPath(parentWindow, browseDialogTitle) },
                    modifier = Modifier.height(AdbrowserTheme.DefaultTextFieldHeight)
                ) {
                    Text(strings.preferencesBrowse)
                }
            }
        }
    }
    PanelSurface(
        modifier = Modifier.fillMaxWidth(),
        padding = PanelPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.preferencesConnection, fontWeight = FontWeight.SemiBold)
            CheckboxRow(
                checked = viewModel.rememberLastDevice,
                onCheckedChange = { viewModel.rememberLastDevice = it }
            ) { Text(strings.preferencesRememberLastDevice) }
            CheckboxRow(
                checked = viewModel.superuser,
                onCheckedChange = { viewModel.superuser = it }
            ) { Text(strings.preferencesSuperuser) }
        }
    }
}

@Composable
private fun LanguageDropdown(viewModel: PreferencesStageModel) {
    val options = languageOptions()
    val selectedTag = viewModel.selectedLanguageTag
    val selected = options.firstOrNull { it.tag == selectedTag } ?: options.first()

    Dropdown(
        modifier = Modifier
            .width(200.dp)
            .height(AdbrowserTheme.DefaultTextFieldHeight),
        menuContent = {
            options.forEach { language ->
                selectableItem(
                    selected = language.tag == selectedTag,
                    onClick = { viewModel.selectedLanguageTag = language.tag }
                ) { Text(language.displayName) }
            }
        }
    ) {
        Text(selected.displayName)
    }
}

@Composable
private fun preferencesStatusMessage(status: PreferencesStageModel.Status): String = when (status) {
    PreferencesStageModel.Status.None -> ""
    PreferencesStageModel.Status.SidebarSpacingReset -> strings.preferencesStatusSidebarSpacingReset
    PreferencesStageModel.Status.FileColumnWidthsReset -> strings.preferencesStatusFileColumnWidthsReset
    PreferencesStageModel.Status.MainWindowBoundsReset -> strings.preferencesStatusMainWindowBoundsReset
    PreferencesStageModel.Status.PreferencesSaved -> strings.statusPreferencesSaved
    PreferencesStageModel.Status.Cancelled -> strings.preferencesStatusCancelled
    PreferencesStageModel.Status.AdbPathNotFound -> strings.preferencesStatusAdbPathNotFound
    PreferencesStageModel.Status.AdbExecutableInvalid -> strings.preferencesStatusAdbExecutableInvalid
    PreferencesStageModel.Status.AdbPathEmpty -> strings.preferencesStatusAdbPathEmpty
    is PreferencesStageModel.Status.Failed -> {
        val reason = status.reason?.takeIf { it.isNotBlank() } ?: strings.commonUnknownError
        "${strings.preferencesStatusFailedPrefix}$reason"
    }
}

private val TabLabelFontSize = 20.sp
private val TabLabelFontWeight = FontWeight.Normal
private val PanelPadding = PaddingValues(14.dp)

private val StatusErrorColor = Color(0xFFB94747)