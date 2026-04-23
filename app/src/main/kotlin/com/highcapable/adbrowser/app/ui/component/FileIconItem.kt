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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.interaction.PointerBoundsGuard
import com.highcapable.adbrowser.app.ui.interaction.onPressRelease
import com.highcapable.adbrowser.app.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.app.ui.interaction.onSelectionPrimaryPress
import com.highcapable.adbrowser.app.ui.interaction.rememberOrderedBoundsTracker
import com.highcapable.adbrowser.app.ui.interaction.rememberPointerBoundsGuard
import com.highcapable.adbrowser.app.ui.interaction.rememberRenameActivationEnabled
import com.highcapable.adbrowser.app.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.DeviceFileItem
import kotlin.math.max

@Composable
fun FileIconItem(
    item: DeviceFileItem,
    displayName: String,
    selected: Boolean,
    canTapLabelToRename: Boolean,
    isInlineRenaming: Boolean,
    inlineRenameInput: TextFieldState,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    shouldHandlePrimaryInteraction: () -> Boolean = { true },
    onBeginInlineRename: () -> Unit,
    onConfirmInlineRename: (String) -> Boolean,
    onCancelInlineRename: () -> Unit,
    shouldRestoreFocusOnInlineRenameFailure: () -> Boolean = { true },
    onHitBoundsChanged: (List<Rect>) -> Unit = {},
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val colors = AdbrowserTheme.colors

    val density = LocalDensity.current
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }

    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var iconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var connectorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val editorBoundsGuard = rememberPointerBoundsGuard(item)
    val hitBoundsTracker = rememberOrderedBoundsTracker(
        key = item,
        order = FileIconHitRegion.entries,
        onBoundsChanged = onHitBoundsChanged
    )
    val trackerWidth = with(density) {
        max(
            hitBoundsTracker.boundsOf(FileIconHitRegion.Icon)?.width ?: 0f,
            hitBoundsTracker.boundsOf(FileIconHitRegion.Text)?.width ?: 0f
        ).toDp()
    }

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
    val renameActivationEnabled = rememberRenameActivationEnabled(
        key = item,
        canTapLabelToRename = canTapLabelToRename,
        isInlineRenaming = isInlineRenaming
    )

    DisposableEffect(item) {
        onDispose {
            FileIconHitRegion.entries.forEach(hitBoundsTracker::clear)
        }
    }

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
                    .fileIconHitTarget(
                        item = item,
                        interactionSource = interactionSource,
                        isInlineRenaming = isInlineRenaming,
                        targetCoordinates = { iconCoordinates },
                        containerCoordinates = { containerCoordinates },
                        editorBoundsGuard = editorBoundsGuard,
                        shouldHandlePrimaryInput = { shouldHandlePrimaryInteraction() },
                        onPressedChange = { pressed = it },
                        onPrimaryClick = onPrimaryClick,
                        onDoubleClick = currentOnDoubleClick,
                        onSecondaryClick = onSecondaryClick,
                        onBoundsChanged = { coordinates, bounds ->
                            iconCoordinates = coordinates
                            hitBoundsTracker.update(FileIconHitRegion.Icon, coordinates, bounds)
                        }
                    )
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
                    .width(trackerWidth)
                    .height(IconLabelConnectorHeight)
                    // This connector keeps the gap between icon and label inside the same hit
                    // corridor, so users can click the visual "stem" as part of the item instead
                    // of accidentally falling through to blank-area behavior.
                    .fileIconHitTarget(
                        item = item,
                        interactionSource = interactionSource,
                        isInlineRenaming = isInlineRenaming,
                        targetCoordinates = { connectorCoordinates },
                        containerCoordinates = { containerCoordinates },
                        editorBoundsGuard = editorBoundsGuard,
                        shouldHandlePrimaryInput = { shouldHandlePrimaryInteraction() },
                        onPressedChange = { pressed = it },
                        onPrimaryClick = onPrimaryClick,
                        onDoubleClick = currentOnDoubleClick,
                        onSecondaryClick = onSecondaryClick,
                        onBoundsChanged = { coordinates, bounds ->
                            connectorCoordinates = coordinates
                            hitBoundsTracker.update(FileIconHitRegion.Connector, coordinates, bounds)
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(textHighlight)
                    .fileIconHitTarget(
                        item = item,
                        interactionSource = interactionSource,
                        isInlineRenaming = isInlineRenaming,
                        targetCoordinates = { textCoordinates },
                        containerCoordinates = { containerCoordinates },
                        editorBoundsGuard = editorBoundsGuard,
                        shouldHandlePrimaryInput = {
                            !renameActivationEnabled && shouldHandlePrimaryInteraction()
                        },
                        onPressedChange = { pressed = it },
                        onPrimaryClick = onPrimaryClick,
                        onDoubleClick = currentOnDoubleClick,
                        onSecondaryClick = onSecondaryClick,
                        onBoundsChanged = { coordinates, bounds ->
                            textCoordinates = coordinates
                            hitBoundsTracker.update(FileIconHitRegion.Text, coordinates, bounds)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                FileIconNameSection(
                    item = item,
                    displayName = displayName,
                    selected = selected,
                    isInlineRenaming = isInlineRenaming,
                    canTapLabelToRename = renameActivationEnabled,
                    inlineRenameInput = inlineRenameInput,
                    editorBoundsGuard = editorBoundsGuard,
                    shouldHandlePrimaryInteraction = shouldHandlePrimaryInteraction,
                    onBeginInlineRename = onBeginInlineRename,
                    onOpen = currentOnDoubleClick,
                    onConfirmInlineRename = onConfirmInlineRename,
                    onCancelInlineRename = onCancelInlineRename,
                    shouldRestoreFocusOnInlineRenameFailure = shouldRestoreFocusOnInlineRenameFailure
                )
            }
        }
        overlay()
    }
}

