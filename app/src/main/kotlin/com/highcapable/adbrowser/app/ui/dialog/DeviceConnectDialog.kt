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
 * This file is created by fankes on 2026/4/18.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.dialog

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.cl.LocalAppState
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.component.ContentIcon
import com.highcapable.adbrowser.app.ui.dialog.base.DialogActionRow
import com.highcapable.adbrowser.app.ui.dialog.base.DialogScaffold
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.DeviceConnectDialogModel
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.EditableComboBox
import org.jetbrains.jewel.ui.component.PopupManager
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.comboBoxStyle
import java.awt.Window

@Composable
fun DeviceConnectDialog(
    onCloseRequest: () -> Unit,
    ownerWindow: Window? = null
) {
    val colors = AdbrowserTheme.colors

    val appState = LocalAppState.current
    val viewModel = remember(appState) { DeviceConnectDialogModel(appState) }
    val focusRequester = remember { FocusRequester() }
    val popupManager = remember { PopupManager(name = "DeviceConnectAddressHistory") }

    DisposableEffect(viewModel) {
        onDispose(viewModel::dispose)
    }

    LaunchedEffect(viewModel) {
        snapshotFlow { viewModel.addressState.text.toString() }
            .distinctUntilChanged()
            .collect { viewModel.onAddressChanged() }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(viewModel.isConnecting) {
        if (viewModel.isConnecting) popupManager.setPopupVisible(false)
    }

    val leadingText = when (val status = viewModel.status) {
        DeviceConnectDialogModel.Status.None -> ""
        DeviceConnectDialogModel.Status.Connecting -> strings.dialogDeviceConnectConnecting
        is DeviceConnectDialogModel.Status.Failed -> status.reason
            ?.takeIf { it.isNotBlank() }
            ?: strings.commonUnknownError
    }
    val leadingColor = when (viewModel.status) {
        is DeviceConnectDialogModel.Status.Failed -> JewelTheme.globalColors.text.error
        else -> JewelTheme.contentColor
    }
    val suggestions = viewModel.addressSuggestions

    DialogScaffold(
        title = strings.dialogDeviceConnectTitle,
        onCloseRequest = { viewModel.cancelAndClose(onCloseRequest) },
        horizontalAlignment = Alignment.CenterHorizontally,
        ownerWindow = ownerWindow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            ContentIcon(
                key = AppIcons.Internet,
                tint = colors.primaryAccent,
                modifier = Modifier.size(LogoIconSize)
            )
            ContentIcon(
                key = AppIcons.Ellipsis,
                tint = colors.primaryAccent,
                modifier = Modifier
                    .size(DotIconSize)
                    .alpha(0.5f)
            )
            ContentIcon(
                key = AppIcons.Device,
                tint = colors.primaryAccent,
                modifier = Modifier.size(LogoIconSize)
            )
        }
        DeviceAddressComboBox(
            viewModel = viewModel,
            popupManager = popupManager,
            focusRequester = focusRequester,
            suggestions = suggestions,
            onConfirm = { viewModel.connect(onCloseRequest = onCloseRequest) }
        )
        DialogActionRow(
            primaryText = strings.dialogCommonOk,
            onPrimary = {
                viewModel.connect(onCloseRequest = onCloseRequest)
            },
            secondaryText = strings.dialogCommonCancel,
            onSecondary = { viewModel.cancelAndClose(onCloseRequest) },
            primaryEnabled = viewModel.canConfirm,
            leadingText = leadingText,
            leadingTextColor = leadingColor
        )
    }
}

@Composable
private fun DeviceAddressComboBox(
    viewModel: DeviceConnectDialogModel,
    popupManager: PopupManager,
    focusRequester: FocusRequester,
    suggestions: List<String>,
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current
    val placeholderPadding = JewelTheme.comboBoxStyle.metrics.contentPadding
    val currentText = viewModel.addressState.text.toString()
    val hasExactSuggestionMatch = suggestions.any { it.equals(currentText, ignoreCase = true) }
    val pendingSuggestion = suggestions.firstOrNull()
        ?.takeIf { !it.equals(currentText, ignoreCase = true) }
    var comboBoxWidth by remember { mutableStateOf(Dp.Unspecified) }
    var suppressNextAutoPopup by remember { mutableStateOf(false) }

    // Auto-open suggestions while the user is typing a matching prefix. Programmatic edits such as
    // accepting a suggestion opt out once so the popup can close cleanly instead of reopening from
    // the text change they trigger.
    LaunchedEffect(currentText, suggestions, viewModel.isConnecting) {
        when {
            viewModel.isConnecting -> popupManager.setPopupVisible(false)
            suppressNextAutoPopup -> suppressNextAutoPopup = false
            currentText.isBlank() || suggestions.isEmpty() || hasExactSuggestionMatch ->
                popupManager.setPopupVisible(false)
            else -> popupManager.setPopupVisible(true)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        EditableComboBox(
            textFieldState = viewModel.addressState,
            enabled = !viewModel.isConnecting,
            popupManager = popupManager,
            onEnterPress = {
                when {
                    pendingSuggestion != null -> {
                        suppressNextAutoPopup = true
                        viewModel.selectAddressSuggestion(pendingSuggestion)
                        popupManager.setPopupVisible(false)
                        focusRequester.requestFocus()
                    }
                    viewModel.canConfirm -> onConfirm()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AdbrowserTheme.DefaultTextFieldHeight)
                .focusRequester(focusRequester)
                .onSizeChanged { comboBoxWidth = with(density) { it.width.toDp() } }
                .pointerInput(viewModel.isConnecting, suggestions) {
                    if (viewModel.isConnecting) return@pointerInput

                    awaitEachGesture {
                        val initialVisible = popupManager.isPopupVisible.value
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && !initialVisible && suggestions.isNotEmpty()) {
                            popupManager.setPopupVisible(true)
                        }
                    }
                },
            popupModifier = Modifier.then(
                if (comboBoxWidth != Dp.Unspecified) Modifier.width(comboBoxWidth) else Modifier
            ),
            maxPopupHeight = MaxPopupHeight,
            maxPopupWidth = comboBoxWidth,
            popupContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    suggestions.forEach { suggestion ->
                        AddressSuggestionItem(
                            text = suggestion,
                            selected = suggestion.equals(currentText, ignoreCase = true),
                            onSelected = {
                                suppressNextAutoPopup = true
                                viewModel.selectAddressSuggestion(suggestion)
                                popupManager.setPopupVisible(false)
                                focusRequester.requestFocus()
                            }
                        )
                    }
                }
            }
        )
        if (currentText.isBlank())
            Text(
                text = strings.dialogDeviceConnectPlaceholder,
                color = JewelTheme.globalColors.text.info,
                modifier = Modifier
                    .padding(placeholderPadding)
                    .fillMaxWidth()
            )
    }
}

@Composable
private fun AddressSuggestionItem(
    text: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }

    SimpleListItem(
        text = text,
        selected = selected || hovered,
        active = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .onHover { hovered = it }
            .pointerInput(onSelected) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation()
                    if (up != null) onSelected()
                }
            }
    )
}

private val LogoIconSize = 40.dp
private val DotIconSize = 25.dp

private val MaxPopupHeight = 180.dp