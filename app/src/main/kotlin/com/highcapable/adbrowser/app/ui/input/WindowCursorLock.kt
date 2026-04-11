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
 * This file is created by fankes on 2026/4/11.
 */
@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.highcapable.adbrowser.app.ui.input

import java.awt.AWTEvent
import java.awt.Component
import java.awt.Cursor
import java.awt.EventQueue
import java.awt.KeyboardFocusManager
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import java.util.IdentityHashMap
import javax.swing.SwingUtilities

/**
 * Temporarily forces a resize cursor across the active AWT window while a drag interaction is running.
 *
 * Compose hover icons only affect the component currently under the pointer. During a resize drag
 * the pointer often leaves the original handle immediately, so this helper bridges down to AWT and
 * keeps the cursor consistent until the drag stops.
 */
class WindowCursorLock(private val pointerCursor: Cursor) {

    private var lockedWindow: Window? = null
    private var lockedComponentCursors: IdentityHashMap<Component, CursorRestoreState>? = null
    private var cursorLockListener: AWTEventListener? = null

    /** Starts forcing the configured cursor for the current active window. */
    fun lock() {
        val window = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow ?: return
        if (lockedWindow === window) return

        val cursorMap = IdentityHashMap<Component, CursorRestoreState>()

        fun applyCursor(component: Component?) {
            val target = component ?: window
            if (!cursorMap.containsKey(target)) {
                // Store whether the cursor was explicitly set before we touch it. On unlock we must
                // restore either the previous cursor or the inherited/null state, not just a cursor value.
                cursorMap[target] = CursorRestoreState(
                    wasCursorSet = target.isCursorSet,
                    cursor = if (target.isCursorSet) target.cursor else null
                )
            }
            target.cursor = pointerCursor
        }

        fun updateCursorUnderPointer() {
            val pointerLocation = MouseInfo.getPointerInfo()?.location ?: return
            val pointInWindow = pointerLocation.location

            // Convert screen coordinates before hit testing. getDeepestComponentAt expects the point
            // in the target container's local coordinate space, not screen space.
            SwingUtilities.convertPointFromScreen(pointInWindow, window)
            val target = SwingUtilities.getDeepestComponentAt(window, pointInWindow.x, pointInWindow.y)
            applyCursor(target)
        }

        val listener = AWTEventListener { event ->
            val mouseEvent = event as? MouseEvent ?: return@AWTEventListener
            val sourceComponent = mouseEvent.component ?: return@AWTEventListener
            if (SwingUtilities.getWindowAncestor(sourceComponent) !== window) return@AWTEventListener

            when (mouseEvent.id) {
                // Re-evaluate the deepest hovered component whenever the pointer moves so nested
                // child components cannot revert the resize cursor mid-drag.
                MouseEvent.MOUSE_DRAGGED,
                MouseEvent.MOUSE_MOVED,
                MouseEvent.MOUSE_ENTERED,
                MouseEvent.MOUSE_EXITED -> updateCursorUnderPointer()
            }
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(
            listener,
            AWTEvent.MOUSE_MOTION_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK
        )

        applyCursor(window)
        updateCursorUnderPointer()
        lockedWindow = window
        lockedComponentCursors = cursorMap
        cursorLockListener = listener
    }

    /** Stops forcing the cursor and restores every component that was modified during the lock. */
    fun unlock() {
        cursorLockListener?.let { Toolkit.getDefaultToolkit().removeAWTEventListener(it) }
        cursorLockListener = null

        val window = lockedWindow
        val cursorMap = lockedComponentCursors ?: return
        cursorMap.forEach { (component, state) ->
            component.cursor = if (state.wasCursorSet) state.cursor else null
        }

        // AWT does not always refresh the visible cursor immediately after restoring component
        // cursors, especially when the mouse has not moved yet. Posting a synthetic move nudges
        // the window to recompute the cursor under the current pointer position.
        refreshWindowCursor(window)
        EventQueue.invokeLater {
            // Run once more on the next event-turn because some platforms process the restoration
            // and the synthetic move in separate phases.
            refreshWindowCursor(window)
        }

        lockedWindow = null
        lockedComponentCursors = null
    }

    private fun refreshWindowCursor(window: Window?) {
        val targetWindow = window ?: KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow ?: return
        val screenPoint = MouseInfo.getPointerInfo()?.location ?: return
        val pointInWindow = Point(screenPoint)

        SwingUtilities.convertPointFromScreen(pointInWindow, targetWindow)

        val target = SwingUtilities.getDeepestComponentAt(targetWindow, pointInWindow.x, pointInWindow.y) ?: targetWindow
        val targetPoint = SwingUtilities.convertPoint(targetWindow, pointInWindow, target)

        // Post a synthetic move instead of mutating cursor state directly again. This lets the
        // target component/window re-apply its own normal cursor logic after the lock is released.
        Toolkit.getDefaultToolkit().systemEventQueue.postEvent(
            MouseEvent(
                target,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                targetPoint.x,
                targetPoint.y,
                screenPoint.x,
                screenPoint.y,
                0,
                false,
                0
            )
        )
    }

    private data class CursorRestoreState(
        val wasCursorSet: Boolean,
        val cursor: Cursor?
    )
}