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
 * This file is created by fankes on 2026/4/20.
 */
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.interaction.LocalPrimaryActionController
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ButtonActionRow(
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    tertiaryText: String? = null,
    onTertiary: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    checkboxText: String? = null,
    checkboxChecked: Boolean = false,
    onCheckboxCheckedChange: ((Boolean) -> Unit)? = null,
    leadingText: String = "",
    leadingTextColor: Color = JewelTheme.contentColor,
    primaryEnabled: Boolean = true,
    tertiaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    checkboxEnabled: Boolean = true,
    primaryButtonWidth: Dp = 80.dp,
    tertiaryButtonWidth: Dp = 80.dp,
    secondaryButtonWidth: Dp = 80.dp
) {
    val primaryActionController = LocalPrimaryActionController.current

    SideEffect {
        primaryActionController?.update(onPrimary, primaryEnabled)
    }

    DisposableEffect(primaryActionController) {
        onDispose { primaryActionController?.clear() }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (checkboxText != null && onCheckboxCheckedChange != null) {
                CheckboxRow(
                    text = checkboxText,
                    checked = checkboxChecked,
                    onCheckedChange = onCheckboxCheckedChange,
                    enabled = checkboxEnabled
                )
                if (leadingText.isNotEmpty()) Spacer(Modifier.width(12.dp))
            }
            if (leadingText.isNotEmpty())
                Text(
                    text = leadingText,
                    color = leadingTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
        }
        if (tertiaryText != null && onTertiary != null) {
            OutlinedButton(
                enabled = tertiaryEnabled,
                onClick = onTertiary,
                modifier = Modifier.width(tertiaryButtonWidth)
            ) {
                Text(tertiaryText)
            }
            Spacer(Modifier.width(12.dp))
        }
        if (secondaryText != null && onSecondary != null) {
            OutlinedButton(
                enabled = secondaryEnabled,
                onClick = onSecondary,
                modifier = Modifier.width(secondaryButtonWidth)
            ) {
                Text(secondaryText)
            }
            Spacer(Modifier.width(12.dp))
        }
        DefaultButton(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier.width(primaryButtonWidth)
        ) {
            Text(primaryText)
        }
    }
}