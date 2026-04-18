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
 * This file is created by fankes on 2026/4/19.
 */
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.core.adb.model.pairing.PairingDevice
import com.highcapable.adbrowser.core.adb.model.pairing.QrPairingSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DevicePairDialogModel(private val appState: AppState) : ViewModel() {

    private companion object {

        const val QR_SESSION_TTL_SECONDS = 60
    }

    enum class Tab {
        QrCode,
        PairingCode
    }

    sealed interface Status {
        data object None : Status
        data object PreparingQr : Status
        data object WaitingForQr : Status
        data object Pairing : Status
        data class Failed(val reason: String?) : Status
    }

    private val adbClient get() = appState.appServices.adbClient

    private var initializeJob: Job? = null
    private var qrWaitJob: Job? = null
    private var qrExpirationJob: Job? = null
    private var pairingDevicesJob: Job? = null
    private var manualPairJob: Job? = null
    private var onPairingCompleted: (() -> Unit)? = null
    private var initialized = false

    val pairingDevices = mutableStateListOf<PairingDevice>()

    var currentTab by mutableStateOf(Tab.QrCode)
        private set
    var status by mutableStateOf<Status>(Status.None)
        private set
    var qrSession by mutableStateOf<QrPairingSession?>(null)
        private set
    var qrRemainingSeconds by mutableStateOf(0)
        private set
    var qrError by mutableStateOf<String?>(null)
        private set
    var activePairingDevice by mutableStateOf<PairingDevice?>(null)
        private set
    var codeDialogError by mutableStateOf<String?>(null)
        private set
    var isObservingPairingDevices by mutableStateOf(false)
        private set
    var isManualPairing by mutableStateOf(false)
        private set

    val canClose get() = status != Status.PreparingQr && !isManualPairing
    val canRefreshQr get() = currentTab == Tab.QrCode && status != Status.PreparingQr && !isManualPairing

    /**
     * Binds the success callback once and starts the default QR tab workflow.
     */
    fun bind(onPairingCompleted: () -> Unit) {
        this.onPairingCompleted = onPairingCompleted
        if (initialized) return

        initialized = true
        activateTab(currentTab)
    }

    /**
     * Switches between the QR and pairing-code flows, cancelling the inactive branch so stale
     * discovery work cannot update the wrong tab after the user pivots.
     */
    fun selectTab(tab: Tab) {
        if (isManualPairing) return
        if (currentTab == tab) return

        currentTab = tab
        status = Status.None
        qrError = null
        codeDialogError = null
        activePairingDevice = null
        activateTab(tab)
    }

    /**
     * Opens the six-digit pairing-code dialog for the selected endpoint.
     */
    fun openPairingCodeDialog(device: PairingDevice) {
        if (isManualPairing) return
        activePairingDevice = device
        codeDialogError = null
    }

    fun dismissPairingCodeDialog() {
        if (isManualPairing) return
        activePairingDevice = null
        codeDialogError = null
    }

    /**
     * Runs `adb pair` for the selected manual endpoint and lets the main stage claim focus later
     * once the corresponding network device actually appears in the observed device list.
     */
    fun confirmManualPair(pairingCode: String) {
        val device = activePairingDevice ?: return
        val normalizedCode = pairingCode.trim()
        if (normalizedCode.length != 6 || normalizedCode.any { !it.isDigit() }) return

        manualPairJob?.cancel()
        manualPairJob = modelScope.launch {
            isManualPairing = true
            status = Status.Pairing
            codeDialogError = null
            try {
                val result = adbClient.pairDevice(device, normalizedCode)
                if (!result.isOk) {
                    status = Status.None
                    codeDialogError = result.errorMessage
                    return@launch
                }

                appState.pendingDeviceSelectionCoordinator.enqueueConnectedDevice(
                    address = device.address,
                    serviceName = device.serviceName
                )
                onPairingCompleted?.invoke()
            } catch (_: CancellationException) {
                // Manual close is allowed to abort the current pairing attempt.
            } catch (t: Throwable) {
                status = Status.None
                codeDialogError = t.message
            } finally {
                isManualPairing = false
                manualPairJob = null
            }
        }
    }

    /**
     * Cancels all backend work before closing so background adb commands do not keep a stale dialog
     * model alive after the window disappears.
     */
    fun cancelAndClose(onCloseRequest: () -> Unit) {
        initializeJob?.cancel()
        qrWaitJob?.cancel()
        qrExpirationJob?.cancel()
        pairingDevicesJob?.cancel()
        manualPairJob?.cancel()
        onCloseRequest()
    }

    /**
     * Regenerates the current QR session manually or after expiration/failure.
     */
    fun refreshQrSession() {
        if (!canRefreshQr) return
        if (currentTab != Tab.QrCode) return

        prepareQrSession(clearQrError = true)
    }

    private fun activateTab(tab: Tab) {
        when (tab) {
            Tab.QrCode -> {
                stopPairingDeviceObservation(clearDevices = false)
                prepareQrSession(clearQrError = true)
            }
            Tab.PairingCode -> {
                stopQrWorkflow(clearSession = true)
                startObservePairingDevices()
            }
        }
    }

    private fun prepareQrSession(clearQrError: Boolean) {
        stopQrWorkflow(clearSession = true)
        initializeJob = modelScope.launch {
            if (clearQrError) qrError = null
            status = Status.PreparingQr
            try {
                val result = adbClient.createQrPairingSession()
                if (!result.isOk) {
                    status = Status.Failed(result.errorMessage)
                    return@launch
                }

                val session = result.data ?: run {
                    status = Status.Failed(null)
                    return@launch
                }

                qrSession = session
                startQrExpirationCountdown()
                waitForQrPairing(session)
            } catch (_: CancellationException) {
                // Tab switches or window close intentionally cancel the current QR session.
            } catch (t: Throwable) {
                status = Status.Failed(t.message)
            } finally {
                initializeJob = null
            }
        }
    }

    private fun waitForQrPairing(session: QrPairingSession) {
        qrWaitJob?.cancel()
        qrWaitJob = modelScope.launch {
            status = Status.WaitingForQr
            try {
                val result = adbClient.completeQrPairing(session)
                if (!result.isOk) {
                    if (currentTab == Tab.QrCode) {
                        qrError = result.errorMessage
                        scheduleQrSessionRefresh()
                        return@launch
                    }

                    status = Status.Failed(result.errorMessage)
                    return@launch
                }

                val device = result.data ?: run {
                    if (currentTab == Tab.QrCode) {
                        qrError = null
                        scheduleQrSessionRefresh()
                        return@launch
                    }

                    status = Status.Failed(null)
                    return@launch
                }

                appState.pendingDeviceSelectionCoordinator.enqueueConnectedDevice(
                    address = device.address,
                    serviceName = device.serviceName
                )
                onPairingCompleted?.invoke()
            } catch (_: CancellationException) {
                // QR pairing is long-lived by design; cancellation simply means the user switched tabs.
            } catch (t: Throwable) {
                status = Status.Failed(t.message)
            } finally {
                qrWaitJob = null
            }
        }
    }

    /**
     * Keeps the available pairing-endpoint list hot only while the pairing-code tab is active.
     */
    private fun startObservePairingDevices() {
        if (pairingDevicesJob?.isActive == true) return

        isObservingPairingDevices = true
        pairingDevicesJob = modelScope.launch {
            try {
                adbClient.observePairingDevices().collect { result ->
                    if (!result.isOk) {
                        status = Status.Failed(result.errorMessage)
                        pairingDevices.clear()
                        return@collect
                    }

                    status = Status.None
                    pairingDevices.clear()
                    pairingDevices += result.data.orEmpty()
                }
            } catch (_: CancellationException) {
                // Normal when the user leaves the tab or closes the dialog.
            } catch (t: Throwable) {
                status = Status.Failed(t.message)
                pairingDevices.clear()
            } finally {
                isObservingPairingDevices = false
                pairingDevicesJob = null
            }
        }
    }

    /**
     * QR sessions should not stay on screen indefinitely because the device-side pairing service
     * is short-lived. We track a visible TTL and regenerate automatically once it expires so the
     * user always scans a live code instead of a stale one that will fail immediately.
     */
    private fun startQrExpirationCountdown() {
        qrExpirationJob?.cancel()
        qrExpirationJob = modelScope.launch {
            qrRemainingSeconds = QR_SESSION_TTL_SECONDS

            while (qrRemainingSeconds > 0) {
                delay(1_000L)
                qrRemainingSeconds -= 1
            }

            if (currentTab == Tab.QrCode)
                scheduleQrSessionRefresh()
        }
    }

    private fun scheduleQrSessionRefresh() {
        modelScope.launch {
            if (currentTab == Tab.QrCode)
                prepareQrSession(clearQrError = false)
        }
    }

    private fun stopQrWorkflow(clearSession: Boolean) {
        initializeJob?.cancel()
        qrWaitJob?.cancel()
        qrExpirationJob?.cancel()
        qrRemainingSeconds = 0
        if (clearSession) qrSession = null
    }

    private fun stopPairingDeviceObservation(clearDevices: Boolean) {
        pairingDevicesJob?.cancel()
        if (clearDevices) pairingDevices.clear()
    }

    fun dispose() {
        modelScope.cancel()
    }
}