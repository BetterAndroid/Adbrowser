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
package com.highcapable.adbrowser.core.adb.model

/**
 * Represents a generic operation result used by backend write operations.
 *
 * This model is intentionally tiny and stable because it is passed frequently between
 * repository/service and ViewModel layers.
 */
data class OperationResult<T>(
    val isOk: Boolean,
    val errorMessage: String? = null,
    val data: T? = null
) {

    companion object {

        /**
         * Creates a successful operation result.
         */
        fun ok() = OperationResult<Unit>(isOk = true)

        /**
         * Creates a successful operation result with data.
         */
        fun <T> success(data: T?) = OperationResult(isOk = true, data = data)

        /**
         * Creates a failed operation result with a readable error message.
         */
        fun error(response: AdbResponse) = OperationResult<Unit>(isOk = false, errorMessage = response.message)

        /**
         * Creates a failed operation result with a readable error message.
         */
        fun error(errorMessage: String) = OperationResult<Unit>(isOk = false, errorMessage = errorMessage)

        /**
         * Creates a failed operation result with a readable error message with data.
         */
        fun <T> failure(response: AdbResponse) = OperationResult<T>(isOk = false, errorMessage = response.message)

        /**
         * Creates a failed operation result with a readable error message with data.
         */
        fun <T> failure(errorMessage: String) = OperationResult<T>(isOk = false, errorMessage = errorMessage)
    }
}