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
 * This file is created by fankes on 2026/5/3.
 */
package com.highcapable.adbrowser.app.ui.platform.jbr

import com.jetbrains.JBR as JetBrainsRuntime

/**
 * Centralizes runtime checks for JetBrains Runtime specific features used by the desktop UI.
 *
 * Callers can probe lightweight support through [isAvailable], or fail fast through [require]
 * when a code path should never continue without JBR.
 */
object JBR {

    /**
     * Whether JetBrains Runtime APIs are available to the current process.
     */
    val isAvailable get() = runCatching { JetBrainsRuntime.isAvailable() }.getOrDefault(false)

    /**
     * Ensures the current process is running on a usable JetBrains Runtime.
     */
    fun require() {
        if (!isAvailable) throw RuntimeException(
            "To run this program, you need to use a JRE or JDK powered by JetBrains Runtime (JBR)."
        )
    }
}