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
 * This file is created by fankes on 2026/4/7.
 */
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.ui.utils.SystemFileChooser
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.core.common.utils.OsType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Window
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class InitialSetupStageModel(private val appState: AppState) : ViewModel() {

    private companion object {
        const val ADB_VALIDATE_TIMEOUT_MS = 5000L
    }

    private val settingsService get() = appState.appServices.settingsService
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Validation and persistence outcomes shown in the footer of the initial setup window.
     * Common path failures are modeled explicitly so they can be localized directly.
     */
    sealed interface Status {
        data object None : Status
        data object AdbPathNotFound : Status
        data object AdbExecutableInvalid : Status
        data object AdbPathEmpty : Status
        data class Failed(val reason: String?) : Status
    }

    private enum class ValidationResult {
        Ok,
        PathEmpty,
        PathNotFound,
        ExecutableInvalid
    }

    val adbExecPath = TextFieldState(resolveInitialAdbPath())
    var status by mutableStateOf<Status>(Status.None)
    var isBusy by mutableStateOf(false)
        private set

    /**
     * Validates the chosen executable and persists it as the application's configured ADB path.
     *
     * The success callback is only invoked after the path is both valid and saved, so the caller
     * can safely advance into the main application flow without hiding any validation error.
     */
    fun continueSetup(onSuccess: () -> Unit = {}) {
        if (isBusy) return

        modelScope.launch {
            isBusy = true
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

                val normalizedPath = adbExecPath.text.toString().trim()
                val saved = runCatching {
                    settingsService.current.adbExecPath = normalizedPath
                    withContext(Dispatchers.IO) {
                        settingsService.save()
                    }
                }.onFailure {
                    status = Status.Failed(it.message)
                }.isSuccess
                if (!saved) return@launch

                status = Status.None

                // Initial setup feeds global app state, so the rest of the application must be
                // notified after the path is saved successfully.
                appState.sync()
                onSuccess()
            } finally {
                isBusy = false
            }
        }
    }

    /** Opens the native file chooser and clears any previous validation error on success. */
    fun browseAdbPath(parentWindow: Window?, dialogTitle: String) {
        val selectedPath = SystemFileChooser.chooseFile(
            parent = parentWindow,
            title = dialogTitle,
            initialPath = adbExecPath.text.toString()
        ) ?: return

        setAdbPath(selectedPath)
        status = Status.None
    }

    /** Cancels model coroutines when the setup window leaves composition. */
    fun dispose() {
        modelScope.cancel()
    }

    /**
     * Validates the chosen executable with a timeout.
     *
     * This guard is important because selecting an arbitrary binary can cause validation to hang
     * inside the spawned process. Timing out keeps the setup window responsive and lets the UI
     * report the path as invalid instead of getting stuck forever.
     */
    private suspend fun validateAdbPath(): ValidationResult {
        val path = adbExecPath.text.toString().trim()
        if (path.isBlank()) return ValidationResult.PathEmpty

        val parsedPath = runCatching { Path.of(path) }.getOrNull()
            ?: return ValidationResult.PathNotFound
        if (!Files.exists(parsedPath)) return ValidationResult.PathNotFound

        // Validate the exact text field value before it is persisted. This avoids coupling first-run
        // validation to any previously saved app state.
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

    /** Replaces the text field content atomically after browse or environment discovery. */
    private fun setAdbPath(path: String) {
        val value = path.trim()
        adbExecPath.edit {
            replace(0, length, value)
        }
    }

    /**
     * Resolves the initial path shown in the setup window.
     *
     * A persisted setting wins if present; otherwise we probe the environment so the user can
     * confirm an inferred path instead of having to type it manually on first launch.
     */
    private fun resolveInitialAdbPath(): String {
        val persistedPath = settingsService.current.adbExecPath.trim()
        if (persistedPath.isNotBlank()) return persistedPath

        return resolveAdbPathFromEnvironment().orEmpty()
    }

    /**
     * Tries several common ADB discovery sources in order of specificity.
     *
     * The explicit environment variable wins first, then standard Android SDK roots, and finally
     * the system PATH. This order keeps a user-provided override from being masked by a global SDK.
     */
    private fun resolveAdbPathFromEnvironment(): String? {
        val executableName = if (OsType.isWindows) "adb.exe" else "adb"

        val explicitPath = System.getenv("ADB_EXEC_PATH")
            ?.trim()
            ?.takeIf(::isUsableExecutablePath)
        if (explicitPath != null) return explicitPath

        val sdkRootCandidates = listOf("ANDROID_SDK_ROOT", "ANDROID_HOME")
            .asSequence()
            .mapNotNull { System.getenv(it)?.trim()?.takeIf(String::isNotBlank) }
            .map { Path.of(it).resolve("platform-tools").resolve(executableName).toString() }
            .firstOrNull(::isUsableExecutablePath)
        if (sdkRootCandidates != null) return sdkRootCandidates

        return System.getenv("PATH")
            ?.split(File.pathSeparatorChar)
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.map { Path.of(it).resolve(executableName).toString() }
            ?.firstOrNull(::isUsableExecutablePath)
    }

    /** Basic filesystem-level executable check used by environment discovery. */
    private fun isUsableExecutablePath(path: String): Boolean {
        val candidate = runCatching { Path.of(path.trim()) }.getOrNull() ?: return false
        if (!Files.isRegularFile(candidate)) return false

        // Windows executables are not reliably represented by Files.isExecutable, so the file
        // existence check is enough there once the extension has already been resolved.
        return OsType.isWindows || Files.isExecutable(candidate)
    }
}