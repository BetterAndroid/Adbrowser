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
 * This file is created by fankes on 2026/4/12.
 */
package com.highcapable.adbrowser.core.common.utils.extension

private val FileSizeUnits = arrayOf("B", "KB", "MB", "GB", "TB")

/**
 * Formats a raw byte count using decimal units so the UI matches common desktop file managers.
 *
 * The product intentionally displays `KB/MB/GB/TB` instead of IEC `KiB/MiB/...`, and the example
 * requirement uses decimal scaling (`3_642_296 -> 3.64 MB`), so this formatter uses a 1000 base.
 */
fun Long.toFriendlyFileSize(): String {
    if (this < 0L) return "0 ${FileSizeUnits.first()}"
    if (this < 1000L) return "$this ${FileSizeUnits.first()}"

    var value = this.toDouble()
    var unitIndex = 0
    while (value >= 1000.0 && unitIndex < FileSizeUnits.lastIndex) {
        value /= 1000.0
        unitIndex++
    }

    val formatted = "%.2f".format(value).trimEnd('0').trimEnd('.')
    return "$formatted ${FileSizeUnits[unitIndex]}"
}