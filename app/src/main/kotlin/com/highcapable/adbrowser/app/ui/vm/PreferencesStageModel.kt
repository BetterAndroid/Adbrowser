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
 * This file is created by fankes on 2026/4/3.
 */
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.locale.Locales
import com.highcapable.adbrowser.app.locale.normalizeStoredLanguageTag
import com.highcapable.adbrowser.app.ui.utils.SystemFileChooser
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.app.ui.vm.model.type.FileSortMode
import com.highcapable.adbrowser.app.ui.vm.model.type.FileViewMode
import com.highcapable.adbrowser.core.domain.setting.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Window
import java.nio.file.Files
import java.nio.file.Path

class PreferencesStageModel(private val appState: AppState) : ViewModel() {

    private companion object {

        const val DEFAULT_DEVICE_PANE_WIDTH = 300.0
        const val DEFAULT_FILE_COLUMN_WIDTH_NAME = 260.0
        const val DEFAULT_FILE_COLUMN_WIDTH_SIZE = 140.0
        const val DEFAULT_FILE_COLUMN_WIDTH_MODIFIED = 240.0
        const val DEFAULT_FILE_COLUMN_WIDTH_PERMISSION = 150.0
        const val ADB_VALIDATE_TIMEOUT_MS = 5000L
    }

    private val settingsService get() = appState.appServices.settingsService

    /**
     * Top-level Preferences sections shown by the tab strip.
     */
    enum class Tab { General, Files, Device }

    /**
     * Visual category for the footer status message.
     */
    enum class StatusCategory { Normal, Error }

    /**
     * Footer status payloads shown by the Preferences window.
     *
     * Errors are intentionally modeled explicitly for common validation failures so the stage can
     * map them to localized strings without parsing backend text.
     */
    sealed interface Status {
        data object None : Status
        data object SidebarSpacingReset : Status
        data object FileColumnWidthsReset : Status
        data object MainWindowBoundsReset : Status
        data object PreferencesSaved : Status
        data object Cancelled : Status
        data object AdbPathNotFound : Status
        data object AdbExecutableInvalid : Status
        data object AdbPathEmpty : Status
        data class Failed(val reason: String?) : Status
    }

    var currentTab by mutableStateOf(Tab.General)

    var selectedLanguageTag by mutableStateOf(Locales.FOLLOW_SYSTEM)

    var showHiddenFiles by mutableStateOf(false)
    var foldersFirst by mutableStateOf(true)
    var rememberLastFileViewMode by mutableStateOf(true)
    var rememberLastFileSortMode by mutableStateOf(true)
    var rememberLastDevicePath by mutableStateOf(true)

    val adbExecPath = TextFieldState("")
    var rememberLastDevice by mutableStateOf(true)
    var superuser by mutableStateOf(false)

    var status by mutableStateOf<Status>(Status.None)
    var isSaving by mutableStateOf(false)
        private set

    val statusCategory get() = when (status) {
        is Status.None,
        is Status.SidebarSpacingReset,
        is Status.FileColumnWidthsReset,
        is Status.MainWindowBoundsReset,
        is Status.PreferencesSaved,
        is Status.Cancelled -> StatusCategory.Normal
        is Status.AdbPathNotFound,
        is Status.AdbExecutableInvalid,
        is Status.AdbPathEmpty,
        is Status.Failed -> StatusCategory.Error
    }

    init {
        restoreFromSettings()
    }

    /** Resets the saved device pane width and notifies the main stage to re-read settings. */
    fun resetSidebarSpacing() {
        val success = runPersistAction(
            successStatus = Status.SidebarSpacingReset
        ) {
            settingsService.current.devicePaneWidth = DEFAULT_DEVICE_PANE_WIDTH
            settingsService.save()
        }
        if (success) appState.sync()
    }

