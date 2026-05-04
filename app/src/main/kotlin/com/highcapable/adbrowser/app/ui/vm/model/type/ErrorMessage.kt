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
 * This file is created by fankes on 2026/5/3.
 */
package com.highcapable.adbrowser.app.ui.vm.model.type

import com.highcapable.adbrowser.core.adb.model.OperationResult
import com.highcapable.adbrowser.core.common.utils.extension.normalized

/**
 * Centralized backend message classification used by ViewModels.
 *
 * The backend still returns a mix of raw ADB and shell error strings, so these checks stay
 * tolerant and substring-based for now. Keeping them in one place avoids every dialog or stage
 * growing its own slightly different parsing rules.
 */
object ErrorMessage {

    const val INVALID_PERMISSION_TOKEN = "__invalid_permission__"
    const val UNKNOWN_ERROR_TOKEN = "__unknown_error__"

    private const val DEVICE_UNAUTHORIZED = "device unauthorized"
    private const val FAILED_TO_AUTHENTICATE_TO = "failed to authenticate to"
    private const val DEVICE_OFFLINE = "device offline"
    private const val DEVICE = "device"
    private const val DEVICE_QUOTE_PREFIX = "device '"
    private const val DEVICE_NOT_FOUND_SUFFIX = "' not found"
    private const val NO_SUCH_FILE_OR_DIRECTORY = "no such file or directory"
    private const val NOT_FOUND = "not found"
    private const val PERMISSION_DENIED = "permission denied"
    private const val OPERATION_NOT_PERMITTED = "operation not permitted"
    private const val NOT_PERMITTED = "not permitted"
    private const val ALREADY_EXISTS = "already exists"
    private const val ERROR_PREFIX = "error:"

    enum class AdbKind {
        DeviceUnauthorized,
        DeviceOffline,
        DeviceNotFound,
        PathNotFound,
        PermissionDenied,
        Unknown
    }

    enum class FileKind {
        AlreadyExists,
        PermissionDenied,
        Unknown
    }

    /**
     * Internal UI tokens are kept separate from backend message kinds.
     *
     * They represent synthetic states created inside the app when a dialog needs to carry a
     * stable machine-readable meaning without forcing the UI layer to parse free-form text.
     */
    enum class InternalKind {
        InvalidPermission,
        Unknown
    }

    /**
     * ADB and shell failures overlap in wording, so ordering matters here.
     *
     * We classify device-transport states first, then file/path failures, and only then fall back
     * to a generic unknown bucket.
     */
    fun resolveAdbKind(message: String?) = with(message.normalized()) {
        when {
            DEVICE_UNAUTHORIZED in this || FAILED_TO_AUTHENTICATE_TO in this -> AdbKind.DeviceUnauthorized
            DEVICE_OFFLINE in this -> AdbKind.DeviceOffline
            isDeviceNotFound(this) -> AdbKind.DeviceNotFound
            NO_SUCH_FILE_OR_DIRECTORY in this || NOT_FOUND in this -> AdbKind.PathNotFound
            isPermissionDenied(this) -> AdbKind.PermissionDenied
            else -> AdbKind.Unknown
        }
    }

    /**
     * File write operations currently only need a smaller set of UI-facing buckets.
     */
    fun resolveFileKind(message: String?) = with(message.normalized()) {
        when {
            ALREADY_EXISTS in this -> FileKind.AlreadyExists
            isPermissionDenied(this) -> FileKind.PermissionDenied
            else -> FileKind.Unknown
        }
    }

    fun resolveInternalKind(message: String?) = when {
        message == INVALID_PERMISSION_TOKEN -> InternalKind.InvalidPermission
        message.isNullOrBlank() || message == UNKNOWN_ERROR_TOKEN -> InternalKind.Unknown
        else -> null
    }

    private fun isDeviceNotFound(message: String) =
        (DEVICE_QUOTE_PREFIX in message && DEVICE_NOT_FOUND_SUFFIX in message) ||
            (DEVICE in message && NOT_FOUND in message && ERROR_PREFIX in message)

    private fun isPermissionDenied(message: String) =
        PERMISSION_DENIED in message ||
            OPERATION_NOT_PERMITTED in message ||
            NOT_PERMITTED in message
}

fun OperationResult<*>.resolveAdbErrorKind() = ErrorMessage.resolveAdbKind(errorMessage)
fun OperationResult<*>.resolveFileErrorKind() = ErrorMessage.resolveFileKind(errorMessage)
fun OperationResult<*>.orUnknownErrorToken() = errorMessage?.takeIf { it.isNotBlank() } ?: ErrorMessage.UNKNOWN_ERROR_TOKEN