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
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.frontend.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.frontend.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.frontend.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import org.jetbrains.jewel.ui.component.Text
import kotlin.math.max

@Composable
fun FileIconItem(
    item: DeviceFileItem,
    selected: Boolean,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    onHitBoundsChanged: (List<Rect>) -> Unit = {},
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val colors = AdbrowserTheme.colors
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }

    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var iconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var bridgeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
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

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            containerCoordinates = coordinates
        },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = contentModifier
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            fun Modifier.hitTarget(
                targetCoordinates: () -> LayoutCoordinates?,
                onPositioned: (LayoutCoordinates, Rect) -> Unit
            ) = clip(RoundedCornerShape(8.dp))
                .hoverable(interactionSource = interactionSource)
                .onGloballyPositioned { coordinates ->
                    onPositioned(coordinates, coordinates.boundsInRoot())
                }
                .onSecondaryPress(pass = PointerEventPass.Initial) { position ->
                    val target = targetCoordinates()
                    val container = containerCoordinates
                    val translatedPosition = if (target != null && container != null)
                        container.localPositionOf(target, position)
                    else position

                    onSecondaryClick(translatedPosition)
                }
                .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) { event ->
                    if (!event.buttons.isPrimaryPressed) return@onPointerEvent

                    event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
                    pressed = true
                    onPrimaryClick(
                        event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed,
                        event.keyboardModifiers.isShiftPressed
                    )
                }
                .pointerInput(item) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            pressed = false
                        },
                        onDoubleTap = { currentOnDoubleClick() }
                    )
                }

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
                    .hitTarget(targetCoordinates = { textCoordinates }) { coordinates, bounds ->
                        textCoordinates = coordinates
                        textBoundsInRoot = bounds
                        onHitBoundsChanged(listOfNotNull(iconBoundsInRoot, bridgeBoundsInRoot, textBoundsInRoot))
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(textHighlight)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.name,
                    color = foreground,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        overlay()
    }
}

private const val ItemContentColorAlpha = 0.15f