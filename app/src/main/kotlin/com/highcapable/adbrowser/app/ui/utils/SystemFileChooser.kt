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
package com.highcapable.adbrowser.app.ui.utils

import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window
import java.io.File

/**
 * Opens the native system file chooser dialog.
 */
object SystemFileChooser {

    /**
     * Opens a file chooser dialog and returns the selected file path, or null if cancelled.
     * @param parent the parent window for the dialog, or null for no parent.
     * @param title the title of the file chooser dialog.
     * @param initialPath an optional initial path to open in the dialog.
     */
    fun chooseFile(parent: Window?, title: String, initialPath: String = ""): String? {
        val dialog = when (parent) {
            is Frame -> FileDialog(parent, title, FileDialog.LOAD)
            is Dialog -> FileDialog(parent, title, FileDialog.LOAD)
            else -> FileDialog(null as Frame?, title, FileDialog.LOAD)
        }

        configureInitialPath(dialog, initialPath)
        dialog.isVisible = true

        val fileName = dialog.file ?: return null
        val directory = dialog.directory ?: return fileName

        return File(directory, fileName).absolutePath
    }

    private fun configureInitialPath(dialog: FileDialog, initialPath: String) {
        val normalized = initialPath.trim()
        if (normalized.isBlank()) return

        val initial = File(normalized)
        when {
            initial.exists() && initial.isDirectory -> dialog.directory = initial.absolutePath
            initial.exists() -> {
                dialog.directory = initial.parent
                dialog.file = initial.name
            }
            initial.parentFile?.exists() == true -> {
                dialog.directory = initial.parent
                dialog.file = initial.name
            }
        }
    }
}