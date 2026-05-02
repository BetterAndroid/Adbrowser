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
 * This file is created by fankes on 2026/4/18.
 */
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.app.ui.vm.model.type.ErrorMessage
import com.highcapable.adbrowser.app.ui.vm.model.type.resolveAdbErrorKind
import com.highcapable.adbrowser.core.adb.model.OperationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DeviceConnectDialogModel(private val appState: AppState) : ViewModel() {

    private companion object {

        const val MAX_ADDRESS_HISTORY_SIZE = 12
    }

    sealed interface Status {
        data object None : Status
        data object Connecting : Status
        data class Failed(val reason: String?) : Status
    }

    private val adbClient get() = appState.appServices.adbClient
    private val settingsService get() = appState.appServices.settingsService

    private var connectJob: Job? = null

    val addressState = TextFieldState("")
    val addressHistory = mutableStateListOf<String>().apply {
        addAll(settingsService.current.connectedDeviceAddressHistory.distinctBy { it.lowercase() })
    }

    var status by mutableStateOf<Status>(Status.None)
        private set
    var isConnecting by mutableStateOf(false)
        private set

    val canConfirm get() = !isConnecting && addressState.text.toString().trim().isNotBlank()

    /**
     * Returns the best-matching connection history entries for the current input.
     *
     * Prefix matches are shown before looser substring matches so keyboard-first users can usually
     * hit Enter without fighting unrelated older addresses.
     */
    val addressSuggestions: List<String>
        get() {
            val query = addressState.text.toString().trim()
            if (addressHistory.isEmpty()) return emptyList()
            if (query.isBlank()) return addressHistory.toList()

            val lowerQuery = query.lowercase()
            val prefixMatches = mutableListOf<String>()
            val containsMatches = mutableListOf<String>()

            addressHistory.forEach { address ->
                val lowerAddress = address.lowercase()
                when {
                    lowerAddress.startsWith(lowerQuery) -> prefixMatches += address
                    lowerQuery in lowerAddress -> containsMatches += address
                }
            }

            return prefixMatches + containsMatches
        }

    /**
     * Clears a previous failure once the user edits the address again.
     */
    fun onAddressChanged() {
        if (status is Status.Failed && !isConnecting) status = Status.None
    }

    /**
     * Fills the text field from one of the remembered addresses.
     */
    fun selectAddressSuggestion(value: String) {
        addressState.edit {
            replace(0, length, value)
        }
        onAddressChanged()
    }

    /**
     * Starts the connect request and registers the target device for retrospective selection.
     *
     * The dialog does not force an immediate refresh/selection bridge into the main stage. It only
     * records the normalized target in the app-scoped pending coordinator, then closes. Active
     * main-stage models can observe that coordinator during their own lifecycle and consume the
     * pending entry once the connected transport actually shows up in the refreshed device list.
     */
    fun connect(onCloseRequest: () -> Unit) {
        if (!canConfirm) return

        val address = addressState.text.toString().trim()
        connectJob?.cancel()
        connectJob = modelScope.launch {
            isConnecting = true
            status = Status.Connecting
            try {
                val result = adbClient.connectDevice(address)
                if (!result.isOk && !isDeviceUnauthorized(result)) {
                    status = Status.Failed(result.errorMessage)
                    return@launch
                }

                rememberConnectedAddress(address)
                status = Status.None
                appState.pendingDeviceSelectionCoordinator.enqueueConnectedDevice(address)
                onCloseRequest()
            } catch (_: CancellationException) {
                // Cancellation is expected when the user closes the dialog while a command runs.
            } catch (t: Throwable) {
                status = Status.Failed(t.message)
            } finally {
                isConnecting = false
                connectJob = null
            }
        }
    }

    /**
     * Cancels the in-flight command immediately and closes the dialog.
     */
    fun cancelAndClose(onCloseRequest: () -> Unit) {
        connectJob?.cancel()
        onCloseRequest()
    }

    /**
     * Persists successful network endpoints exactly as the user entered them.
     *
     * The dialog treats history as best-effort convenience state: save failures should not undo a
     * connection that already succeeded, so persistence errors are intentionally ignored here.
     */
    private suspend fun rememberConnectedAddress(address: String) {
        val rawAddress = address.trim()
        val updatedHistory = buildList {
            add(rawAddress)
            addAll(addressHistory.filterNot { it.equals(rawAddress, ignoreCase = true) })
        }.take(MAX_ADDRESS_HISTORY_SIZE)

        addressHistory.clear()
        addressHistory += updatedHistory

        settingsService.current.connectedDeviceAddressHistory = updatedHistory.toMutableList()
        runCatching { settingsService.save() }
    }

    fun dispose() {
        modelScope.cancel()
    }

    /**
     * Some devices report an unauthorized transport immediately after `adb connect`.
     *
     * For the connect dialog this still means the network connection itself succeeded, and the
     * subsequent device refresh should surface that transport in the unauthorized state.
     */
    private fun isDeviceUnauthorized(result: OperationResult<*>) = result.resolveAdbErrorKind() == ErrorMessage.AdbKind.DeviceUnauthorized
}