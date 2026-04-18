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

import com.highcapable.adbrowser.core.logging.di.LoggingScope
import com.highcapable.adbrowser.core.logging.generated.AdbrowserProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory log store implementation.
 */
@LoggingScope
@Inject
class LogServiceImpl : LogService {

    private companion object {

        const val DEFAULT_LOGGER_NAME = AdbrowserProperties.PROJECT_NAME
    }

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _entryCount = MutableStateFlow(0)

    private val _visibleEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _visibleLevels = MutableStateFlow(LogLevel.entries.toSet())

    private val consoleLoggers = ConcurrentHashMap<String, Logger>()

    override val entries get() = _entries.value
    override val entryCount get() = _entryCount.value

    override fun log(level: LogLevel, category: String, message: String) {
        val entry = LogEntry(Instant.now(), level, category, message)

        // Log4j owns console rendering so timestamp / color / category formatting stay centralized
        // in log4j2.xml instead of being duplicated in every call site.
        loggerOf(category).log(level.toLog4jLevel(), message)

        _entries.update { current -> listOf(entry) + current }
        _entryCount.value += 1

        // Most log appends only need an O(1) prepend. We only rebuild from the full raw snapshot
        // when the filter itself changes, which keeps the hot logging path lightweight.
        if (level in _visibleLevels.value) _visibleEntries.update { current -> listOf(entry) + current }
    }

    override fun clear() {
        _entries.value = emptyList()
        _visibleEntries.value = emptyList()
        _entryCount.value = 0
    }

    override fun isLevelVisible(level: LogLevel) = level in _visibleLevels.value

    override fun setLevelVisible(level: LogLevel, visible: Boolean) {
        val updatedLevels = _visibleLevels.value.toMutableSet().apply {
            if (visible) add(level) else remove(level)
        }
        if (updatedLevels == _visibleLevels.value) return

        _visibleLevels.value = updatedLevels
        rebuildVisibleEntries()
    }

    override fun observeEntries() = _entries.asStateFlow()
    override fun observeVisibleEntries() = _visibleEntries.asStateFlow()
    override fun observeEntryCount() = _entryCount.asStateFlow()
    override fun observeVisibleLevels() = _visibleLevels.asStateFlow()

    /**
     * Rebuilds the filtered snapshot from the raw source of truth after filter toggles change.
     *
     * Filtering is centralized here so consumers never need to keep a second copy of the full log
     * list just to drive checkbox-based level filtering.
     */
    private fun rebuildVisibleEntries() {
        val visibleLevels = _visibleLevels.value
        _visibleEntries.value = _entries.value.filter { it.level in visibleLevels }
    }

    private fun loggerOf(category: String): Logger {
        val loggerName = category.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { "$DEFAULT_LOGGER_NAME.$it" }
            ?: DEFAULT_LOGGER_NAME
        return consoleLoggers.getOrPut(loggerName) { LogManager.getLogger(loggerName) }
    }

    private fun LogLevel.toLog4jLevel() = when (this) {
        LogLevel.Trace -> Level.TRACE
        LogLevel.Information -> Level.INFO
        LogLevel.Warning -> Level.WARN
        LogLevel.Error -> Level.ERROR
    }
}