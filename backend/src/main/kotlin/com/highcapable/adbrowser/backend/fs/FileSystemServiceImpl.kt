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
package com.highcapable.adbrowser.backend.fs

import com.highcapable.adbrowser.backend.adb.model.AndroidDevice
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.shell.ShellCommandExecutor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * File-system service using ADB shell abstraction.
 */
class FileSystemServiceImpl(
    private val shellCommandExecutor: ShellCommandExecutor,
    private val logService: LogService
) : FileSystemService {

    private companion object {

        val dateTimePatterns = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )

        val monthTimePatterns = listOf(
            DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.US),
            DateTimeFormatter.ofPattern("MMM dd HH:mm", Locale.US)
        )
        val monthTimeWithYearPatterns = listOf(
            DateTimeFormatter.ofPattern("MMM d HH:mm yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MMM dd HH:mm yyyy", Locale.US)
        )

        val monthYearPatterns = listOf(
            DateTimeFormatter.ofPattern("MMM d yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US)
        )
    }

    override suspend fun list(device: AndroidDevice, path: String): List<DeviceFileEntry> {
        logService.log(LogLevel.Trace, "FS", "List path '$path' for '${device.serial}'.")
        val listPath = normalizeDirectoryListPath(path)
        val output = shellCommandExecutor.executeFileOperation(device, "ls -la '${escapeShell(listPath)}'")
        val entries = parseLsOutput(path, output).toMutableList()
        resolveSymlinkDirectoryFlags(device, entries)

        if (entries.isEmpty())
            logService.log(LogLevel.Warning, "FS", "No entries parsed from '$path'. Raw output: $output")

        return entries
    }

    override suspend fun search(device: AndroidDevice, path: String, keyword: String): List<DeviceFileEntry> {
        val entries = list(device, path)
        val result = entries.filter { it.name.contains(keyword, ignoreCase = true) }
        logService.log(LogLevel.Information, "FS", "Search '$keyword' returned ${result.size} entries.")

        return result
    }

    override suspend fun createFolder(device: AndroidDevice, parentPath: String, folderName: String): OperationResult = runOperation {
        val parent = escapeShell(parentPath.trimEnd('/'))
        val name = escapeShell(folderName)
        shellCommandExecutor.executeFileOperation(device, "mkdir -p '$parent/$name'")
        logService.log(LogLevel.Information, "FS", "Created folder '$folderName' under '$parentPath'.")
    }

    override suspend fun delete(device: AndroidDevice, path: String): OperationResult = runOperation {
        shellCommandExecutor.executeFileOperation(device, "rm -rf '${escapeShell(path)}'")
        logService.log(LogLevel.Warning, "FS", "Deleted path '$path'.")
    }

    override suspend fun rename(device: AndroidDevice, path: String, newName: String): OperationResult = runOperation {
        val targetPath = buildTargetPath(path, newName)
        shellCommandExecutor.executeFileOperation(
            device,
            "mv '${escapeShell(path)}' '${escapeShell(targetPath)}'"
        )
        logService.log(LogLevel.Information, "FS", "Renamed '$path' to '$targetPath'.")
    }

    override suspend fun copy(device: AndroidDevice, sourcePath: String, targetPath: String): OperationResult = runOperation {
        shellCommandExecutor.executeFileOperation(
            device,
            "cp -a '${escapeShell(sourcePath)}' '${escapeShell(targetPath)}'"
        )
        logService.log(LogLevel.Information, "FS", "Copied '$sourcePath' to '$targetPath'.")
    }

    override suspend fun move(device: AndroidDevice, sourcePath: String, targetPath: String): OperationResult = runOperation {
        shellCommandExecutor.executeFileOperation(
            device,
            "mv '${escapeShell(sourcePath)}' '${escapeShell(targetPath)}'"
        )
        logService.log(LogLevel.Information, "FS", "Moved '$sourcePath' to '$targetPath'.")
    }

    private suspend fun runOperation(block: suspend () -> Unit) = try {
        block()
        OperationResult.success()
    } catch (t: Throwable) {
        val message = t.message ?: t::class.simpleName ?: "Unknown error"
        logService.log(LogLevel.Error, "FS", message)
        OperationResult.failure(message)
    }

    private fun escapeShell(value: String): String = value.replace("'", "'\\''")

    private fun normalizeDirectoryListPath(path: String): String {
        if (path.isBlank() || path == "/") return "/"
        return "${path.trimEnd('/')}/"
    }

    private fun buildTargetPath(sourcePath: String, newName: String): String {
        val normalized = sourcePath.trimEnd('/')
        val index = normalized.lastIndexOf('/')
        if (index <= 0) return "/$newName"
        val parent = normalized.take(index)

        return "$parent/$newName"
    }

    private fun parseLsOutput(parentPath: String, output: String): List<DeviceFileEntry> {
        val result = mutableListOf<DeviceFileEntry>()
        val lines = output.lineSequence().filter { it.isNotBlank() }.toList()

        lines.forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("total ", ignoreCase = true) || line.length < 11) return@forEach

            val permission = line.take(10)
            if (permission.first() !in charArrayOf('d', '-', 'l')) return@forEach

            val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (tokens.size < 8) return@forEach
            val size = tokens[4].toLongOrNull() ?: return@forEach

            val nameStartIndex = resolveNameStartIndex(tokens)
            if (nameStartIndex !in tokens.indices) return@forEach

            var name = tokens.subList(nameStartIndex, tokens.size).joinToString(" ")
            if (name.isBlank() || name == "." || name == ".." || name.endsWith(" ->")) return@forEach
            if (name.contains(" -> ")) name = name.substringBefore(" -> ")

            val isSymlink = permission.first() == 'l'
            val modifiedTime = parseModifiedTime(tokens, nameStartIndex)

            result += DeviceFileEntry(
                path = parentPath,
                name = name,
                isDirectory = permission.first() == 'd',
                isSymlink = isSymlink,
                size = size,
                modifiedTime = modifiedTime,
                permission = permission.substring(1)
            )
        }

        return result
    }

    private fun parseModifiedTime(tokens: List<String>, nameStartIndex: Int): Instant {
        if (nameStartIndex <= 5) return Instant.now()

        val combined = tokens.subList(5, nameStartIndex).joinToString(" ")
        val zoneId = ZoneId.systemDefault()

        for (formatter in dateTimePatterns) {
            val parsed = runCatching { LocalDateTime.parse(combined, formatter) }.getOrNull()
            if (parsed != null) return parsed.atZone(zoneId).toInstant()
        }

        for (formatter in monthTimePatterns) {
            val parsed = runCatching {
                LocalDateTime.parse(
                    "$combined ${LocalDate.now().year}",
                    monthTimeWithYearPatterns[monthTimePatterns.indexOf(formatter)]
                )
            }.getOrNull()
            if (parsed != null) return parsed.atZone(zoneId).toInstant()
        }

        for (formatter in monthYearPatterns) {
            val parsed = runCatching { LocalDate.parse(combined, formatter) }.getOrNull()
            if (parsed != null) return LocalDateTime.of(parsed, LocalTime.MIDNIGHT).atZone(zoneId).toInstant()
        }

        return Instant.now()
    }

    private fun resolveNameStartIndex(tokens: List<String>) = when {
        tokens.size >= 8 && isIsoDateToken(tokens[5]) && isClockToken(tokens[6]) -> 7
        tokens.size >= 9 && isMonthToken(tokens[5]) -> 8
        else -> minOf(7, tokens.lastIndex)
    }

    private fun isIsoDateToken(value: String) = Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)

    private fun isClockToken(value: String) = Regex("\\d{2}:\\d{2}(:\\d{2})?").matches(value)

    private fun isMonthToken(value: String) = value.length == 3 && value.all { it.isLetter() }

    /**
     * Resolves symlink directory flags in batches to reduce ADB round-trips.
     */
    private suspend fun resolveSymlinkDirectoryFlags(device: AndroidDevice, entries: MutableList<DeviceFileEntry>) {
        val symlinkIndexes = entries.indices.filter { entries[it].isSymlink }
        if (symlinkIndexes.isEmpty()) return

        val chunkSize = 32
        for (batch in symlinkIndexes.chunked(chunkSize)) {
            val command = buildSymlinkDirectoryCheckCommand(entries, batch)
            val output = shellCommandExecutor.executeFileOperation(device, command)
            applySymlinkDirectoryCheckResult(entries, batch, output)
        }
    }

    private fun buildSymlinkDirectoryCheckCommand(entries: List<DeviceFileEntry>, batchIndexes: List<Int>): String {
        val segments = mutableListOf<String>()
        for ((localIndex, entryIndex) in batchIndexes.withIndex()) {
            val entry = entries[entryIndex]
            val fullPath = if (entry.path == "/") "/${entry.name}" else "${entry.path.trimEnd('/')}/${entry.name}"
            val escaped = escapeShell(fullPath)
            segments += "if [ -d '$escaped' ]; then echo '$localIndex:1'; else echo '$localIndex:0'; fi"
        }

        return segments.joinToString("; ")
    }

    private fun applySymlinkDirectoryCheckResult(entries: MutableList<DeviceFileEntry>, batchIndexes: List<Int>, output: String) {
        val resultMap = mutableMapOf<Int, Boolean>()
        for (line in output.lineSequence().map { it.trim() }.filter { it.isNotBlank() }) {
            val separator = line.indexOf(':')
            if (separator <= 0 || separator >= line.lastIndex) continue
            val localIndex = line.take(separator).toIntOrNull() ?: continue
            resultMap[localIndex] = line.substring(separator + 1) == "1"
        }

        for ((localIndex, entryIndex) in batchIndexes.withIndex()) {
            val isDirectory = resultMap[localIndex] == true
            entries[entryIndex] = entries[entryIndex].copy(isDirectory = isDirectory)
        }
    }
}