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
@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.app.ui.stage

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.component.LogListHint
import com.highcapable.adbrowser.app.ui.component.LogListPane
import com.highcapable.adbrowser.app.ui.component.PanelSurface
import com.highcapable.adbrowser.app.ui.component.WindowTitleBar
import com.highcapable.adbrowser.app.ui.foundation.revealIndexBySingleStep
import com.highcapable.adbrowser.app.ui.interaction.SelectionAreaState
import com.highcapable.adbrowser.app.ui.interaction.blankAreaDragSelection
import com.highcapable.adbrowser.app.ui.interaction.onBlankPrimaryPress
import com.highcapable.adbrowser.app.ui.utils.SystemClipboard
import com.highcapable.adbrowser.app.ui.vm.LogViewerStageModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.ContextMenuItemOptionAction.CopyMenuItemOptionAction
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun FrameWindowScope.LogViewerStage(
    viewModel: LogViewerStageModel,
    hasWindowFocus: Boolean,
    onCloseRequest: () -> Unit,
    decorationsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val interactionState = remember { LogAreaInteractionState() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val needApplyTopPadding = !(WindowTitleBar.isAvailable && decorationsVisible)

    // Keep focus on the log area itself so keyboard navigation and shortcuts still work after
    // clicking rows, blank space, or opening and dismissing a non-focusable context menu.
    Column(
        modifier = modifier
            .fillMaxSize()
            // Half the top padding to harmonize spacing with the title bar.
            .padding(top = if (needApplyTopPadding) ContentPaddingSpace / 2 else 0.dp)
            .padding(horizontal = ContentPaddingSpace)
            .padding(bottom = ContentPaddingSpace)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                handleLogAreaShortcut(
                    viewModel = viewModel,
                    event = it,
                    onNavigateSelection = { step ->
                        val targetIndex = viewModel.navigateSelection(step) ?: return@handleLogAreaShortcut false
                        coroutineScope.launch { listState.revealIndexBySingleStep(targetIndex) }
                        true
                    }
                )
            }
            .onBlankPrimaryPress {
                if (interactionState.consumePendingBlankPrimaryPress()) return@onBlankPrimaryPress
                if (interactionState.hasContextMenu()) {
                    interactionState.dismissContextMenu()
                    return@onBlankPrimaryPress
                }

                viewModel.clearSelection()
            }
            .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) {
                focusRequester.requestFocus()
            }
    ) {
        PanelSurface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            clipContent = true
        ) {
            LogContentPane(
                viewModel = viewModel,
                hasWindowFocus = hasWindowFocus,
                listState = listState,
                horizontalScrollState = horizontalScrollState,
                interactionState = interactionState
            )
        }
    }
}

@Composable
private fun LogContentPane(
    viewModel: LogViewerStageModel,
    hasWindowFocus: Boolean,
    listState: LazyListState,
    horizontalScrollState: ScrollState,
    interactionState: LogAreaInteractionState
) {
    var shouldStickToTop by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        // Auto-follow should only happen when the viewport is already at the top. The log viewer
        // streams newest entries first, so "top" is the equivalent of a tailing position.
        snapshotFlow { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
            .distinctUntilChanged()
            .collect { shouldStickToTop = it }
    }

    LaunchedEffect(viewModel.entries.firstOrNull()?.id, viewModel.entries.size, hasWindowFocus) {
        // When the window is unfocused we always snap back to the newest logs, so reopening the
        // viewer or switching back to it does not leave stale content parked far down the list.
        if (!hasWindowFocus || shouldStickToTop) listState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LogListPane(
            entries = viewModel.entries,
            selectedEntryIds = viewModel.selectedEntryIds.toSet(),
            listState = listState,
            horizontalScrollState = horizontalScrollState,
            timeWidth = viewModel.logColumnWidthTimePx.dp,
            levelWidth = viewModel.logColumnWidthLevelPx.dp,
            categoryWidth = viewModel.logColumnWidthCategoryPx.dp,
            messageWidth = viewModel.logColumnWidthMessagePx.dp,
            onResizeTimeAndLevel = viewModel::resizeTimeAndLevelColumns,
            onResizeLevelAndCategory = viewModel::resizeLevelAndCategoryColumns,
            onResizeCategoryAndMessage = viewModel::resizeCategoryAndMessageColumns,
            onResizeFinished = {},
            onEntryPrimaryClick = { entry, appendSelection, rangeSelection ->
                viewModel.selectEntryByGesture(entry, appendSelection, rangeSelection)
            },
            onEntrySecondaryClick = { entry, position ->
                viewModel.ensureEntrySelectedForContextMenu(entry)
                interactionState.openEntryContextMenu(entry.id, position)
            },
            onEntryBoundsChanged = { entry, bounds ->
                // Bound tracking feeds blank-area hit testing and drag selection. Removing disposed
                // rows eagerly avoids stale hit regions when the lazy list is reusing slots.
                if (bounds == null) interactionState.selectionAreaState.removeVisibleItemBounds(entry.id)
                else interactionState.selectionAreaState.updateVisibleItemBounds(entry.id, bounds)
            },
            modifier = Modifier.fillMaxSize(),
            contentAreaModifier = Modifier
                .nestedScroll(interactionState.selectionAreaState.nestedScrollConnection)
                .onGloballyPositioned { interactionState.selectionAreaState.contentCoordinates = it }
                .blankAreaDragSelection(
                    selectionState = interactionState.selectionAreaState,
                    showSelectionRect = false,
                    selectedKeysProvider = { viewModel.selectedEntryIds.toSet() },
                    onClearSelection = viewModel::clearSelection,
                    onSelectionChanged = { candidateIds, additive, initialSelectionIds ->
                        viewModel.updateDragSelection(candidateIds, additive, initialSelectionIds)
                    },
                    keyOfItem = { it },
                    prepareBlankGesture = {
                        // Blank clicks have three different meanings here:
                        // 1. consume the deferred press after dismissing a context menu
                        // 2. close the currently visible context menu
                        // 3. start a real blank-area selection gesture
                        when {
                            interactionState.consumePendingBlankPrimaryPress() -> false
                            interactionState.hasContextMenu() -> {
                                interactionState.dismissContextMenu()
                                false
                            }
                            else -> {
                                interactionState.dismissContextMenu()
                                true
                            }
                        }
                    },
                    autoScrollBy = { delta ->
                        val consumed = listState.scrollBy(delta)
                        interactionState.selectionAreaState.cumulativeScrollY += consumed
                    }
                ),
            overlay = { entry ->
                val entryState = interactionState.contextMenuState as? LogContextMenuState.Entry
                LogEntryContextMenuPopup(
                    visible = entryState?.itemId == entry.id,
                    position = entryState?.position ?: Offset.Zero,
                    canCopy = viewModel.canCopySelectedEntry(),
                    onCopy = {
                        viewModel.selectedEntryClipboardText()?.let(SystemClipboard::copyText)
                    },
                    onDismissRequest = interactionState::dismissContextMenu,
                    onDismissByOutsidePress = interactionState::dismissContextMenuConsumingNextBlankPress
                )
            }
        )

        if (viewModel.entries.isEmpty())
            LogListHint(
                message = if (viewModel.hasFilteredOutEntries)
                    strings.logsHintEmptyFiltered
                else strings.logsHintEmpty,
                modifier = Modifier.align(Alignment.Center)
            )
    }
}

