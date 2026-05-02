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
 * This file is created by fankes on 2026/4/15.
 */
package com.highcapable.adbrowser.app.ui.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.lazyClassOrNull
import java.awt.Frame
import java.awt.GraphicsConfiguration
import java.awt.KeyboardFocusManager
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import java.awt.event.WindowStateListener
import java.beans.PropertyChangeListener
import java.lang.reflect.Proxy
import javax.swing.RootPaneContainer
import kotlin.math.abs

/**
 * Creates a [WindowFocusListener] backed by lambdas, mirroring the small adapter helpers used for
 * component events in this package.
 */
fun WindowFocusListener(
    windowGainedFocus: (WindowEvent?) -> Unit = {},
    windowLostFocus: (WindowEvent?) -> Unit = {}
) = object : WindowFocusListener {

    override fun windowGainedFocus(event: WindowEvent?) {
        windowGainedFocus(event)
    }

    override fun windowLostFocus(event: WindowEvent?) {
        windowLostFocus(event)
    }
}

/**
 * Creates a [WindowListener] backed by lambdas, so call sites can keep the same lightweight style
 * as [WindowFocusListener] without inlining anonymous adapter classes.
 */
fun WindowListener(
    windowOpened: (WindowEvent?) -> Unit = {},
    windowClosing: (WindowEvent?) -> Unit = {},
    windowClosed: (WindowEvent?) -> Unit = {},
    windowIconified: (WindowEvent?) -> Unit = {},
    windowDeiconified: (WindowEvent?) -> Unit = {},
    windowActivated: (WindowEvent?) -> Unit = {},
    windowDeactivated: (WindowEvent?) -> Unit = {}
) = object : WindowListener {

    override fun windowOpened(event: WindowEvent?) {
        windowOpened(event)
    }

    override fun windowClosing(event: WindowEvent?) {
        windowClosing(event)
    }

    override fun windowClosed(event: WindowEvent?) {
        windowClosed(event)
    }

    override fun windowIconified(event: WindowEvent?) {
        windowIconified(event)
    }

    override fun windowDeiconified(event: WindowEvent?) {
        windowDeiconified(event)
    }

    override fun windowActivated(event: WindowEvent?) {
        windowActivated(event)
    }

    override fun windowDeactivated(event: WindowEvent?) {
        windowDeactivated(event)
    }
}

/**
 * Returns whether any window belonging to this app is currently active.
 *
 * Compose Desktop does not provide an "always on top within this app only" flag, so windows that
 * want that behavior can bridge to AWT and treat "some app window is active" as the signal for
 * enabling `alwaysOnTop`.
 */
@Composable
fun rememberAppHasActiveWindow(): Boolean {
    var appHasActiveWindow by remember {
        mutableStateOf(KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow != null)
    }

    DisposableEffect(Unit) {
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val listener = PropertyChangeListener {
            appHasActiveWindow = focusManager.activeWindow != null
        }
        focusManager.addPropertyChangeListener("activeWindow", listener)

        onDispose {
            focusManager.removePropertyChangeListener("activeWindow", listener)
        }
    }

    return appHasActiveWindow
}

/**
 * Observes whether the given AWT window is effectively zoomed/maximized on the current platform.
 *
 * macOS does not reliably update `Frame.MAXIMIZED_BOTH` for all zoom/fullscreen transitions, so
 * the fallback geometry check compares the live window bounds against both the full screen bounds
 * and the usable bounds with system insets removed.
 */
@Composable
fun rememberWindowZoomed(window: Window): Boolean {
    var isWindowZoomed by remember(window) {
        mutableStateOf(window.isWindowZoomedState())
    }

    DisposableEffect(window) {
        val update = {
            isWindowZoomed = window.isWindowZoomedState()
        }
        val componentListener = ComponentAdapter(
            componentMoved = { update() },
            componentResized = { update() },
            componentShown = { update() }
        )
        val windowStateListener = WindowStateListener {
            update()
        }

        window.addComponentListener(componentListener)
        window.addWindowStateListener(windowStateListener)
        update()

        onDispose {
            window.removeComponentListener(componentListener)
            window.removeWindowStateListener(windowStateListener)
        }
    }

    return isWindowZoomed
}

/**
 * Observes whether the native window decorations are currently hidden by the platform.
 *
 * On macOS this maps to the native fullscreen presentation state rather than the ordinary zoomed
 * window state, because the green-button zoom keeps the title bar visible.
 */