    /** Resets saved file header column widths and notifies the main stage to re-read settings. */
    fun resetFileColumnWidths() {
        val success = runPersistAction(
            successStatus = Status.FileColumnWidthsReset
        ) {
            settingsService.current.fileColumnWidthName = DEFAULT_FILE_COLUMN_WIDTH_NAME
            settingsService.current.fileColumnWidthSize = DEFAULT_FILE_COLUMN_WIDTH_SIZE
            settingsService.current.fileColumnWidthModified = DEFAULT_FILE_COLUMN_WIDTH_MODIFIED
            settingsService.current.fileColumnWidthPermission = DEFAULT_FILE_COLUMN_WIDTH_PERMISSION
            settingsService.save()
        }
        if (success) appState.sync()
    }

    /** Resets persisted main window bounds so the next sync restores default centered behavior. */
    fun resetMainWindowBounds() {
        val success = runPersistAction(
            successStatus = Status.MainWindowBoundsReset
        ) {
            settingsService.current.mainWindowWidth = AppSettings.DEFAULT_MAIN_WINDOW_WIDTH
            settingsService.current.mainWindowHeight = AppSettings.DEFAULT_MAIN_WINDOW_HEIGHT
            settingsService.current.mainWindowPosX = null
            settingsService.current.mainWindowPosY = null
            settingsService.save()
        }
        if (success) appState.sync()
    }

    /**
     * Validates and persists the edited preferences asynchronously.
     *
     * Save is intentionally gated by `isSaving` because ADB path validation may block for several
     * seconds when a wrong executable is selected. The callback is only invoked after a successful
     * save so the caller can safely close the window without hiding validation failures.
     */
    fun save(onSuccess: () -> Unit = {}) {
        if (isSaving) return

        modelScope.launch {
            isSaving = true
            try {
                when (validateAdbPath()) {
                    ValidationResult.PathEmpty -> {
                        status = Status.AdbPathEmpty
                        return@launch
                    }
                    ValidationResult.PathNotFound -> {
                        status = Status.AdbPathNotFound
                        return@launch
                    }
                    ValidationResult.ExecutableInvalid -> {
                        status = Status.AdbExecutableInvalid
                        return@launch
                    }
                    ValidationResult.Ok -> Unit
                }

                val current = settingsService.current

                // Only request a file list refresh when a saved preference affects how entries are
                // presented. This avoids unnecessary reloads for unrelated settings such as language.
                val shouldRefreshFileList = current.showHiddenFiles != showHiddenFiles ||
                    current.foldersFirst != foldersFirst ||
                    current.rememberLastFileViewMode != rememberLastFileViewMode ||
                    current.rememberLastFileSortMode != rememberLastFileSortMode

                val saved = runPersistActionAsync(
                    successStatus = Status.PreferencesSaved
                ) {
                    settingsService.current.language = normalizeStoredLanguageTag(selectedLanguageTag)
                    settingsService.current.adbExecPath = adbExecPath.text.toString().trim()
                    settingsService.current.rememberLastDevice = rememberLastDevice
                    settingsService.current.useSuperuser = superuser
                    settingsService.current.showHiddenFiles = showHiddenFiles
                    settingsService.current.foldersFirst = foldersFirst
                    settingsService.current.rememberLastFileViewMode = rememberLastFileViewMode
                    settingsService.current.rememberLastFileSortMode = rememberLastFileSortMode
                    settingsService.current.rememberLastDevicePath = rememberLastDevicePath

                    // If the user unchecks "remember last file view/sort mode", also clear the persisted values
                    // since they won't be used anymore and may cause confusion if the user later re-enables the setting.
                    if (!rememberLastFileViewMode) settingsService.current.lastFileViewMode = FileViewMode.List.toSettingsType()
                    if (!rememberLastFileSortMode) settingsService.current.lastFileSortMode = FileSortMode.Name.toSettingsType()

                    withContext(Dispatchers.IO) {
                        settingsService.save()
                    }
                }

                if (saved) {
                    // Preferences are consumed by the wider app, not only this window.
                    // A sync broadcasts the updated settings back to active stages.
                    appState.sync(refreshFileList = shouldRefreshFileList)
                    onSuccess()
                }
            } finally {
                isSaving = false
            }
        }
    }