@Composable
private fun LogEntryContextMenuPopup(
    visible: Boolean,
    position: Offset,
    canCopy: Boolean,
    onCopy: () -> Unit,
    onDismissRequest: () -> Unit,
    onDismissByOutsidePress: () -> Unit
) {
    if (!visible) return

    PopupMenu(
        onDismissRequest = { onDismissByOutsidePress(); true },
        popupPositionProvider = rememberPopupPositionProviderAtPosition(position),
        popupProperties = PopupProperties(focusable = false)
    ) {
        logEntryContextMenu(
            canCopy = canCopy,
            onCopy = onCopy,
            onDismissRequest = onDismissRequest
        )
    }
}

private fun MenuScope.logEntryContextMenu(
    canCopy: Boolean,
    onCopy: () -> Unit,
    onDismissRequest: () -> Unit
) = selectableItemWithActionType(
    selected = false,
    enabled = canCopy,
    iconKey = AllIconsKeys.Actions.Copy,
    actionType = CopyMenuItemOptionAction,
    onClick = {
        onDismissRequest()
        onCopy()
    }
) { Text(strings.menuCopy) }

private sealed interface LogContextMenuState {

    /** Keeps the menu pinned to one logical row even if selection changes elsewhere. */
    data class Entry(
        val itemId: String,
        val position: Offset,
        val requestId: Long
    ) : LogContextMenuState
}

/**
 * Transient UI-only interaction state for the log area.
 *
 * The view model owns selection, but the stage still needs a small state holder for pointer-only
 * concerns such as context-menu dismissal rules and drag-selection hit testing data.
 */
private class LogAreaInteractionState {

    private var contextMenuRequestId by mutableStateOf(0L)
    private var consumeNextBlankPrimaryPress by mutableStateOf(false)

    val selectionAreaState = SelectionAreaState<String>()
    var contextMenuState by mutableStateOf<LogContextMenuState?>(null)

    fun openEntryContextMenu(entryId: String, position: Offset) {
        consumeNextBlankPrimaryPress = false
        contextMenuRequestId += 1L
        contextMenuState = LogContextMenuState.Entry(entryId, position, contextMenuRequestId)
    }

    fun dismissContextMenu() {
        contextMenuState = null
        consumeNextBlankPrimaryPress = false
    }

    fun dismissContextMenuConsumingNextBlankPress() {
        if (contextMenuState == null) {
            consumeNextBlankPrimaryPress = false
            return
        }

        // Closing a context menu by clicking outside should not also clear selection in the same
        // gesture. The next blank primary press is therefore consumed once.
        contextMenuState = null
        consumeNextBlankPrimaryPress = true
    }

    fun consumePendingBlankPrimaryPress(): Boolean {
        if (!consumeNextBlankPrimaryPress) return false

        consumeNextBlankPrimaryPress = false
        return true
    }

    fun hasContextMenu() = contextMenuState != null
}

private fun handleLogAreaShortcut(
    viewModel: LogViewerStageModel,
    event: KeyEvent,
    onNavigateSelection: (Int) -> Boolean
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    // Keep shortcut routing local to the log surface. This avoids leaking log-specific shortcuts
    // into the rest of the window when another control eventually gains focus.
    return when {
        (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.A -> {
            viewModel.selectAllEntries()
            true
        }
        (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.C && viewModel.canCopySelectedEntry() -> {
            viewModel.selectedEntryClipboardText()?.let(SystemClipboard::copyText)
            true
        }
        event.key == Key.DirectionUp -> onNavigateSelection(-1)
        event.key == Key.DirectionDown -> onNavigateSelection(1)
        event.key == Key.Escape && viewModel.selectedEntryIds.isNotEmpty() -> {
            viewModel.clearSelection()
            true
        }
        else -> false
    }
}

private val ContentPaddingSpace = 14.dp