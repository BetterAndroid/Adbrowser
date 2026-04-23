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
 * This file is created by fankes on 2026/4/23.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Arms label-driven rename only after the current selection has been stable for a short window.
 *
 * Without this delay, an unselected entry can be selected by the first click of a double-click
 * sequence, then immediately switch its label into rename mode before the second click has a
 * chance to be recognized as "open". Existing steady-state selections stay armed immediately.
 */
@Composable
fun rememberRenameActivationEnabled(
    key: Any? = Unit,
    canTapLabelToRename: Boolean,
    isInlineRenaming: Boolean
): Boolean {
    val eligible = canTapLabelToRename && !isInlineRenaming
    var wasEligible by remember(key) { mutableStateOf(eligible) }
    var renameActivationEnabled by remember(key) { mutableStateOf(eligible) }

    LaunchedEffect(eligible) {
        when {
            !eligible -> {
                renameActivationEnabled = false
                wasEligible = false
            }
            !wasEligible -> {
                // Give the outer row/card one double-click window to consume the "select, then
                // open" sequence before the label starts treating clicks as rename activation.
                renameActivationEnabled = false
                delay(RenameActivationDelayMillis)
                renameActivationEnabled = true
                wasEligible = true
            }
            else -> renameActivationEnabled = true
        }
    }

    return renameActivationEnabled
}

private const val RenameActivationDelayMillis = 350L