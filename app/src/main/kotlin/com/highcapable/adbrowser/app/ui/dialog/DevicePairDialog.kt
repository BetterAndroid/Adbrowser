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
 * This file is created by fankes on 2026/4/19.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.cl.LocalAppState
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.component.ButtonActionRow
import com.highcapable.adbrowser.app.ui.component.ContentIcon
import com.highcapable.adbrowser.app.ui.component.PanelSurface
import com.highcapable.adbrowser.app.ui.component.QrCodePanel
import com.highcapable.adbrowser.app.ui.dialog.base.DialogScaffold
import com.highcapable.adbrowser.app.ui.interaction.ProvidePrimaryAction
import com.highcapable.adbrowser.app.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.DevicePairDialogModel
import com.highcapable.adbrowser.core.adb.model.pairing.PairingDevice
import com.highcapable.adbrowser.core.common.utils.extension.formatWithArgs
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.awt.Window

@Composable
fun DevicePairDialog(
    onCloseRequest: () -> Unit,
    ownerWindow: Window? = null
) {
    val colors = AdbrowserTheme.colors

    val appState = LocalAppState.current
    val viewModel = remember(appState) { DevicePairDialogModel(appState) }

    DisposableEffect(viewModel) {
        onDispose(viewModel::dispose)
    }

    LaunchedEffect(viewModel) {
        viewModel.bind(onPairingCompleted = onCloseRequest)
    }

    val qrError = viewModel.qrError?.takeIf { it.isNotBlank() }
    val leadingText = when (val status = viewModel.status) {
        DevicePairDialogModel.Status.None,
        DevicePairDialogModel.Status.WaitingForQr -> qrError.orEmpty()
        DevicePairDialogModel.Status.PreparingQr -> strings.dialogDevicePairPreparingQr
        DevicePairDialogModel.Status.Pairing -> strings.dialogDevicePairPairing
        is DevicePairDialogModel.Status.Failed -> status.reason
            ?.takeIf { it.isNotBlank() }
            ?: strings.commonUnknownError
    }
    val leadingColor = when (viewModel.status) {
        is DevicePairDialogModel.Status.Failed -> JewelTheme.globalColors.text.error
        else -> if (qrError != null) JewelTheme.globalColors.text.error else colors.pathBreadcrumbForeground
    }

    DialogScaffold(
        title = strings.dialogDevicePairTitle,
        onCloseRequest = {
            if (viewModel.canClose) viewModel.cancelAndClose(onCloseRequest)
        },
        ownerWindow = ownerWindow,
        contentPadding = PaddingValues(12.dp),
        width = 580.dp,
        height = 520.dp
    ) {
        DevicePairTabs(viewModel)

        Column(modifier = Modifier.fillMaxSize()) {
            when (viewModel.currentTab) {
                DevicePairDialogModel.Tab.QrCode -> QrPairTab(viewModel)
                DevicePairDialogModel.Tab.PairingCode -> ManualPairTab(viewModel)
            }
            Spacer(Modifier.height(12.dp))
            ButtonActionRow(
                primaryText = strings.dialogPropertiesClose,
                onPrimary = { viewModel.cancelAndClose(onCloseRequest) },
                primaryEnabled = viewModel.canClose,
                leadingText = leadingText,
                leadingTextColor = leadingColor,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            )
        }
    }

    viewModel.activePairingDevice?.let { device ->
        PairingCodeDialog(
            viewModel = viewModel,
            device = device,
            ownerWindow = ownerWindow
        )
    }
}

