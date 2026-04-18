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
 * This file is created by fankes on 2026/4/19.
 */
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.jetbrains.jewel.foundation.theme.JewelTheme

@Composable
fun QrCodePanel(
    content: String,
    modifier: Modifier = Modifier,
    foregroundColor: Color = JewelTheme.contentColor,
    backgroundColor: Color = Color.Transparent
) {
    val matrix = remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QrMatrixSize, QrMatrixSize)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val moduleSize = size.minDimension / matrix.width.toFloat()

            // Draw discrete QR modules instead of scaling a bitmap so the code stays crisp on both
            // HiDPI and regular desktop displays.
            (0 until matrix.height).forEach { y ->
                (0 until matrix.width).forEach { x ->
                    if (!matrix[x, y]) return@forEach

                    drawRect(
                        color = foregroundColor,
                        topLeft = Offset(x * moduleSize, y * moduleSize),
                        size = Size(moduleSize, moduleSize)
                    )
                }
            }
        }
    }
}

private const val QrMatrixSize = 256