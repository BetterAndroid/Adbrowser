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
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
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

    private companion object {

        const val MDNS_CONNECT_SERVICE_SUFFIX = "._adb-tls-connect._tcp"
    }

    enum class Reason {
        Connected
    }

    data class PendingSelection(
        val serial: String,
        val host: String?,
        val serviceName: String?,
        val reason: Reason
    )

    private data class SerialCandidate(
        val serial: String,
        val host: String?,
        val serviceName: String?
    )

    /**
     * Carries both the original pending request and the actual device serial that satisfied it.
     *
     * A retrospective match may resolve through host fallback, so [matchedSerial] can differ from
     * [PendingSelection.serial] when ADB exposes the final transport under an mDNS-based serial.
     */
    data class ResolvedSelection(
        val selection: PendingSelection,
        val matchedSerial: String
    )

    /**
     * Centralizes device-target selection after each refresh so stage models only need to apply the
     * returned decision instead of reimplementing pending / previous / default fallback logic.
     */
    data class DeviceSelectionResolution(
        val targetDevice: AndroidDeviceItem?,
        val pendingSelection: PendingSelection?,
        val pendingDevice: AndroidDeviceItem?
    )

    private val pendingSelections = linkedMapOf<String, PendingSelection>()
    private val pendingAnnouncements = MutableSharedFlow<PendingSelection>(extraBufferCapacity = 8)

    /**
     * Enqueues a newly connected network device and notifies active stage models immediately.
     */
    fun enqueueConnectedDevice(address: String, serviceName: String? = null) {
        val endpoint = NetworkEndpoint.parse(
            value = address,
            defaultPort = NetworkEndpoint.DEFAULT_ADB_PORT
        )
        val serial = endpoint?.address ?: address.trim()
        val selection = PendingSelection(
            serial = serial,
            host = endpoint?.host,
            serviceName = serviceName?.trim()?.takeIf { it.isNotEmpty() },
            reason = Reason.Connected
        )

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
     *
     * Pairing flows advertise one network endpoint for `adb pair`, but the actual connected device
     * may show up later under a sibling `host:port` serial. We therefore fall back to host-level
     * matching when an exact serial match is unavailable.
     */
    fun consumePendingSelection(serials: Iterable<String>): ResolvedSelection? {
        synchronized(pendingSelections) {
            val candidates = serials.map { serial ->
                SerialCandidate(
                    serial = serial,
                    host = NetworkEndpoint.parse(serial)?.host,
                    serviceName = parseMdnsConnectServiceName(serial)
                )
            }
            val iterator = pendingSelections.entries.iterator()
            while (iterator.hasNext()) {
                val (_, selection) = iterator.next()
                val matchedCandidate = candidates.firstOrNull {
                    it.serial.equals(selection.serial, ignoreCase = true)
                } ?: continue

                iterator.remove()
                return ResolvedSelection(
                    selection = selection,
                    matchedSerial = matchedCandidate.serial
                )
            }

            val serviceIterator = pendingSelections.entries.iterator()
            while (serviceIterator.hasNext()) {
                val (_, selection) = serviceIterator.next()
                val serviceName = selection.serviceName ?: continue
                val matchedCandidate = candidates.firstOrNull {
                    matchesPendingServiceName(
                        candidateServiceName = it.serviceName,
                        pendingServiceName = serviceName
                    )
                } ?: continue

                serviceIterator.remove()
                return ResolvedSelection(
                    selection = selection,
                    matchedSerial = matchedCandidate.serial
                )
            }

            if (candidates.none { it.host != null }) return null

            val hostIterator = pendingSelections.entries.iterator()
            while (hostIterator.hasNext()) {
                val (_, selection) = hostIterator.next()
                val host = selection.host ?: continue
                val matchedCandidate = candidates.firstOrNull {
                    it.host.equals(host, ignoreCase = true)
                } ?: continue

                hostIterator.remove()
                return ResolvedSelection(
                    selection = selection,
                    matchedSerial = matchedCandidate.serial
                )
            }
        }

        return null
    }

    /**
     * Resolves the device that should become active after a list refresh.
     *
     * Resolution order:
     * 1. A newly connected pending device that finally appeared.
     * 2. The previously selected device, rebound to the refreshed list instance.
     * 3. The caller-provided default fallback.
     */
    fun resolveDeviceSelection(
        devices: List<AndroidDeviceItem>,
        previousSelectedDevice: AndroidDeviceItem?,
        selectDefaultDevice: () -> AndroidDeviceItem?
    ): DeviceSelectionResolution {
        val resolvedPendingSelection = consumePendingSelection(devices.map(AndroidDeviceItem::serial))
        val pendingSelection = resolvedPendingSelection?.selection
        val pendingDevice = resolvedPendingSelection
            ?.let { resolved -> devices.firstOrNull { it.serial.equals(resolved.matchedSerial, ignoreCase = true) } }
        val refreshedPreviousSelectedDevice = previousSelectedDevice
            ?.let { previous -> devices.firstOrNull { it == previous } }

        return DeviceSelectionResolution(
            targetDevice = pendingDevice
                ?: refreshedPreviousSelectedDevice
                ?: selectDefaultDevice(),
            pendingSelection = pendingSelection,
            pendingDevice = pendingDevice
        )
    }

    /**
     * Emits newly queued selections to any currently active stage model.
     */
    fun observeAnnouncements() = pendingAnnouncements.asSharedFlow()

    private fun parseMdnsConnectServiceName(serial: String) =
        serial.substringBefore(MDNS_CONNECT_SERVICE_SUFFIX, missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() && serial.endsWith(MDNS_CONNECT_SERVICE_SUFFIX, ignoreCase = true) }

    /**
     * `adb pair` can report only the stable device guid (`adb-<guid>`), while the final connect
     * serial may append an ephemeral suffix such as `adb-<guid>-<suffix>._adb-tls-connect._tcp`.
     * Treat that connect-service instance as belonging to the same pending request.
     */
    private fun matchesPendingServiceName(candidateServiceName: String?, pendingServiceName: String): Boolean {
        val candidate = candidateServiceName?.trim().orEmpty()
        val pending = pendingServiceName.trim()
        if (candidate.isEmpty() || pending.isEmpty()) return false

        return candidate.equals(pending, ignoreCase = true) ||
            candidate.startsWith("$pending-", ignoreCase = true)
    }
}