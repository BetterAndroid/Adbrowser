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
package com.highcapable.adbrowser.app.cl.coordinator

import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.core.common.network.NetworkEndpoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Coordinates "retroactive selection" requests for devices that have just been connected.
 *
 * The queue is owned by [AppState], so it survives transient dialog/window recomposition without
 * forcing any `ViewModel` instance into static state. Main-stage models can subscribe while they
 * are alive and consume matching entries when refreshed device snapshots finally contain the target.
 */
class PendingDeviceSelectionCoordinator {

    enum class Reason {
        Connected
    }

    data class PendingSelection(
        val serial: String,
        val reason: Reason
    )

    private val pendingSelections = linkedMapOf<String, PendingSelection>()
    private val pendingAnnouncements = MutableSharedFlow<PendingSelection>(extraBufferCapacity = 8)

    /**
     * Enqueues a newly connected network device and notifies active stage models immediately.
     */
    fun enqueueConnectedDevice(address: String) {
        val serial = NetworkEndpoint.normalize(
            value = address,
            defaultPort = NetworkEndpoint.DEFAULT_ADB_PORT
        ) ?: address.trim()
        val selection = PendingSelection(serial = serial, reason = Reason.Connected)

        synchronized(pendingSelections) {
            pendingSelections[serial] = selection
        }
        pendingAnnouncements.tryEmit(selection)
    }

    /**
     * Consumes the first queued selection whose serial is now present in [serials].
     *
     * This is intentionally synchronous because the caller already owns the refreshed device
     * snapshot; the coordinator only needs an in-memory lookup before the stage applies selection.
     */
    fun consumePendingSelection(serials: Iterable<String>): PendingSelection? {
        synchronized(pendingSelections) {
            val normalizedSerials = serials.toList()
            val iterator = pendingSelections.entries.iterator()
            while (iterator.hasNext()) {
                val (_, selection) = iterator.next()
                if (normalizedSerials.none { it.equals(selection.serial, ignoreCase = true) }) continue

                iterator.remove()
                return selection
            }
        }

        return null
    }

    /**
     * Emits newly queued selections to any currently active stage model.
     */
    fun observeAnnouncements() = pendingAnnouncements.asSharedFlow()
}