    /** Cancels the model scope when the Preferences window is disposed. */
    fun dispose() {
        modelScope.cancel()
    }

    /** Opens the native file chooser and writes the chosen executable path back into the editor state. */
    fun browseAdbPath(parentWindow: Window?, dialogTitle: String) {
        val selectedPath = SystemFileChooser.chooseOpenFile(
            parent = parentWindow,
            title = dialogTitle,
            initialPath = adbExecPath.text.toString()
        ) ?: return

        setAdbPath(selectedPath)
    }

    /** Restores the editable values from persisted settings and marks the session as cancelled. */
    fun cancel() {
        restoreFromSettings()
        status = Status.Cancelled
    }

    /** Copies the current persisted settings into the editable window state. */
    private fun restoreFromSettings() {
        val settings = settingsService.current
        selectedLanguageTag = normalizeStoredLanguageTag(settings.language)
        showHiddenFiles = settings.showHiddenFiles
        foldersFirst = settings.foldersFirst
        rememberLastFileViewMode = settings.rememberLastFileViewMode
        rememberLastFileSortMode = settings.rememberLastFileSortMode
        rememberLastDevicePath = settings.rememberLastDevicePath
        rememberLastDevice = settings.rememberLastDevice
        superuser = settings.useSuperuser
        setAdbPath(settings.adbExecPath)
    }

    /**
     * Runs a small synchronous persistence action used by reset buttons.
     *
     * Resets are kept synchronous because they are short, self-contained setting writes and the
     * caller expects an immediate success/error status before deciding whether to sync the app.
     */
    private fun runPersistAction(successStatus: Status, block: suspend () -> Unit): Boolean =
        runCatching { runBlocking { block() } }
            .onSuccess { status = successStatus }
            .onFailure { status = Status.Failed(it.message) }
            .isSuccess

    /** Async persistence helper used by the main save flow. */
    private suspend fun runPersistActionAsync(successStatus: Status, block: suspend () -> Unit): Boolean =
        runCatching { block() }
            .onSuccess { status = successStatus }
            .onFailure { status = Status.Failed(it.message) }
            .isSuccess

    private enum class ValidationResult {
        Ok,
        PathEmpty,
        PathNotFound,
        ExecutableInvalid
    }

    /**
     * Validates the selected ADB executable.
     *
     * The timeout is important here: some wrong binaries do not fail fast and can block inside the
     * validation command. Returning `ExecutableInvalid` on timeout keeps the Preferences window
     * responsive instead of leaving the entire save flow stuck forever.
     */
    private suspend fun validateAdbPath(): ValidationResult {
        val path = adbExecPath.text.toString().trim()
        if (path.isBlank()) return ValidationResult.PathEmpty

        val parsedPath = runCatching { Path.of(path) }.getOrNull()
            ?: return ValidationResult.PathNotFound
        if (!Files.exists(parsedPath)) return ValidationResult.PathNotFound

        // Prefer validating the exact path value the user entered instead of relying on global
        // app state. This keeps the check correct even before the setting is actually saved.
        val result = withContext(Dispatchers.IO) {
            withTimeoutOrNull(ADB_VALIDATE_TIMEOUT_MS) {
                runCatching {
                    appState.appServices.adbClient.validateExecPath(path)
                }.getOrNull()
            }
        }

        if (result?.isOk != true) return ValidationResult.ExecutableInvalid

        return ValidationResult.Ok
    }

    /** Replaces the text field content atomically so UI and model stay in sync after browse/reset. */
    private fun setAdbPath(path: String) {
        val value = path.trim()
        adbExecPath.edit {
            replace(0, length, value)
        }
    }
}