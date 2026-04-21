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
package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent

fun Modifier.onSecondaryPress(
    pass: PointerEventPass,
    shouldHandle: (Offset) -> Boolean = { true },
    onSecondaryPress: (Offset) -> Unit
) = onPointerEvent(PointerEventType.Press, pass = pass) { event ->
    if (!event.buttons.isSecondaryPressed) return@onPointerEvent
    if (event.changes.any { it.isConsumed }) return@onPointerEvent

    val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
    if (!shouldHandle(change.position)) return@onPointerEvent
    onSecondaryPress(change.position)
    event.changes.forEach { it.consume() }
}

fun Modifier.onBlankPrimaryPress(
    onPrimaryPress: (Offset) -> Unit
) = onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Final) { event ->
    if (!event.buttons.isPrimaryPressed) return@onPointerEvent
    if (event.changes.any { it.isConsumed }) return@onPointerEvent

    val change = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
    onPrimaryPress(change.position)
}