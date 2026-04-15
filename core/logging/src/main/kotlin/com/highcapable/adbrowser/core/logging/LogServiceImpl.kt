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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import java.time.Instant

/**
 * Thread-safe in-memory log store implementation.
 */
@LoggingScope
@Inject
class LogServiceImpl : LogService {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())

    override val entries get() = _entries.value

    override fun log(level: LogLevel, category: String, message: String) {
        _entries.update { current ->
            listOf(LogEntry(Instant.now(), level, category, message)) + current
        }
    }

    override fun observeEntries() = _entries.asStateFlow()
}