@Composable
private fun DevicePairTabs(viewModel: DevicePairDialogModel) {
    val tabs = listOf(
        TabData.Default(
            selected = viewModel.currentTab == DevicePairDialogModel.Tab.QrCode,
            closable = false,
            onClick = { viewModel.selectTab(DevicePairDialogModel.Tab.QrCode) },
            content = { tabState ->
                SimpleTabContent(
                    state = tabState,
                    label = {
                        Text(
                            text = strings.dialogDevicePairTabQr,
                            fontSize = PairTabLabelFontSize,
                            fontWeight = PairTabLabelFontWeight
                        )
                    }
                )
            }
        ),
        TabData.Default(
            selected = viewModel.currentTab == DevicePairDialogModel.Tab.PairingCode,
            closable = false,
            onClick = { viewModel.selectTab(DevicePairDialogModel.Tab.PairingCode) },
            content = { tabState ->
                SimpleTabContent(
                    state = tabState,
                    label = {
                        Text(
                            text = strings.dialogDevicePairTabCode,
                            fontSize = PairTabLabelFontSize,
                            fontWeight = PairTabLabelFontWeight
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
private fun ColumnScope.QrPairTab(viewModel: DevicePairDialogModel) {
    val colors = AdbrowserTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val refreshBackground = resolveListItemBackground(
        colors = colors,
        selected = false,
        hovered = hovered,
        pressed = pressed
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = strings.dialogDevicePairQrInstructions,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Box(
            modifier = Modifier
                .background(refreshBackground, RoundedCornerShape(10.dp))
                .graphicsLayer {
                    val scale = if (pressed) 0.985f else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .hoverable(interactionSource = interactionSource)
                .pointerInput(viewModel.canRefreshQr) {
                    detectTapGestures(
                        onPress = {
                            if (!viewModel.canRefreshQr) return@detectTapGestures

                            pressed = true
                            val released = tryAwaitRelease()
                            pressed = false

                            if (released) viewModel.refreshQrSession()
                        }
                    )
                }
        ) {
            PanelSurface(
                padding = PaddingValues(14.dp)
            ) {
                if (viewModel.qrSession != null)
                    QrCodePanel(
                        content = viewModel.qrSession?.qrContent.orEmpty(),
                        modifier = Modifier.size(220.dp)
                    )
                else
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicatorBig()
                    }
            }
        }
        Text(
            text = strings.dialogDevicePairQrFooter,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            color = JewelTheme.globalColors.text.info
        )
    }
}

@Composable
private fun ColumnScope.ManualPairTab(viewModel: DevicePairDialogModel) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.dialogDevicePairManualListTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (viewModel.pairingDevices.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "(${viewModel.pairingDevices.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (viewModel.isObservingPairingDevices)
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
        PanelSurface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            clipContent = true
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(viewModel.pairingDevices, key = { it.serviceName + it.address }) { device ->
                        PairingDeviceRow(
                            item = device,
                            enabled = !viewModel.isManualPairing,
                            onClick = { viewModel.openPairingCodeDialog(device) }
                        )
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(250.dp)
                )
                if (viewModel.pairingDevices.isEmpty())
                    Text(
                        text = strings.dialogDevicePairManualSearching,
                        color = JewelTheme.globalColors.text.info,
                        modifier = Modifier.align(Alignment.Center)
                    )
            }
        }
        Text(
            text = strings.dialogDevicePairManualFooter,
            lineHeight = 18.sp,
            color = JewelTheme.globalColors.text.info,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PairingDeviceRow(
    item: PairingDevice,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = AdbrowserTheme.colors

    var hovered by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val background = resolveListItemBackground(
        colors = colors,
        selected = false,
        hovered = hovered,
        pressed = pressed
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .onHover {
                hovered = it
            }
            .pointerInput(enabled, onClick) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!enabled) return@awaitEachGesture

                    pressed = true
                    val up = waitForUpOrCancellation()
                    pressed = false

                    if (up != null) {
                        // The modal pairing-code dialog steals pointer routing immediately after we
                        // open it, so we clear hover eagerly here instead of waiting for a hover-exit
                        // callback that may never arrive.
                        hovered = false
                        onClick()
                    }
                }
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContentIcon(
            key = AppIcons.Device,
            tint = colors.primaryAccent
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.serviceName,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            )
            Text(
                text = item.address,
                fontSize = 11.sp,
                color = JewelTheme.globalColors.text.info,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PairingCodeDialog(
    viewModel: DevicePairDialogModel,
    device: PairingDevice,
    ownerWindow: Window?
) {
    val codeStates = remember(device) { List(PairingCodeLength) { TextFieldState("") } }
    val focusRequesters = remember(device) { List(PairingCodeLength) { FocusRequester() } }
    val code = remember(codeStates) {
        {
            codeStates.joinToString(separator = "") { it.text.toString().take(1) }
        }
    }
    val canConfirm = code().length == PairingCodeLength && !viewModel.isManualPairing
    val centeredTextStyle = TextStyle(
        color = JewelTheme.contentColor,
        fontSize = 18.sp,
        textAlign = TextAlign.Center
    )

    LaunchedEffect(device) {
        focusRequesters.first().requestFocus()
    }

    codeStates.forEachIndexed { index, state ->
        LaunchedEffect(state) {
            snapshotFlow { state.text.toString() }
                .distinctUntilChanged()
                .collect { raw ->
                    // Jewel's TextField is still a regular text box, so we normalize each cell back
                    // to a single digit and advance focus ourselves to get the native 6-box pairing
                    // code feel without introducing a custom input control.
                    val normalized = raw.filter(Char::isDigit).takeLast(1)
                    if (normalized != raw) {
                        state.edit { replace(0, length, normalized) }
                        return@collect
                    }

                    if (normalized.isNotEmpty() && index < focusRequesters.lastIndex)
                        focusRequesters[index + 1].requestFocus()
                }
        }
    }

    DialogScaffold(
        title = strings.dialogDevicePairCodeTitle.formatWithArgs(device.address),
        onCloseRequest = { if (!viewModel.isManualPairing) viewModel.dismissPairingCodeDialog() },
        ownerWindow = ownerWindow,
        width = 420.dp
    ) {
        ProvidePrimaryAction(window) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.dialogDevicePairCodePrompt,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    codeStates.forEachIndexed { index, state ->
                        TextField(
                            state = state,
                            enabled = !viewModel.isManualPairing,
                            textStyle = centeredTextStyle,
                            modifier = Modifier
                                .width(35.dp)
                                .height(AdbrowserTheme.DefaultTextFieldHeight)
                                .focusRequester(focusRequesters[index])
                                .onPreviewKeyEvent { event ->
                                    when {
                                        event.type == KeyEventType.KeyDown && event.key == Key.Backspace -> {
                                            if (state.text.isNotEmpty()) return@onPreviewKeyEvent false
                                            if (index == 0) return@onPreviewKeyEvent false

                                            val previousState = codeStates[index - 1]
                                            previousState.edit { replace(0, length, "") }
                                            focusRequesters[index - 1].requestFocus()
                                            true
                                        }
                                        event.type == KeyEventType.KeyDown &&
                                            (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                                            canConfirm -> {
                                            viewModel.confirmManualPair(code())
                                            true
                                        }
                                        else -> false
                                    }
                                }
                        )
                    }
                }
                ButtonActionRow(
                    primaryText = strings.dialogCommonOk,
                    onPrimary = { viewModel.confirmManualPair(code()) },
                    secondaryText = strings.dialogCommonCancel,
                    onSecondary = viewModel::dismissPairingCodeDialog,
                    primaryEnabled = canConfirm,
                    secondaryEnabled = !viewModel.isManualPairing,
                    leadingText = viewModel.codeDialogError.orEmpty(),
                    leadingTextColor = JewelTheme.globalColors.text.error
                )
            }
        }
    }
}

private val PairTabLabelFontSize = 14.sp
private val PairTabLabelFontWeight = FontWeight.Medium

private const val PairingCodeLength = 6