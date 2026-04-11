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
 * This file is created by fankes on 2026/4/4.
 */
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.highcapable.betterandroid.compose.extension.ui.componentState
import com.highcapable.betterandroid.compose.extension.ui.orNull
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icon.IconKey

@Composable
fun ContentIcon( 
    key: IconKey,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentDescription: String = "Content icon",
    modifier: Modifier = Modifier,
    iconClass: Class<*> = key.iconClass,
    tint: Color = Color.Unspecified
) {
    val contentColor = JewelTheme.contentColor

    Icon(
        key = key,
        contentDescription = contentDescription,
        modifier = modifier.componentState(enabled),
        iconClass = iconClass,
        tint = if (selected) Color.White else tint.orNull() ?: contentColor
    )
}