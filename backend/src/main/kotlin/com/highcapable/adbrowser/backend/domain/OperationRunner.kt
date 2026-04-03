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
package com.highcapable.adbrowser.backend.domain

import com.highcapable.adbrowser.backend.domain.OperationResult.Companion.failure
import com.highcapable.adbrowser.backend.domain.OperationResult.Companion.success
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService

/**
 * A helper class to execute ADB operations and handle their results uniformly.
 */
internal class OperationRunner(
    private val logService: LogService,
    private val category: String
) {

    /**
     * Executes a block of code that performs an ADB operation, captures any exceptions,
     * and returns an OperationResult based on the outcome.
     */
    suspend inline fun exec(block: suspend () -> Pair<Unit?, AdbResponse>) = exec<Unit>(block)

    /**
     * Executes a block of code that performs an ADB operation, captures any exceptions,
     * and returns an OperationResult based on the outcome.
     */
    @JvmName("execTyped")
    suspend inline fun <T> exec(block: suspend () -> Pair<T?, AdbResponse>) = try {
        val (data, response) = block()

        if (response.isOk)
            success(data)
        else failure(response)
    } catch (t: Throwable) {
        val message = t.message ?: t::class.simpleName ?: "Unknown error"
        logService.log(LogLevel.Error, category, message)
        failure(message)
    }
}