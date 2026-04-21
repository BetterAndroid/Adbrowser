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
 * This file is created by fankes on 2026/4/8.
 */
@file:Suppress("AssignedValueIsNeverRead", "COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.foundation.rememberInlineRenameWidth
import com.highcapable.adbrowser.app.ui.interaction.onPressRelease
import com.highcapable.adbrowser.app.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.app.ui.interaction.onSelectionPrimaryPress
import com.highcapable.adbrowser.app.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.DeviceFileItem
import org.jetbrains.jewel.ui.component.Text
import kotlin.math.max

@Composable
fun FileIconItem(
    item: DeviceFileItem,
    displayName: String,
    selected: Boolean,
    isInlineRenaming: Boolean,
    inlineRenameInput: TextFieldState,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    onConfirmInlineRename: (String) -> Boolean,
    onCancelInlineRename: () -> Unit,
    onHitBoundsChanged: (List<Rect>) -> Unit = {},
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val colors = AdbrowserTheme.colors
    val fontSize = AdbrowserTheme.DefaultItemFontSize

    val density = LocalDensity.current
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }

    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var iconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var bridgeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var editorBoundsInRoot by remember(item) { mutableStateOf<Rect?>(null) }
    var iconBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var bridgeBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var textBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )
    val iconHighlight = when {
        selected && pressed -> colors.primaryAccentPressed.copy(alpha = ItemContentColorAlpha)
        selected -> colors.primaryAccent.copy(alpha = ItemContentColorAlpha)
        pressed -> colors.panelBorder
        else -> Color.Transparent
    }
    val textHighlight = when {
        selected -> background
        else -> Color.Transparent
    }
    val foreground = if (selected) Color.White else Color.Unspecified

    fun Modifier.hitTarget(
        targetCoordinates: () -> LayoutCoordinates?,
        onPositioned: (LayoutCoordinates, Rect) -> Unit
    ) = clip(RoundedCornerShape(8.dp))
        .onGloballyPositioned { coordinates ->
            onPositioned(coordinates, coordinates.boundsInRoot())
        }
        .hoverable(interactionSource = interactionSource)
        .onSecondaryPress(
            pass = PointerEventPass.Initial,
            shouldHandle = { position ->
                val target = targetCoordinates()
                val editorBounds = editorBoundsInRoot
                if (!isInlineRenaming || target == null || editorBounds == null) true
                else !editorBounds.contains(target.localToRoot(position))
            }
        ) { position ->
            val target = targetCoordinates()
            val container = containerCoordinates
            val translatedPosition = if (target != null && container != null)
                container.localPositionOf(target, position)
            else position

            if (isInlineRenaming) onCancelInlineRename()
            onSecondaryClick(translatedPosition)
        }
        .then(
            if (!isInlineRenaming)
                Modifier.onSelectionPrimaryPress(onPressedChange = { pressed = it }) { modifiers ->
                    onPrimaryClick(modifiers.appendSelection, modifiers.rangeSelection)
                }.onPressRelease(
                    key = item,
                    onPressedChange = { pressed = it },
                    onDoubleTap = currentOnDoubleClick
                )
            else Modifier
        )

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            containerCoordinates = coordinates
        },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = contentModifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .hitTarget(targetCoordinates = { iconCoordinates }) { coordinates, bounds ->
                        iconCoordinates = coordinates
                        iconBoundsInRoot = bounds
                        onHitBoundsChanged(listOfNotNull(iconBoundsInRoot, bridgeBoundsInRoot, textBoundsInRoot))
                    }
                    .background(iconHighlight)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                FileEntryIcon(
                    item = item,
                    size = 34.dp
                )
            }
            Box(
                modifier = Modifier
                    .width(
                        with(density) {
                            max(
                                iconBoundsInRoot?.width ?: 0f,
                                textBoundsInRoot?.width ?: 0f
                            ).toDp()
                        }
                    )
                    .height(4.dp)
                    .hitTarget(targetCoordinates = { bridgeCoordinates }) { coordinates, bounds ->
                        bridgeCoordinates = coordinates
                        bridgeBoundsInRoot = bounds
                        onHitBoundsChanged(listOfNotNull(iconBoundsInRoot, bridgeBoundsInRoot, textBoundsInRoot))
                    }
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(textHighlight)
                    .hitTarget(targetCoordinates = { textCoordinates }) { coordinates, bounds ->
                        textCoordinates = coordinates
                        textBoundsInRoot = bounds
                        onHitBoundsChanged(listOfNotNull(iconBoundsInRoot, bridgeBoundsInRoot, textBoundsInRoot))
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BoxWithConstraints {
                    val inlineRenameWidth = rememberInlineRenameWidth(
                        text = displayName,
                        maxWidth = maxWidth,
                        maxLines = 2,
                        softWrap = true
                    )

                    Text(
                        text = displayName,
                        color = foreground,
                        fontSize = fontSize,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (isInlineRenaming)
                        InlineRenameField(
                            state = inlineRenameInput,
                            sessionKey = item.path to item.name,
                            onConfirm = onConfirmInlineRename,
                            onCancel = onCancelInlineRename,
                            modifier = Modifier.width(inlineRenameWidth),
                            onBoundsInRootChanged = { editorBoundsInRoot = it },
                            centeredMultiline = true
                        )
                }
            }
        }
        overlay()
    }
}

private const val ItemContentColorAlpha = 0.15f