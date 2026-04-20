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
package com.highcapable.adbrowser.app.ui.vm.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.domain.setting.model.MenuShortcut as SettingsMenuShortcut

/**
 * UI shortcut model used by menus, key handlers, and the Preferences editor.
 */
data class MenuShortcut(
    val keyCode: Long,
    val keyLabel: String,
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false
) {

    companion object {

        private val ignoredModifierKeyCodes = setOf(
            Key.CtrlLeft.keyCode,
            Key.CtrlRight.keyCode,
            Key.MetaLeft.keyCode,
            Key.MetaRight.keyCode,
            Key.AltLeft.keyCode,
            Key.AltRight.keyCode,
            Key.ShiftLeft.keyCode,
            Key.ShiftRight.keyCode
        )

        fun fromEvent(event: KeyEvent): MenuShortcut? {
            if (event.type != KeyEventType.KeyDown) return null

            val key = event.key
            if (key.keyCode in ignoredModifierKeyCodes) return null

            val keyLabel = displayKeyLabel(key) ?: return null

            return MenuShortcut(
                keyCode = key.keyCode,
                keyLabel = keyLabel,
                ctrl = event.isCtrlPressed,
                meta = event.isMetaPressed,
                alt = event.isAltPressed,
                shift = event.isShiftPressed
            )
        }

        fun defaultFor(action: Action) = when (action) {
            Action.Open -> if (OsType.isMacOS) primary(Key.O, "O") else single(Key.Enter, "Enter")
            Action.NewFolder -> primary(Key.N, "N", shift = true)
            Action.Rename -> if (OsType.isMacOS) single(Key.Enter, "Enter") else single(Key.F2, "F2")
            Action.Copy -> primary(Key.C, "C")
            Action.Cut -> primary(Key.X, "X")
            Action.Paste -> primary(Key.V, "V")
            Action.Delete -> if (OsType.isMacOS) primary(Key.Backspace, "⌫") else single(Key.Delete, "Delete")
            Action.Properties -> primary(Key.I, "I")
            Action.Refresh -> single(Key.F5, "F5")
        }

        fun SettingsMenuShortcut.Collection.toUiType() = Collection(
            open = open.toUiType(Action.Open),
            newFolder = newFolder.toUiType(Action.NewFolder),
            rename = rename.toUiType(Action.Rename),
            copy = copy.toUiType(Action.Copy),
            cut = cut.toUiType(Action.Cut),
            paste = paste.toUiType(Action.Paste),
            delete = delete.toUiType(Action.Delete),
            properties = properties.toUiType(Action.Properties),
            refresh = refresh.toUiType(Action.Refresh)
        )

        @Composable
        fun Label(action: Action) = when (action) {
            Action.Open -> strings.menuOpen
            Action.NewFolder -> strings.menuNewFolder
            Action.Rename -> strings.menuRename
            Action.Copy -> strings.menuCopy
            Action.Cut -> strings.menuCut
            Action.Paste -> strings.menuPaste
            Action.Delete -> strings.menuDelete
            Action.Properties -> strings.menuProperties
            Action.Refresh -> strings.menuRefresh
        }

        private fun SettingsMenuShortcut.toUiType(action: Action) = if (keyCode == 0L || keyLabel.isBlank())
            defaultFor(action)
        else MenuShortcut(
            keyCode = keyCode,
            keyLabel = keyLabel,
            ctrl = ctrl,
            meta = meta,
            alt = alt,
            shift = shift
        )

        private fun primary(key: Key, label: String, shift: Boolean = false) = MenuShortcut(
            keyCode = key.keyCode,
            keyLabel = label,
            ctrl = !OsType.isMacOS,
            meta = OsType.isMacOS,
            shift = shift
        )

        private fun single(key: Key, label: String) = MenuShortcut(
            keyCode = key.keyCode,
            keyLabel = label
        )

        private fun displayKeyLabel(key: Key) = when (key) {
            Key.A -> "A"
            Key.B -> "B"
            Key.C -> "C"
            Key.D -> "D"
            Key.E -> "E"
            Key.F -> "F"
            Key.G -> "G"
            Key.H -> "H"
            Key.I -> "I"
            Key.J -> "J"
            Key.K -> "K"
            Key.L -> "L"
            Key.M -> "M"
            Key.N -> "N"
            Key.O -> "O"
            Key.P -> "P"
            Key.Q -> "Q"
            Key.R -> "R"
            Key.S -> "S"
            Key.T -> "T"
            Key.U -> "U"
            Key.V -> "V"
            Key.W -> "W"
            Key.X -> "X"
            Key.Y -> "Y"
            Key.Z -> "Z"
            Key.Zero -> "0"
            Key.One -> "1"
            Key.Two -> "2"
            Key.Three -> "3"
            Key.Four -> "4"
            Key.Five -> "5"
            Key.Six -> "6"
            Key.Seven -> "7"
            Key.Eight -> "8"
            Key.Nine -> "9"
            Key.Enter, Key.NumPadEnter -> "Enter"
            Key.Backspace -> "⌫"
            Key.Delete -> "Delete"
            Key.Spacebar -> "Space"
            Key.Tab -> "Tab"
            Key.Escape -> "Esc"
            Key.Comma -> ","
            Key.DirectionUp -> "Up"
            Key.DirectionDown -> "Down"
            Key.DirectionLeft -> "Left"
            Key.DirectionRight -> "Right"
            Key.F1 -> "F1"
            Key.F2 -> "F2"
            Key.F3 -> "F3"
            Key.F4 -> "F4"
            Key.F5 -> "F5"
            Key.F6 -> "F6"
            Key.F7 -> "F7"
            Key.F8 -> "F8"
            Key.F9 -> "F9"
            Key.F10 -> "F10"
            Key.F11 -> "F11"
            Key.F12 -> "F12"
            else -> null
        }
    }

    enum class Action {
        Open,
        NewFolder,
        Rename,
        Copy,
        Cut,
        Paste,
        Delete,
        Properties,
        Refresh
    }

    data class Collection(
        val open: MenuShortcut = defaultFor(Action.Open),
        val newFolder: MenuShortcut = defaultFor(Action.NewFolder),
        val rename: MenuShortcut = defaultFor(Action.Rename),
        val copy: MenuShortcut = defaultFor(Action.Copy),
        val cut: MenuShortcut = defaultFor(Action.Cut),
        val paste: MenuShortcut = defaultFor(Action.Paste),
        val delete: MenuShortcut = defaultFor(Action.Delete),
        val properties: MenuShortcut = defaultFor(Action.Properties),
        val refresh: MenuShortcut = defaultFor(Action.Refresh)
    ) {

        operator fun get(action: Action) = when (action) {
            Action.Open -> open
            Action.NewFolder -> newFolder
            Action.Rename -> rename
            Action.Copy -> copy
            Action.Cut -> cut
            Action.Paste -> paste
            Action.Delete -> delete
            Action.Properties -> properties
            Action.Refresh -> refresh
        }

        fun updated(action: Action, shortcut: MenuShortcut) = when (action) {
            Action.Open -> copy(open = shortcut)
            Action.NewFolder -> copy(newFolder = shortcut)
            Action.Rename -> copy(rename = shortcut)
            Action.Copy -> copy(copy = shortcut)
            Action.Cut -> copy(cut = shortcut)
            Action.Paste -> copy(paste = shortcut)
            Action.Delete -> copy(delete = shortcut)
            Action.Properties -> copy(properties = shortcut)
            Action.Refresh -> copy(refresh = shortcut)
        }

        fun toSettingsType() = SettingsMenuShortcut.Collection(
            open = open.toSettingsType(),
            newFolder = newFolder.toSettingsType(),
            rename = rename.toSettingsType(),
            copy = copy.toSettingsType(),
            cut = cut.toSettingsType(),
            paste = paste.toSettingsType(),
            delete = delete.toSettingsType(),
            properties = properties.toSettingsType(),
            refresh = refresh.toSettingsType()
        )
    }

    val isDefined get() = keyCode != 0L && keyLabel.isNotBlank()

    fun matches(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown || !isDefined) return false

        return event.key.keyCode == keyCode &&
            event.isCtrlPressed == ctrl &&
            event.isMetaPressed == meta &&
            event.isAltPressed == alt &&
            event.isShiftPressed == shift
    }

    fun toComposeShortcut() = KeyShortcut(
        key = Key(keyCode),
        ctrl = ctrl,
        meta = meta,
        alt = alt,
        shift = shift
    )

    fun toMenuKeybinding() = buildSet {
        if (ctrl) add("Ctrl")
        if (meta) add("⌘")
        if (alt) add(if (OsType.isMacOS) "⌥" else "Alt")
        if (shift) add(if (OsType.isMacOS) "⇧" else "Shift")
        add(keyLabel)
    }

    fun displayText() = toMenuKeybinding().joinToString(" + ")

    fun toSettingsType() = SettingsMenuShortcut(
        keyCode = keyCode,
        keyLabel = keyLabel,
        ctrl = ctrl,
        meta = meta,
        alt = alt,
        shift = shift
    )
}