@Composable
fun rememberWindowDecorationsVisible(window: Window): Boolean {
    var isDecorationsVisible by remember(window) {
        mutableStateOf(window.isWindowDecorationsVisibleState())
    }

    DisposableEffect(window) {
        val update = {
            isDecorationsVisible = window.isWindowDecorationsVisibleState()
        }
        val componentListener = ComponentAdapter(
            componentShown = { update() }
        )
        val propertyListener = PropertyChangeListener { event ->
            if (event.propertyName == FlatLafButtonsBoundsKey ||
                event.propertyName == AppleButtonsBoundsKey
            ) update()
        }
        val fullScreenListenerHandle = window.installMacOSFullscreenListener { isFullscreen ->
            isDecorationsVisible = !isFullscreen
        }

        window.addComponentListener(componentListener)
        rootPaneOf(window)?.addPropertyChangeListener(propertyListener)
        update()

        onDispose {
            window.removeComponentListener(componentListener)
            rootPaneOf(window)?.removePropertyChangeListener(propertyListener)
            fullScreenListenerHandle?.close()
        }
    }

    return isDecorationsVisible
}

private fun Window.isWindowZoomedState(): Boolean {
    val frame = this as? Frame ?: return false
    if ((frame.extendedState and Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) return true

    val bounds = frame.bounds
    val screenBounds = frame.graphicsConfiguration?.bounds ?: return false
    if (bounds.isAlmostEqualTo(screenBounds)) return true

    val usableBounds = frame.graphicsConfiguration
        ?.let(::screenUsableBounds)
        ?: return false
    return bounds.isAlmostEqualTo(usableBounds)
}

private fun Window.isWindowDecorationsVisibleState(): Boolean {
    // For macOS only,
    // the presence of window buttons is a more reliable signal of whether we're
    // in native fullscreen than the usual maximized state,
    // which can be entered without hiding the title bar.
    // Swing provides the button bounds as a client property,
    // so we can listen for changes to that as well as the fullscreen listener to cover all the ways the state can change.
    if (!OsType.isMacOS) return true

    val buttonsBounds = windowButtonsBoundsOf(this)
    if (buttonsBounds != null) return buttonsBounds.width > 0

    return !isMacOSNativeFullscreenState()
}

private fun Window.isMacOSNativeFullscreenState() = graphicsConfiguration?.device?.fullScreenWindow === this

private fun Window.installMacOSFullscreenListener(onStateChanged: (Boolean) -> Unit): AutoCloseable? {
    if (!OsType.isMacOS) return null

    return runCatching {
        val listenerClass = FullScreenListenerClass ?: return null

        val listener = Proxy.newProxyInstance(
            listenerClass.classLoader,
            arrayOf(listenerClass)
        ) { _, method, _ ->
            when (method.name) {
                "windowEnteringFullScreen", "windowEnteredFullScreen" -> onStateChanged(true)
                "windowExitingFullScreen", "windowExitedFullScreen" -> onStateChanged(false)
            }
            null
        }

        addFullScreenListenerTo?.invokeQuietly(this, listener)

        AutoCloseable {
            removeFullScreenListenerFrom?.invokeQuietly(this, listener)
        }
    }.getOrNull()
}

private fun rootPaneOf(window: Window) = (window as? RootPaneContainer)?.rootPane

private fun windowButtonsBoundsOf(window: Window): Rectangle? {
    val rootPane = rootPaneOf(window) ?: return null
    return (rootPane.getClientProperty(FlatLafButtonsBoundsKey) as? Rectangle)
        ?: (rootPane.getClientProperty(AppleButtonsBoundsKey) as? Rectangle)
}

private fun screenUsableBounds(configuration: GraphicsConfiguration): Rectangle {
    val screenBounds = configuration.bounds
    val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)

    return Rectangle(
        screenBounds.x + screenInsets.left,
        screenBounds.y + screenInsets.top,
        screenBounds.width - screenInsets.left - screenInsets.right,
        screenBounds.height - screenInsets.top - screenInsets.bottom
    )
}

private fun Rectangle.isAlmostEqualTo(other: Rectangle, tolerancePx: Int = 2) =
    abs(x - other.x) <= tolerancePx &&
        abs(y - other.y) <= tolerancePx &&
        abs(width - other.width) <= tolerancePx &&
        abs(height - other.height) <= tolerancePx

private val FullScreenListenerClass by lazyClassOrNull("com.apple.eawt.FullScreenListener")
private val FullScreenUtilitiesClass by lazyClassOrNull("com.apple.eawt.FullScreenUtilities")

private val addFullScreenListenerTo by lazy {
    FullScreenListenerClass?.let { listenerClass ->
        FullScreenUtilitiesClass?.resolve()
            ?.optional()
            ?.firstMethodOrNull {
                name == "addFullScreenListenerTo"
                parameters(Window::class, listenerClass)
            }
    }
}

private val removeFullScreenListenerFrom by lazy {
    FullScreenListenerClass?.let { listenerClass ->
        FullScreenUtilitiesClass?.resolve()
            ?.optional()
            ?.firstMethodOrNull {
                name == "removeFullScreenListenerFrom"
                parameters(Window::class, listenerClass)
            }
    }
}

private const val FlatLafButtonsBoundsKey = "FlatLaf.fullWindowContent.buttonsBounds"
private const val AppleButtonsBoundsKey = "apple.awt.fullWindowContent.buttonsBounds"