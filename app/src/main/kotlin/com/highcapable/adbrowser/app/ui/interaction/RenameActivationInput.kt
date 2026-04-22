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
 * This file is created by fankes on 2026/4/22.
 */
@file:Suppress("AssignedValueIsNeverRead", "KotlinConstantConditions")

package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Handles the desktop-file-manager gesture where a second tap on the selected label activates
 * rename, while double-tap still opens the entry.
 */
fun Modifier.onRenameActivationInput(
    enabled: Boolean,
    key: Any,
    shouldHandle: () -> Boolean = { true },
    onRename: () -> Unit,
    onOpen: () -> Unit
): Modifier = if (enabled)
    pointerInput(key, enabled) {
        var allowCurrentGesture = false

        detectTapGestures(
            onPress = {
                // Decide on pointer-down whether this gesture belongs to the file item. If the
                // press is only being used to dismiss an open context menu, later tap callbacks
                // must not re-check mutable outer state and accidentally enter rename mode.
                allowCurrentGesture = shouldHandle()
                tryAwaitRelease()
            },
            onTap = {
                if (!allowCurrentGesture) return@detectTapGestures
                onRename()
            },
            onDoubleTap = {
                if (!allowCurrentGesture) return@detectTapGestures
                onOpen()
            }
        )
    }
else this