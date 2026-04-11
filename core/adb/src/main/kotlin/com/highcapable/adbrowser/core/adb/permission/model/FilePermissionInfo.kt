package com.highcapable.adbrowser.core.adb.permission.model

/**
 * Parsed file permission details from `ls -ld`.
 */
data class FilePermissionInfo(
    val symbolicPermission: String,
    val numericPermission: Int
)