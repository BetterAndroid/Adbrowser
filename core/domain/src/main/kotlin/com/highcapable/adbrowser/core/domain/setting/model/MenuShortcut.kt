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
package com.highcapable.adbrowser.core.domain.setting.model

import kotlinx.serialization.Serializable

/**
 * Persisted keyboard shortcut payload.
 *
 * The domain layer stores a generic key code plus a user-facing key label instead of any
 * Compose-specific type so the settings file stays UI-framework agnostic.
 */
@Serializable
data class MenuShortcut(
    val keyCode: Long = 0L,
    val keyLabel: String = "",
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false
) {

    /**
     * Shortcut collection persisted under `AppSettings.menuShortcuts`.
     *
     * Zero key codes mean "not customized yet", allowing the app layer to supply the current
     * platform defaults without hard-coding Compose key values into the backend model.
     */
    @Serializable
    data class Collection(
        val open: MenuShortcut = MenuShortcut(),
        val newFolder: MenuShortcut = MenuShortcut(),
        val rename: MenuShortcut = MenuShortcut(),
        val copy: MenuShortcut = MenuShortcut(),
        val cut: MenuShortcut = MenuShortcut(),
        val paste: MenuShortcut = MenuShortcut(),
        val delete: MenuShortcut = MenuShortcut(),
        val properties: MenuShortcut = MenuShortcut(),
        val refresh: MenuShortcut = MenuShortcut()
    )
}