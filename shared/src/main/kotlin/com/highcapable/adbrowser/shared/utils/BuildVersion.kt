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
 * This file is created by fankes on 2026/4/6.
 */
package com.highcapable.adbrowser.shared.utils

import com.highcapable.adbrowser.generated.AdbrowserProperties

/**
 * Provides build version text for UI display.
 *
 * - Format: `Build: {GIT_COMMIT_ID_ABBREV} | {GIT_COMMIT_TIME}`
 */
object BuildVersion {

    /**
     * Returns a user-facing build version text.
     */
    const val TEXT = "Build: ${AdbrowserProperties.GIT_COMMIT_ID_ABBREV} | ${AdbrowserProperties.GIT_COMMIT_TIME}"
}