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

package com.highcapable.adbrowser.frontend.ui.input

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
 * Utility class to lock the cursor to a specific type (e.g., pointer) when it's within a window,
 * and restore the original cursor when it leaves.
 *
 * This is necessary because Swing doesn't provide a built-in way to change the cursor for the entire window and all its
 * components based on the cursor's position. By listening to mouse events and tracking the cursor's location,
 * we can dynamically update the cursor for the component under the pointer and restore it when
 * the pointer moves away. This class ensures that the cursor is consistently set to the desired
 * type while it's within the window, and that all original cursor states are properly restored when unlocking.
 */
class WindowCursorLock(private val pointerCursor: Cursor) {

    private var lockedWindow: Window? = null
    private var lockedComponentCursors: IdentityHashMap<Component, CursorRestoreState>? = null
    private var cursorLockListener: AWTEventListener? = null

    /**
     * Locks the cursor to the specified type for the currently active window and all its components.
     */
    fun lock() {
        val window = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow ?: return
        if (lockedWindow === window) return

        val cursorMap = IdentityHashMap<Component, CursorRestoreState>()

        fun applyCursor(component: Component?) {
            val target = component ?: window
            if (!cursorMap.containsKey(target)) {
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
            val target = SwingUtilities.getDeepestComponentAt(window, pointInWindow.x, pointInWindow.y)

            SwingUtilities.convertPointFromScreen(pointInWindow, window)
            applyCursor(target)
        }

        val listener = AWTEventListener { event ->
            val mouseEvent = event as? MouseEvent ?: return@AWTEventListener
            val sourceComponent = mouseEvent.component ?: return@AWTEventListener
            if (SwingUtilities.getWindowAncestor(sourceComponent) !== window) return@AWTEventListener

            when (mouseEvent.id) {
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

    /**
     * Unlocks the cursor, restoring the original cursor for all components that were modified during locking.
     */
    fun unlock() {
        cursorLockListener?.let { Toolkit.getDefaultToolkit().removeAWTEventListener(it) }
        cursorLockListener = null

        val window = lockedWindow
        val cursorMap = lockedComponentCursors ?: return
        cursorMap.forEach { (component, state) ->
            component.cursor = if (state.wasCursorSet) state.cursor else null
        }

        refreshWindowCursor(window)
        EventQueue.invokeLater {
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