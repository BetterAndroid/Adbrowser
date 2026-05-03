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
package com.highcapable.adbrowser.app.ui.platform.macos

import com.highcapable.adbrowser.app.locale.strings.Strings
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.logging.LogLevel
import com.highcapable.adbrowser.core.logging.LogService
import java.awt.AWTEvent
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import java.text.MessageFormat
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Localizes the built-in macOS application menu by patching Cocoa `NSMenuItem` titles.
 *
 * JDK/JBR still keeps those titles outside the public Java API, so the localization has to be
 * re-applied when macOS rebuilds or re-activates the application menu.
 */
object MacOSAppMenuLocalizer {

    private const val CATEGORY = "App"

    fun install(strings: Strings, logService: LogService, onOpenPreferences: () -> Unit, onExitRequest: () -> Unit): AutoCloseable? {
        if (!OsType.isMacOS || !Desktop.isDesktopSupported()) return null

        val desktop = runCatching { Desktop.getDesktop() }.getOrNull() ?: return null
        installDesktopHandlers(desktop, onOpenPreferences, onExitRequest)

        val apply = {
            runCatching { localize(strings) }.onFailure {
                logService.log(
                    LogLevel.Warning,
                    CATEGORY,
                    "Failed to localize macOS app menu: ${it.message.orEmpty()}"
                )
            }.let {}
        }

        val listener = AWTEventListener { event ->
            when (event.id) {
                WindowEvent.WINDOW_OPENED,
                WindowEvent.WINDOW_ACTIVATED,
                WindowEvent.WINDOW_GAINED_FOCUS -> scheduleApply(apply)
            }
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(
            listener,
            AWTEvent.WINDOW_EVENT_MASK or AWTEvent.WINDOW_FOCUS_EVENT_MASK
        )
        scheduleApply(apply)

        return AutoCloseable {
            Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
            clearDesktopHandlers(desktop)
        }
    }

    private fun installDesktopHandlers(desktop: Desktop, onOpenPreferences: () -> Unit, onExitRequest: () -> Unit) {
        runCatching {
            if (desktop.isSupported(Desktop.Action.APP_ABOUT))
                desktop.setAboutHandler(null)

            if (desktop.isSupported(Desktop.Action.APP_PREFERENCES))
                desktop.setPreferencesHandler { onOpenPreferences() }

            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER))
                desktop.setQuitHandler { _, response ->
                    onExitRequest()
                    response.cancelQuit()
                }
        }.let {}
    }

    private fun clearDesktopHandlers(desktop: Desktop) {
        runCatching {
            if (desktop.isSupported(Desktop.Action.APP_ABOUT))
                desktop.setAboutHandler(null)

            if (desktop.isSupported(Desktop.Action.APP_PREFERENCES))
                desktop.setPreferencesHandler(null)

            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER))
                desktop.setQuitHandler(null)
        }.let {}
    }

    private fun scheduleApply(action: () -> Unit) {
        SwingUtilities.invokeLater { action() }
        Timer(250) { action() }.apply {
            isRepeats = false
            start()
        }
    }

    private fun localize(strings: Strings) {
        val application = MacOSObjectiveC.sendPointer(
            receiver = MacOSObjectiveC.nsClass("NSApplication"),
            selectorName = "sharedApplication"
        ) ?: return
        val mainMenu = MacOSObjectiveC.sendPointer(application, "mainMenu") ?: return
        val appMenuItem = MacOSObjectiveC.sendPointer(mainMenu, "itemAtIndex:", 0L) ?: return
        val appMenu = MacOSObjectiveC.sendPointer(appMenuItem, "submenu") ?: return

        val itemCount = MacOSObjectiveC.sendLong(appMenu, "numberOfItems")
        repeat(itemCount.toInt()) { index ->
            val item = MacOSObjectiveC.sendPointer(appMenu, "itemAtIndex:", index.toLong()) ?: return@repeat
            val action = MacOSObjectiveC.selectorName(MacOSObjectiveC.sendPointer(item, "action"))
            val submenu = MacOSObjectiveC.sendPointer(item, "submenu")
            val keyEquivalent = MacOSObjectiveC.stringValue(MacOSObjectiveC.sendPointer(item, "keyEquivalent")).orEmpty()

            val title = when {
                index == 0 -> format(strings.menuMacOSAboutApp, strings.mainTitle)
                action == "orderFrontStandardAboutPanel:" -> format(strings.menuMacOSAboutApp, strings.mainTitle)
                keyEquivalent == "," -> strings.menuPreferences
                submenu != null -> strings.menuMacOSServices
                action == "hide:" -> format(strings.menuMacOSHideApp, strings.mainTitle)
                action == "hideOtherApplications:" -> strings.menuMacOSHideOthers
                action == "unhideAllApplications:" -> strings.menuMacOSShowAll
                action == "terminate:" -> format(strings.menuMacOSExitApp, strings.mainTitle)
                else -> null
            } ?: return@repeat

            MacOSObjectiveC.nsString(title)?.let {
                MacOSObjectiveC.sendVoid(item, "setTitle:", it)
            }
        }
    }

    private fun format(pattern: String, vararg args: Any) = MessageFormat.format(pattern, *args)
}