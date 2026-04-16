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
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.cl.AppState
import com.highcapable.adbrowser.app.ui.utils.SystemFileChooser
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.app.ui.vm.model.LogEntryItem
import com.highcapable.adbrowser.core.common.fs.Environment
import com.highcapable.adbrowser.core.logging.LogEntry
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.generated.AdbrowserProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Window
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LogViewerStageModel(private val appState: AppState) : ViewModel() {

    private companion object {

        val LogTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val FileTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

        const val EXPORT_LOG_FILE_NAME = "${AdbrowserProperties.PROJECT_NAME}_{date}.log"

        const val LOG_COLUMN_MIN_WIDTH_TIME = 150f
        const val LOG_COLUMN_MIN_WIDTH_LEVEL = 90f
        const val LOG_COLUMN_MIN_WIDTH_CATEGORY = 120f
        const val LOG_COLUMN_MIN_WIDTH_MESSAGE = 220f
        const val LOG_COLUMN_DEFAULT_WIDTH_TIME = 180f
        const val LOG_COLUMN_DEFAULT_WIDTH_LEVEL = 100f
        const val LOG_COLUMN_DEFAULT_WIDTH_CATEGORY = 140f
        const val LOG_COLUMN_DEFAULT_WIDTH_MESSAGE = 80f
    }

    private val logService get() = appState.appServices.logService

    val entries = mutableStateListOf<LogEntryItem>()
    val selectedEntryIds = mutableStateListOf<String>()
    val hasEntries get() = entryCount > 0
    val hasVisibleEntries get() = entries.isNotEmpty()
    val hasFilteredOutEntries get() = hasEntries && !hasVisibleEntries

    var entryCount by mutableStateOf(logService.entryCount)
        private set
    var visibleLevels by mutableStateOf(LogLevel.entries.toSet())
        private set

    // `selectedEntryId` is the primary item for keyboard navigation and context menus, while the
    // list can still contain multiple selected entries.
    var selectedEntryId by mutableStateOf<String?>(null)
        private set

    // The anchor is preserved for Shift-range selection so extending the range remains stable even
    // after intermediate Ctrl-clicks or drag-selection updates.
    var selectionAnchorEntryId by mutableStateOf<String?>(null)
        private set

    // Log viewer column widths are intentionally session-only. They reset to defaults every time
    // the window is reopened instead of being persisted into global app settings.
    var logColumnWidthTimePx by mutableStateOf(LOG_COLUMN_DEFAULT_WIDTH_TIME)
        private set
    var logColumnWidthLevelPx by mutableStateOf(LOG_COLUMN_DEFAULT_WIDTH_LEVEL)
        private set
    var logColumnWidthCategoryPx by mutableStateOf(LOG_COLUMN_DEFAULT_WIDTH_CATEGORY)
        private set
    var logColumnWidthMessagePx by mutableStateOf(LOG_COLUMN_DEFAULT_WIDTH_MESSAGE)
        private set

    init {
        normalizeLogColumnWidths()
        modelScope.launch {
            try {
                logService.observeVisibleEntries().collect { snapshot ->
                    entries.clear()
                    entries += snapshot.map(::toLogEntryItem)
                    reconcileSelection()
                }
            } catch (_: CancellationException) {
                // Ignore cancellation when the window is closing.
            }
        }
        modelScope.launch {
            try {
                // Hidden logs must still update menu enablement, so total count is tracked from
                // the service separately instead of inferred from the visible snapshot.
                logService.observeEntryCount().collect { entryCount = it }
            } catch (_: CancellationException) {
                // Ignore cancellation when the window is closing.
            }
        }
        modelScope.launch {
            try {
                // The menu checkboxes are rendered by Compose, so the backend-owned filter state
                // still needs to be surfaced as observable UI state instead of a plain getter.
                logService.observeVisibleLevels().collect { visibleLevels = it }
            } catch (_: CancellationException) {
                // Ignore cancellation when the window is closing.
            }
        }
    }

    /** Resizes the Time column and returns the applied delta after width clamping. */
    fun resizeTimeAndLevelColumns(deltaDp: Float): Float {
        val old = logColumnWidthTimePx
        logColumnWidthTimePx = (old + deltaDp).coerceAtLeast(LOG_COLUMN_MIN_WIDTH_TIME)

        return logColumnWidthTimePx - old
    }

    /** Resizes the Level column and returns the applied delta after width clamping. */
    fun resizeLevelAndCategoryColumns(deltaDp: Float): Float {
        val old = logColumnWidthLevelPx
        logColumnWidthLevelPx = (old + deltaDp).coerceAtLeast(LOG_COLUMN_MIN_WIDTH_LEVEL)

        return logColumnWidthLevelPx - old
    }

    /** Resizes the Category column and returns the applied delta after width clamping. */
    fun resizeCategoryAndMessageColumns(deltaDp: Float): Float {
        val old = logColumnWidthCategoryPx
        logColumnWidthCategoryPx = (old + deltaDp).coerceAtLeast(LOG_COLUMN_MIN_WIDTH_CATEGORY)

        return logColumnWidthCategoryPx - old
    }

    fun selectEntry(entry: LogEntryItem) {
        setSelection(
            selectedIds = linkedSetOf(entry.id),
            primaryId = entry.id,
            anchorId = entry.id
        )
    }

    fun isLevelVisible(level: LogLevel) = level in visibleLevels

    fun setLevelVisible(level: LogLevel, visible: Boolean) = logService.setLevelVisible(level, visible)

    fun selectEntryByGesture(
        entry: LogEntryItem,
        appendSelection: Boolean,
        rangeSelection: Boolean
    ) {
        // Match common desktop selection semantics:
        // Shift-click extends from the anchor, Ctrl/Command-click toggles, plain click replaces.
        when {
            rangeSelection -> selectRangeToEntry(entry, additive = appendSelection)
            appendSelection -> toggleEntrySelection(entry)
            else -> selectEntry(entry)
        }
    }

    fun clearSelection() {
        setSelection(emptySet())
    }

    fun ensureEntrySelectedForContextMenu(entry: LogEntryItem) {
        if (selectedEntryIds.size > 1 && entry.id in selectedEntryIds) {
            // Right-clicking an item inside an existing multi-selection should keep the selection
            // block intact and only move the primary item.
            selectedEntryId = entry.id
            if (selectionAnchorEntryId == null) selectionAnchorEntryId = entry.id
            return
        }

        setSelection(
            selectedIds = linkedSetOf(entry.id),
            primaryId = entry.id,
            anchorId = entry.id
        )
    }

    fun isEntrySelected(entry: LogEntryItem) = entry.id in selectedEntryIds

    fun canCopySelectedEntry() = selectedEntries.isNotEmpty()

    fun selectedEntryClipboardText() = selectedEntries
        .takeIf { it.isNotEmpty() }
        ?.joinToString(System.lineSeparator(), transform = ::formatEntryForClipboard)

    fun hasMultipleSelectedEntries() = selectedEntryIds.size > 1

    fun selectAllEntries() {
        val ids = entries.mapTo(linkedSetOf(), LogEntryItem::id)
        val primaryId = selectedEntryId?.takeIf { it in ids } ?: entries.firstOrNull()?.id
        val anchorId = entries.firstOrNull()?.id

        setSelection(
            selectedIds = ids,
            primaryId = primaryId,
            anchorId = anchorId
        )
    }

    fun updateDragSelection(
        candidateIds: Set<String>,
        additive: Boolean,
        initialSelectionIds: Set<String>
    ) {
        // Drag-selection is calculated from currently visible rows, so we always re-filter against
        // the live entry list before applying it to avoid leaking stale ids into selection state.
        val validCandidateIds = entries
            .map(LogEntryItem::id)
            .filterTo(linkedSetOf()) { it in candidateIds }
        val finalSelection = if (additive)
            (initialSelectionIds - validCandidateIds) + (validCandidateIds - initialSelectionIds)
        else validCandidateIds
        val primaryId = entries.firstOrNull { it.id in validCandidateIds }?.id
            ?: entries.firstOrNull { it.id in finalSelection }?.id

        setSelection(
            selectedIds = finalSelection,
            primaryId = primaryId,
            anchorId = selectionAnchorEntryId ?: primaryId
        )
    }

    /**
     * Moves the single selection through the current log list.
     *
     * When nothing is selected yet, Down starts from the first row and Up starts from the last
     * row so keyboard navigation still feels natural without an initial mouse click.
     */
    fun navigateSelection(step: Int): Int? {
        if (entries.isEmpty()) return null

        val selectedIndices = entries.mapIndexedNotNull { index, entry ->
            index.takeIf { entry.id in selectedEntryIds }
        }
        val targetIndex = when {
            selectedIndices.isEmpty() && step < 0 -> entries.lastIndex
            selectedIndices.isEmpty() -> 0
            selectedIndices.size > 1 && step < 0 -> selectedIndices.min()
            selectedIndices.size > 1 -> selectedIndices.max()
            else -> (selectedIndices.first() + step).coerceIn(0, entries.lastIndex)
        }

        val targetEntry = entries.getOrNull(targetIndex) ?: return null
        setSelection(
            selectedIds = linkedSetOf(targetEntry.id),
            primaryId = targetEntry.id,
            anchorId = targetEntry.id
        )
        return targetIndex
    }

    fun clearEntries() {
        clearSelection()
        logService.clear()
    }

    fun resetColumnWidths() {
        logColumnWidthTimePx = LOG_COLUMN_DEFAULT_WIDTH_TIME
        logColumnWidthLevelPx = LOG_COLUMN_DEFAULT_WIDTH_LEVEL
        logColumnWidthCategoryPx = LOG_COLUMN_DEFAULT_WIDTH_CATEGORY
        logColumnWidthMessagePx = LOG_COLUMN_DEFAULT_WIDTH_MESSAGE
        normalizeLogColumnWidths()
    }

    fun export(parentWindow: Window?, dialogTitle: String) {
        val snapshot = entries.toList()
        if (snapshot.isEmpty()) return

        val exportFileName = EXPORT_LOG_FILE_NAME.replace("{date}", FileTimeFormatter.format(LocalDateTime.now()))
        val outputPath = SystemFileChooser.chooseSaveFile(
            parent = parentWindow,
            title = dialogTitle,
            initialPath = File(Environment.userHomeDir, exportFileName).absolutePath
        ) ?: return

        modelScope.launch {
            try {
                val output = snapshot.joinToString(System.lineSeparator(), transform = ::formatEntryForClipboard)
                withContext(Dispatchers.IO) {
                    File(outputPath).writeText(output)
                }
            } catch (_: CancellationException) {
                // Ignore cancellation when the window is closing.
            } catch (throwable: Throwable) {
                logService.log(
                    level = LogLevel.Error,
                    category = "LogViewer",
                    message = "Failed to export logs: ${throwable.message ?: throwable::class.simpleName.orEmpty()}"
                )
            }
        }
    }

    fun dispose() {
        modelScope.cancel()
    }

    /** Reapplies minimum widths so persisted settings cannot collapse a column to zero. */
    private fun normalizeLogColumnWidths() {
        logColumnWidthTimePx = logColumnWidthTimePx.coerceAtLeast(LOG_COLUMN_MIN_WIDTH_TIME)
        logColumnWidthLevelPx = logColumnWidthLevelPx.coerceAtLeast(LOG_COLUMN_MIN_WIDTH_LEVEL)
        logColumnWidthCategoryPx = logColumnWidthCategoryPx.coerceAtLeast(LOG_COLUMN_MIN_WIDTH_CATEGORY)
        logColumnWidthMessagePx = logColumnWidthMessagePx.coerceAtLeast(LOG_COLUMN_MIN_WIDTH_MESSAGE)
    }

    private val selectedEntries: List<LogEntryItem>
        get() = if (selectedEntryIds.isEmpty()) emptyList()
        else entries.filter { it.id in selectedEntryIds }

    /**
     * Repairs selection after the backing entry list changes.
     *
     * This is needed because the log stream can replace the visible snapshot at any time; keeping
     * orphaned ids would break keyboard navigation and clipboard copy behavior.
     */
    private fun reconcileSelection() {
        val validIds = entries
            .map(LogEntryItem::id)
            .filterTo(linkedSetOf()) { it in selectedEntryIds }

        selectedEntryIds.clear()
        selectedEntryIds += validIds
        selectedEntryId = selectedEntryId
            ?.takeIf { it in validIds }
            ?: validIds.firstOrNull()
        selectionAnchorEntryId = selectionAnchorEntryId
            ?.takeIf { it in validIds }
            ?: selectedEntryId
    }

    private fun setSelection(
        selectedIds: Set<String>,
        primaryId: String? = null,
        anchorId: String? = null
    ) {
        // All public selection APIs funnel through here so primary / anchor invariants stay
        // consistent no matter whether the source gesture was click, drag, keyboard, or menu.
        val validIds = entries
            .map(LogEntryItem::id)
            .filterTo(linkedSetOf()) { it in selectedIds }

        selectedEntryIds.clear()
        selectedEntryIds += validIds
        selectedEntryId = primaryId
            ?.takeIf { it in validIds }
            ?: validIds.firstOrNull()
        selectionAnchorEntryId = anchorId
            ?.takeIf { it in validIds }
            ?: selectedEntryId
    }

    private fun toggleEntrySelection(entry: LogEntryItem) {
        // Toggling can remove the current primary item, so we recompute both primary and anchor
        // from the remaining selection instead of assuming the clicked row always survives.
        val current = selectedEntryIds.toMutableSet()
        if (!current.add(entry.id)) current.remove(entry.id)

        val primaryId = when {
            entry.id in current -> entry.id
            selectedEntryId in current -> selectedEntryId
            else -> entries.firstOrNull { it.id in current }?.id
        }
        val anchorId = when {
            entry.id in current -> entry.id
            selectionAnchorEntryId in current -> selectionAnchorEntryId
            else -> primaryId
        }

        setSelection(
            selectedIds = current,
            primaryId = primaryId,
            anchorId = anchorId
        )
    }

    private fun selectRangeToEntry(entry: LogEntryItem, additive: Boolean) {
        val targetIndex = entries.indexOfFirst { it.id == entry.id }
        if (targetIndex < 0) return

        // Keep the original anchor when possible so repeated Shift-clicks continue extending from
        // the first meaningful selection point instead of jumping to the last clicked row.
        val anchorId = selectionAnchorEntryId
            ?.takeIf { anchor -> entries.any { it.id == anchor } }
            ?: selectedEntryId
            ?: entry.id
        val anchorIndex = entries.indexOfFirst { it.id == anchorId }.coerceAtLeast(0)

        val range = entries
            .subList(minOf(anchorIndex, targetIndex), maxOf(anchorIndex, targetIndex) + 1)
            .mapTo(linkedSetOf(), LogEntryItem::id)
        val selectedIds = if (additive) selectedEntryIds.toSet() + range else range

        setSelection(
            selectedIds = selectedIds,
            primaryId = entry.id,
            anchorId = anchorId
        )
    }

    private fun formatEntryForClipboard(entry: LogEntryItem) =
        "[${entry.timeText}] [${entry.levelText}] [${entry.category}] ${entry.message}"

    /** Converts the backend log entry into a stable UI item with a deterministic selection id. */
    private fun toLogEntryItem(entry: LogEntry) = LogEntryItem(
        id = "${entry.time.epochSecond}:${entry.time.nano}:${entry.level.name}:${entry.category}:${entry.message.hashCode()}",
        level = entry.level,
        timeText = LogTimeFormatter.format(entry.time.atZone(ZoneId.systemDefault())),
        levelText = entry.level.name,
        category = entry.category,
        message = entry.message
    )
}