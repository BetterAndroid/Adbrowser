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
 * This file is created by fankes on 2026/4/2.
 */
package com.highcapable.adbrowser.core.logging

import kotlinx.coroutines.flow.Flow

/**
 * Provides a stable logging abstraction used by backend and MVVM state layers.
 */
interface LogService {

    /**
     * Returns logs in reverse chronological order.
     */
    val entries: List<LogEntry>

    /**
     * Returns the total amount of buffered logs before any UI-side filtering is applied.
     */
    val entryCount: Int

    /**
     * Writes a log entry to the in-memory log store.
     *
     * This function is called very frequently by ADB/FS operations, so implementations
     * should keep it lightweight and thread-safe.
     */
    fun log(level: LogLevel, category: String, message: String)

    /**
     * Removes all currently buffered log entries.
     *
     * The log window uses this for "Clear" so the underlying source of truth must be reset as
     * well, otherwise the UI would immediately rehydrate from stale in-memory data.
     */
    fun clear()

    /**
     * Returns whether the given level is currently enabled in the shared runtime filter.
     *
     * The log viewer keeps these toggles only for the current app run, so the logging service is
     * the right place to own them instead of duplicating the same filter state in every window.
     */
    fun isLevelVisible(level: LogLevel): Boolean

    /**
     * Updates the shared runtime log-level filter.
     */
    fun setLevelVisible(level: LogLevel, visible: Boolean)

    /**
     * Observes the current log snapshot and every subsequent append.
     *
     * The returned list keeps the same reverse-chronological ordering as [entries], which lets
     * UI consumers render logs without polling or manual refresh buttons.
     */
    fun observeEntries(): Flow<List<LogEntry>>

    /**
     * Observes the current visible log snapshot after the runtime level filter is applied.
     */
    fun observeVisibleEntries(): Flow<List<LogEntry>>

    /**
     * Observes the total buffered log count before filtering.
     *
     * This lets UI enablement react to hidden logs as well without re-subscribing to the full raw
     * list and rebuilding another copy in the view model.
     */
    fun observeEntryCount(): Flow<Int>

    /**
     * Observes the current runtime log-level filter.
     *
     * The log viewer menu uses this to keep checkbox state in sync without reopening the window.
     */
    fun observeVisibleLevels(): Flow<Set<LogLevel>>
}