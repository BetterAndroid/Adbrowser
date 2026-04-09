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
package com.highcapable.adbrowser.frontend.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.frontend.ui.assets.AppIcons
import com.highcapable.adbrowser.frontend.ui.modifier.edgeBorder
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.model.PathBreadcrumbSegment
import org.jetbrains.jewel.ui.component.Text

@Composable
fun PathBreadcrumbBar(
    segments: List<PathBreadcrumbSegment>,
    scrollState: ScrollState,
    onRootClick: () -> Unit,
    onSegmentClick: (PathBreadcrumbSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.hintBackground)
            .edgeBorder(
                color = colors.panelBorder,
                top = true,
                bottom = false,
                start = false,
                end = false
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbRootButton(onClick = onRootClick)
        segments.forEach { segment ->
            BreadcrumbSegment(
                segment = segment,
                onClick = { onSegmentClick(segment) }
            )
        }
    }
}

@Composable
private fun BreadcrumbRootButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(4.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        ContentIcon(
            key = AppIcons.FilePathArrow,
            contentDescription = "/",
            modifier = Modifier
                .size(14.dp)
                .alpha(0.5f)
        )
    }
}

@Composable
private fun BreadcrumbSegment(
    segment: PathBreadcrumbSegment,
    onClick: () -> Unit
) {
    val colors = AdbrowserTheme.colors

    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (segment.showLeadingArrow) {
            ContentIcon(
                key = AppIcons.FilePathArrow,
                contentDescription = ">",
                modifier = Modifier
                    .size(14.dp)
                    .alpha(0.5f)
            )
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .clip(RoundedCornerShape(4.dp))
                .padding(2.dp)
        ) {
            Text(
                text = segment.displayName,
                fontSize = 13.sp,
                color = colors.pathBreadcrumbForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}