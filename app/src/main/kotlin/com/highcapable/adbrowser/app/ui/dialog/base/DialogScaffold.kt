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
 * This file is created by fankes on 2026/4/5.
 */
@file:Suppress("AssignedValueIsNeverRead", "COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.app.ui.dialog.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.window.DialogWindowScope
import com.highcapable.adbrowser.app.ui.component.WindowButtonsSpacing
import com.highcapable.adbrowser.app.ui.component.WindowTitleBar
import com.highcapable.adbrowser.app.ui.input.WindowListener
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.utils.LookAndFeel
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import java.awt.Dialog
import java.awt.GraphicsConfiguration
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import javax.swing.JDialog
import javax.swing.SwingUtilities

@Composable
fun DialogScaffold(
    title: String,
    onCloseRequest: () -> Unit,
    ownerWindow: Window? = null,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    maxWidth: Dp = 460.dp,
    maxHeight: Dp = Dp.Unspecified,
    verticalSpacing: Dp = 12.dp,
    contentPadding: ComponentPadding = ComponentPadding(20.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    titleBarBackgroundColor: Color = Color.Transparent,
    fitsTitleBarHeight: Boolean = true,
    buttonsSpacing: WindowButtonsSpacing = WindowButtonsSpacing.Default,
    content: @Composable DialogWindowScope.() -> Unit
) {
    val currentTitle by rememberUpdatedState(title)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)
    var hasCenteredDialog by remember(ownerWindow) { mutableStateOf(false) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var packedContentSize by remember { mutableStateOf(IntSize.Zero) }

    SwingDialog(
        create = {
            createComposeDialog(ownerWindow).apply {
                defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
                addWindowListener(
                    WindowListener(
                        windowClosing = { currentOnCloseRequest() }
                    )
                )
                LookAndFeel.applyMacOSImmersiveTitleBarStyle(this)
            }
        },
        dispose = ComposeDialog::dispose,
        update = { dialog ->
            dialog.title = currentTitle
            dialog.isResizable = false
            dialog.pack()
            if (!hasCenteredDialog) {
                ownerWindow?.let(dialog::setLocationRelativeTo)
                hasCenteredDialog = true
            }
        }
    ) {
        val colors = AdbrowserTheme.colors

        val density = LocalDensity.current
        val maxDialogSize = rememberDialogMaxSize(window, density, maxWidth, maxHeight)
        val resolvedWidth = width.coerceToAtMostOrNull(maxDialogSize.width)
        val resolvedHeight = height.coerceToAtMostOrNull(maxDialogSize.height)

        // Half the top padding to harmonize spacing with the title bar.
        val compatiblePadding = contentPadding.let {
            if (fitsTitleBarHeight)
                it.copy(top = contentPadding.top / 2)
            else it
        }

        if (contentSize != IntSize.Zero && contentSize != packedContentSize) {
            packedContentSize = contentSize
            SwingUtilities.invokeLater {
                window.pack()
                if (!hasCenteredDialog) {
                    ownerWindow?.let(window::setLocationRelativeTo)
                    hasCenteredDialog = true
                }
            }
        }

        DialogScaffoldLayout(
            modifier = Modifier
                .background(colors.mainBackground)
                .onSizeChanged { contentSize = it }
                .then(if (resolvedWidth != null)
                    Modifier.width(resolvedWidth)
                else Modifier.widthIn(max = maxDialogSize.width))
                .then(if (resolvedHeight != null)
                    Modifier.height(resolvedHeight)
                else Modifier.heightIn(max = maxDialogSize.height)),
            title = title,
            titleBarBackgroundColor = titleBarBackgroundColor,
            buttonsSpacing = buttonsSpacing,
            contentPadding = compatiblePadding,
            verticalSpacing = verticalSpacing,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * Measures the dialog body first, then fits the custom title bar to that body width.
 *
 * This avoids letting the title bar's internal `fillMaxWidth()` force every auto-sized dialog to
 * jump straight to the maximum allowed width.
 */
@Composable
private fun DialogWindowScope.DialogScaffoldLayout(
    modifier: Modifier = Modifier,
    title: String,
    titleBarBackgroundColor: Color,
    buttonsSpacing: WindowButtonsSpacing,
    contentPadding: ComponentPadding,
    verticalSpacing: Dp,
    horizontalAlignment: Alignment.Horizontal,
    content: @Composable DialogWindowScope.() -> Unit
) {
    val showTitleBar = WindowTitleBar.isAvailable
    val titleBarHeightPx = with(LocalDensity.current) { buttonsSpacing.height.roundToPx() }

    Layout(
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                horizontalAlignment = horizontalAlignment
            ) {
                content()
            }
            if (showTitleBar)
                WindowTitleBar(
                    title = title,
                    backgroundColor = titleBarBackgroundColor,
                    buttonsSpacing = buttonsSpacing
                )
        }
    ) { measurables, constraints ->
        val bodyMeasurable = measurables.first()
        val titleBarMeasurable = measurables.getOrNull(1)
        val reservedTitleBarHeight = if (titleBarMeasurable != null) titleBarHeightPx else 0
        val bodyConstraints = Constraints(
            minWidth = constraints.minWidth,
            maxWidth = constraints.maxWidth,
            minHeight = (constraints.minHeight - reservedTitleBarHeight).coerceAtLeast(0),
            maxHeight = (constraints.maxHeight - reservedTitleBarHeight).coerceAtLeast(0)
        )
        val bodyPlaceable = bodyMeasurable.measure(bodyConstraints)
        val titleBarPlaceable = titleBarMeasurable?.measure(
            Constraints(
                minWidth = bodyPlaceable.width,
                maxWidth = bodyPlaceable.width,
                minHeight = reservedTitleBarHeight,
                maxHeight = reservedTitleBarHeight
            )
        )

        val layoutWidth = bodyPlaceable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = (bodyPlaceable.height + (titleBarPlaceable?.height ?: 0))
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            var currentY = 0
            titleBarPlaceable?.let {
                it.placeRelative(0, currentY)
                currentY += it.height
            }
            bodyPlaceable.placeRelative(0, currentY)
        }
    }
}

@Composable
private fun rememberDialogMaxSize(
    window: Window,
    density: Density,
    maxWidth: Dp,
    maxHeight: Dp
) = remember(window.graphicsConfiguration, density, maxWidth, maxHeight) {
    window.resolveDialogMaxSize(density, maxWidth, maxHeight)
}

private fun Window.resolveDialogMaxSize(density: Density, maxWidth: Dp, maxHeight: Dp): DpSize {
    val usableBounds = graphicsConfiguration
        ?.let(::screenUsableBounds)
        ?: Rectangle(Toolkit.getDefaultToolkit().screenSize)
    val safeMaxWidth = with(density) { usableBounds.width.toDp() } * DialogScreenFillFraction
    val safeMaxHeight = with(density) { usableBounds.height.toDp() } * DialogScreenFillFraction

    return DpSize(
        width = if (!maxWidth.isUnspecified) maxWidth.coerceAtMost(safeMaxWidth) else safeMaxWidth,
        height = if (!maxHeight.isUnspecified) maxHeight.coerceAtMost(safeMaxHeight) else safeMaxHeight
    )
}

private fun createComposeDialog(ownerWindow: Window?) =
    if (ownerWindow != null) ComposeDialog(
        ownerWindow,
        Dialog.ModalityType.DOCUMENT_MODAL,
        ownerWindow.graphicsConfiguration
    ) else ComposeDialog()

private fun screenUsableBounds(configuration: GraphicsConfiguration): Rectangle {
    val screenBounds = configuration.bounds
    val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)

    return Rectangle(
        screenBounds.x + screenInsets.left,
        screenBounds.y + screenInsets.top,
        screenBounds.width - screenInsets.left - screenInsets.right,
        screenBounds.height - screenInsets.top - screenInsets.bottom
    )
}

private fun Dp.coerceToAtMostOrNull(max: Dp) =
    if (!isUnspecified) coerceAtMost(max) else null

private const val DialogScreenFillFraction = 0.85f