private enum class FileIconHitRegion {
    Icon,
    Connector,
    Text
}

private fun Modifier.fileIconHitTarget(
    item: DeviceFileItem,
    interactionSource: MutableInteractionSource,
    isInlineRenaming: Boolean,
    targetCoordinates: () -> LayoutCoordinates?,
    containerCoordinates: () -> LayoutCoordinates?,
    editorBoundsGuard: PointerBoundsGuard,
    shouldHandlePrimaryInput: (Offset) -> Boolean,
    onPressedChange: (Boolean) -> Unit,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    onBoundsChanged: (LayoutCoordinates, Rect) -> Unit
): Modifier = clip(RoundedCornerShape(8.dp))
    .onGloballyPositioned { coordinates ->
        onBoundsChanged(coordinates, coordinates.boundsInRoot())
    }
    .hoverable(interactionSource = interactionSource)
    .onSecondaryPress(
        pass = PointerEventPass.Initial,
        shouldHandle = { position ->
            val target = targetCoordinates() ?: return@onSecondaryPress true
            if (!isInlineRenaming) return@onSecondaryPress true

            // While inline rename is active, editor-local context menus must stay inside the text
            // field. Outer item menus are only allowed when the pointer press lands outside the
            // editor bounds in root coordinates.
            editorBoundsGuard.shouldHandleRoot(target.localToRoot(position))
        }
    ) { position ->
        val target = targetCoordinates()
        val container = containerCoordinates()
        val translatedPosition = if (target != null && container != null)
            container.localPositionOf(target, position)
        else position

        onSecondaryClick(translatedPosition)
    }
    .then(
        if (!isInlineRenaming)
            Modifier.onSelectionPrimaryPress(
                shouldHandle = shouldHandlePrimaryInput,
                onPressedChange = onPressedChange
            ) { modifiers ->
                onPrimaryClick(modifiers.appendSelection, modifiers.rangeSelection)
            }.onPressRelease(
                key = item,
                shouldHandle = shouldHandlePrimaryInput,
                onPressedChange = onPressedChange,
                onDoubleTap = onDoubleClick
            )
        else Modifier
    )

private const val ItemContentColorAlpha = 0.15f
private val IconLabelConnectorHeight = 4.dp