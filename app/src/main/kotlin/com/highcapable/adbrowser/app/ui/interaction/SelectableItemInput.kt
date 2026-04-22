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
 * This file is created by fankes on 2026/4/21.
 */
package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Normalized modifier-key snapshot for selection gestures.
 */
data class SelectionPressModifiers(
    val appendSelection: Boolean,
    val rangeSelection: Boolean
)

/**
 * Handles the common "press to select" behavior used by list-like rows.
 *
 * Selection is triggered on pointer press instead of click release so the UI mirrors desktop file
 * managers, where selection, right-click follow-up menus, and drag gestures all need the primary
 * item to become active immediately.
 */
fun Modifier.onSelectionPrimaryPress(
    pass: PointerEventPass = PointerEventPass.Initial,
    shouldHandle: (Offset) -> Boolean = { true },
    onPressedChange: ((Boolean) -> Unit)? = null,
    onPrimaryPress: (SelectionPressModifiers) -> Unit
): Modifier = onPointerEvent(PointerEventType.Press, pass = pass) { event ->
    if (!event.buttons.isPrimaryPressed) return@onPointerEvent

    val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
    if (!shouldHandle(change.position)) return@onPointerEvent
    onPressedChange?.invoke(true)
    onPrimaryPress(
        SelectionPressModifiers(
            appendSelection = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed,
            rangeSelection = event.keyboardModifiers.isShiftPressed
        )
    )
}

/**
 * Simpler primary-press handler for rows that do not care about selection modifiers.
 */
fun Modifier.onPrimaryPress(
    pass: PointerEventPass = PointerEventPass.Initial,
    shouldHandle: (Offset) -> Boolean = { true },
    onPressedChange: ((Boolean) -> Unit)? = null,
    onPrimaryPress: () -> Unit
): Modifier = onPointerEvent(PointerEventType.Press, pass = pass) { event ->
    if (!event.buttons.isPrimaryPressed) return@onPointerEvent

    val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
    if (!shouldHandle(change.position)) return@onPointerEvent
    onPressedChange?.invoke(true)
    onPrimaryPress()
}

/**
 * Resets the visual pressed state on release and optionally forwards double-clicks.
 *
 * This stays separate from the press-selection hook above because selection must fire on the first
 * press, while double-click open still needs the second tap callback from Compose's tap detector.
 */
fun Modifier.onPressRelease(
    key: Any,
    shouldHandle: (Offset) -> Boolean = { true },
    onPressedChange: (Boolean) -> Unit,
    onDoubleTap: (() -> Unit)? = null
): Modifier = pointerInput(key) {
    detectTapGestures(
        onPress = { offset ->
            if (!shouldHandle(offset)) return@detectTapGestures
            tryAwaitRelease()
            onPressedChange(false)
        },
        onDoubleTap = { offset ->
            if (!shouldHandle(offset)) return@detectTapGestures
            onDoubleTap?.invoke()
        }
    )
}