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
import com.highcapable.adbrowser.backend.domain.OperationRunner
import com.highcapable.adbrowser.backend.fs.model.DeviceFileEntry
import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.backend.shell.AdbShellCommandExecutor
import me.tatarka.inject.annotations.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * File-system service using ADB shell abstraction.
 */
@Inject
class FileSystemServiceImpl(
    private val shellCommandExecutor: AdbShellCommandExecutor,
    private val logService: LogService
) : FileSystemService {

    private companion object {

        const val CATEGORY = "FS"

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

        val statDateTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val statTimeRegex = """^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))? ([+-]\d{2}:?\d{2})$""".toRegex()

        val isoDateTokenPattern = "\\d{4}-\\d{2}-\\d{2}".toRegex()
        val clockTokenPattern = "\\d{2}:\\d{2}(:\\d{2})?".toRegex()
    }

    private val runner = OperationRunner(logService, CATEGORY)

    override suspend fun list(device: AndroidDevice, path: String) = runner.exec<List<DeviceFileEntry>> {
        logService.log(LogLevel.Trace, CATEGORY, "List path '$path' for '$device'.")
        val listPath = normalizeDirectoryListPath(path)
        val response = shellCommandExecutor.executeFileOperation(device, "ls -la '${escapeShell(listPath)}'")

        if (response.isOk) {
            val entries = parseLsOutput(path, response.standardOutput).toMutableList()
            resolveSymlinkDirectoryFlags(device, entries)
            enrichEntriesByStat(device, entries)

            if (entries.isEmpty())
                logService.log(LogLevel.Warning, CATEGORY, "No entries parsed from '$path'. Raw output: ${response.message}")

            entries to response
        } else null to response
    }

    override suspend fun search(device: AndroidDevice, path: String, keyword: String) = runner.exec<List<DeviceFileEntry>> {
        // TODO: Implement search by running `find` command and parsing the output.
        //       This is more efficient than listing all files and filtering on client side, especially for large directories.
        TODO()
    }

    override suspend fun createFolder(device: AndroidDevice, parentPath: String, folderName: String) = runner.exec {
        val parent = escapeShell(parentPath.trimEnd('/'))
        val name = escapeShell(folderName)
        val response = shellCommandExecutor.executeFileOperation(device, "mkdir -p '$parent/$name'")

        if (response.isOk) logService.log(LogLevel.Information, CATEGORY, "Created folder '$folderName' under '$parentPath'.")
        null to response
    }

    override suspend fun delete(device: AndroidDevice, path: String) = runner.exec {
        val response = shellCommandExecutor.executeFileOperation(device, "rm -rf '${escapeShell(path)}'")

        if (response.isOk) logService.log(LogLevel.Warning, CATEGORY, "Deleted path '$path'.")
        null to response
    }

    override suspend fun rename(device: AndroidDevice, path: String, newName: String) = runner.exec {
        val targetPath = buildTargetPath(path, newName)
        val response = shellCommandExecutor.executeFileOperation(
            device,
            "mv '${escapeShell(path)}' '${escapeShell(targetPath)}'"
        )

        if (response.isOk) logService.log(LogLevel.Information, CATEGORY, "Renamed '$path' to '$targetPath'.")
        null to response
    }

    override suspend fun copy(device: AndroidDevice, sourcePath: String, targetPath: String) = runner.exec {
        val response = shellCommandExecutor.executeFileOperation(
            device,
            "cp -a '${escapeShell(sourcePath)}' '${escapeShell(targetPath)}'"
        )

        if (response.isOk) logService.log(LogLevel.Information, CATEGORY, "Copied '$sourcePath' to '$targetPath'.")
        null to response
    }

    override suspend fun move(device: AndroidDevice, sourcePath: String, targetPath: String) = runner.exec {
        val response = shellCommandExecutor.executeFileOperation(
            device,
            "mv '${escapeShell(sourcePath)}' '${escapeShell(targetPath)}'"
        )

        if (response.isOk) logService.log(LogLevel.Information, CATEGORY, "Moved '$sourcePath' to '$targetPath'.")
        null to response
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
            val modifiedAt = parseModifiedAt(tokens, nameStartIndex)

            result += DeviceFileEntry(
                path = parentPath,
                name = name,
                isDirectory = permission.first() == 'd',
                isSymlink = isSymlink,
                size = size,
                createdAt = modifiedAt,
                modifiedAt = modifiedAt,
                permission = permission.substring(1)
            )
        }

        return result
    }

    private fun parseModifiedAt(tokens: List<String>, nameStartIndex: Int): Instant {
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

    private fun isIsoDateToken(value: String) = isoDateTokenPattern.matches(value)
    private fun isClockToken(value: String) = clockTokenPattern.matches(value)
    private fun isMonthToken(value: String) = value.length == 3 && value.all { it.isLetter() }

    /**
     * Uses `stat` output to enrich timestamps because `ls -la` is not enough for creation time.
     *
     * Rule:
     * - createdAt = Birth
     * - fallback createdAt = Access (when Birth is unavailable)
     */
    private suspend fun enrichEntriesByStat(device: AndroidDevice, entries: MutableList<DeviceFileEntry>) {
        if (entries.isEmpty()) return

        val chunkSize = 16
        entries.indices.chunked(chunkSize).forEach { batch ->
            val command = buildStatBatchCommand(entries, batch)
            val response = runCatching { shellCommandExecutor.executeFileOperation(device, command) }.getOrNull()
            if (response == null || !response.isOk) {
                logService.log(LogLevel.Warning, CATEGORY, "Stat enrich skipped for batch due to command error.")
                return@forEach
            }

            applyStatBatchResult(entries, batch, response.standardOutput)
        }
    }

    private fun buildStatBatchCommand(entries: List<DeviceFileEntry>, batchIndexes: List<Int>): String {
        val segments = mutableListOf<String>()
        for ((localIndex, entryIndex) in batchIndexes.withIndex()) {
            val entry = entries[entryIndex]
            val fullPath = if (entry.path == "/") "/${entry.name}" else "${entry.path.trimEnd('/')}/${entry.name}"
            val escaped = escapeShell(fullPath)
            segments += "echo '__STAT_BEGIN_${localIndex}__'; stat '$escaped' 2>/dev/null || true; echo '__STAT_END_${localIndex}__'"
        }

        return segments.joinToString("; ")
    }

    private fun applyStatBatchResult(entries: MutableList<DeviceFileEntry>, batchIndexes: List<Int>, output: String) {
        val sections = parseStatSections(output)
        for ((localIndex, entryIndex) in batchIndexes.withIndex()) {
            val section = sections[localIndex] ?: continue
            val parsed = parseStatTimes(section) ?: continue
            val original = entries[entryIndex]
            val createdAt = parsed.birthAt ?: parsed.accessAt ?: original.createdAt
            val modifiedAt = parsed.modifyAt ?: original.modifiedAt
            entries[entryIndex] = original.copy(createdAt = createdAt, modifiedAt = modifiedAt)
        }
    }

    private fun parseStatSections(output: String): Map<Int, String> {
        val lines = output.lineSequence().toList()
        val beginPrefix = "__STAT_BEGIN_"
        val endPrefix = "__STAT_END_"
        val sections = mutableMapOf<Int, String>()

        var currentIndex: Int? = null
        val currentLines = mutableListOf<String>()

        lines.forEach { line ->
            val begin = line.takeIf { it.startsWith(beginPrefix) && it.endsWith("__") }
            if (begin != null) {
                currentIndex = begin.removePrefix(beginPrefix).removeSuffix("__").toIntOrNull()
                currentLines.clear()
                return@forEach
            }

            val end = line.takeIf { it.startsWith(endPrefix) && it.endsWith("__") }
            if (end != null) {
                val endIndex = end.removePrefix(endPrefix).removeSuffix("__").toIntOrNull()
                if (currentIndex != null && endIndex == currentIndex)
                    sections[currentIndex] = currentLines.joinToString("\n")

                currentIndex = null
                currentLines.clear()
                return@forEach
            }

            if (currentIndex != null) currentLines += line
        }

        return sections
    }

    private fun parseStatTimes(section: String): StatTimes? {
        val accessAt = extractStatTime(section, "Access:")
        val modifyAt = extractStatTime(section, "Modify:")
        val birthAt = extractStatTime(section, "Birth:")

        if (accessAt == null && modifyAt == null && birthAt == null) return null
        return StatTimes(accessAt = accessAt, modifyAt = modifyAt, birthAt = birthAt)
    }

    private fun extractStatTime(section: String, label: String): Instant? {
        val line = section.lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.startsWith(label) &&
                    !it.startsWith("$label (")
            } ?: return null

        val dateText = line.removePrefix(label).trim()
        if (dateText == "-" || dateText.isBlank()) return null

        val match = statTimeRegex.matchEntire(dateText) ?: return null
        val datePart = match.groupValues[1]
        val timePart = match.groupValues[2]
        val fractionPart = match.groupValues[3]
        val offsetRaw = match.groupValues[4]

        val dateTime = runCatching {
            LocalDateTime.parse("$datePart $timePart", statDateTimePattern)
        }.getOrNull() ?: return null

        val nanos = fractionPart
            .takeIf { it.isNotBlank() }
            ?.padEnd(9, '0')
            ?.toIntOrNull()
            ?: 0

        val normalizedOffset = if (offsetRaw.contains(':'))
            offsetRaw
        else "${offsetRaw.take(3)}:${offsetRaw.substring(3)}"
        val offset = runCatching { ZoneOffset.of(normalizedOffset) }.getOrNull() ?: return null

        return OffsetDateTime.of(dateTime.withNano(nanos), offset).toInstant()
    }

    /**
     * Resolves symlink directory flags in batches to reduce ADB round-trips.
     */
    private suspend fun resolveSymlinkDirectoryFlags(device: AndroidDevice, entries: MutableList<DeviceFileEntry>) {
        val symlinkIndexes = entries.indices.filter { entries[it].isSymlink }
        if (symlinkIndexes.isEmpty()) return

        val chunkSize = 32
        symlinkIndexes.chunked(chunkSize).forEach { batch ->
            val command = buildSymlinkDirectoryCheckCommand(entries, batch)
            val response = runCatching { shellCommandExecutor.executeFileOperation(device, command) }.getOrNull()
            if (response == null || !response.isOk) {
                logService.log(LogLevel.Warning, CATEGORY, "Symlink directory check skipped for batch due to command error.")
                return@forEach
            }

            applySymlinkDirectoryCheckResult(entries, batch, response.standardOutput)
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

    private data class StatTimes(
        val accessAt: Instant?,
        val modifyAt: Instant?,
        val birthAt: Instant?
    )
}