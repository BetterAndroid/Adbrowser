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
 * This file is created by fankes on 2026/4/30.
 */
package com.highcapable.adbrowser.app.ui.utils

import androidx.compose.ui.graphics.Color
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.highcapable.adbrowser.app.ui.theme.AdbrowserColorScheme
import com.highcapable.betterandroid.compose.extension.ui.toPlatformColor
import org.jetbrains.jewel.ui.component.styling.MenuStyle
import java.awt.Window
import javax.swing.JCheckBoxMenuItem
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JRadioButtonMenuItem
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

/**
 * Applies style to the Swing-backed desktop on those platforms.
 *
 * Compose renders the app content, but menus on Windows/Linux still go through Swing/AWT.
 * Because of that, theme changes need both a FlatLaf refresh and a manual refresh for already
 * created menu instances.
 */
object LookAndFeel {

    /**
     * Minimal data needed to project the Compose theme into Swing menus and window decorations.
     */
    data class UiStyle(
        val colors: AdbrowserColorScheme,
        val contentColor: Color,
        val menuStyle: MenuStyle
    )

    private const val POPUP_MENU_CORNER_RADIUS = 4

    /**
     * Initializes the look and feel for the application.
     * This should be called once at startup before any windows are created.
     */
    fun initialize(isDarkMode: Boolean) {
        apply(isDarkMode, updateExistingWindows = false, waitUntilApplied = true)
    }

    /** Synchronizes the look and feel across all existing windows, applying the specified dark mode and UI style. */
    fun sync(isDarkMode: Boolean, uiStyle: UiStyle) {
        apply(isDarkMode, updateExistingWindows = true, waitUntilApplied = false, uiStyle)
    }

    /**
     * Re-installs FlatLaf and refreshes existing menus when the system theme changes.
     *
     * `FlatLaf.updateUI()` alone is not enough here because popup menus may already exist and can
     * keep stale background/foreground values until the window is reopened.
     */
    private fun apply(isDarkMode: Boolean, updateExistingWindows: Boolean, waitUntilApplied: Boolean, uiStyle: UiStyle? = null) {
        val install = {
            runCatching {
                System.setProperty("flatlaf.updateUIOnLafChange", "true")
                System.setProperty("flatlaf.useWindowDecorations", "true")
                System.setProperty("flatlaf.menuBarEmbedded", "false")

                if (uiStyle != null) applyUiStyle(uiStyle)

                if (isDarkMode) FlatDarkLaf.setup() else FlatLightLaf.setup()
                if (updateExistingWindows) {
                    FlatLaf.updateUI()
                    syncMenuPopupDefaults()

                    FlatLaf.revalidateAndRepaintAllFramesAndDialogs()
                    FlatLaf.repaintAllFramesAndDialogs()
                }
            }.let {}
        }

        when {
            SwingUtilities.isEventDispatchThread() -> install()
            waitUntilApplied -> SwingUtilities.invokeAndWait(install)
            else -> SwingUtilities.invokeLater(install)
        }
    }

    /** Mirrors the current Compose/Jewel theme values into FlatLaf UI defaults. */
    private fun applyUiStyle(uiStyle: UiStyle) {
        val decorationBackground = uiStyle.colors.mainBackground.toUiResource()
        val decorationForeground = uiStyle.contentColor.toUiResource()

        val menuBackground = uiStyle.menuStyle.colors.background.toPlatformColor()
        val menuBorder = uiStyle.menuStyle.colors.border.toPlatformColor()

        val itemColors = uiStyle.menuStyle.colors.itemColors
        val content = itemColors.content.toPlatformColor()
        val contentHovered = itemColors.contentHovered.toPlatformColor()
        val keybindingTint = itemColors.keybindingTint.toPlatformColor()
        val keybindingTintHovered = itemColors.keybindingTintHovered.toPlatformColor()
        val backgroundHovered = itemColors.backgroundHovered.toPlatformColor()

        // FlatTitlePane may use the parent background instead of TitlePane.background.
        UIManager.put("TitlePane.unifiedBackground", false)

        UIManager.put("MenuBar.background", decorationBackground)
        UIManager.put("MenuBar.borderColor", decorationBackground)
        UIManager.put("MenuBar.selectionBackground", backgroundHovered)

        UIManager.put("TitlePane.background", decorationBackground)
        UIManager.put("TitlePane.inactiveBackground", decorationBackground)
        UIManager.put("TitlePane.foreground", decorationForeground)
        UIManager.put("TitlePane.inactiveForeground", decorationForeground)
        UIManager.put("TitlePane.embeddedForeground", decorationForeground)

        UIManager.put("MenuItem.foreground", content)
        UIManager.put("MenuItem.selectionForeground", contentHovered)
        UIManager.put("MenuItem.acceleratorForeground", keybindingTint)
        UIManager.put("MenuItem.selectionAcceleratorForeground", keybindingTintHovered)
        UIManager.put("MenuItem.acceleratorSelectionForeground", keybindingTintHovered)
        UIManager.put("MenuItem.selectionBackground", backgroundHovered)

        UIManager.put("Menu.foreground", content)
        UIManager.put("Menu.selectionForeground", contentHovered)
        UIManager.put("Menu.acceleratorForeground", keybindingTint)
        UIManager.put("Menu.selectionAcceleratorForeground", keybindingTintHovered)
        UIManager.put("Menu.acceleratorSelectionForeground", keybindingTintHovered)
        UIManager.put("Menu.selectionBackground", backgroundHovered)

        UIManager.put("CheckBoxMenuItem.foreground", content)
        UIManager.put("CheckBoxMenuItem.selectionForeground", contentHovered)
        UIManager.put("CheckBoxMenuItem.acceleratorForeground", keybindingTint)
        UIManager.put("CheckBoxMenuItem.selectionAcceleratorForeground", keybindingTintHovered)
        UIManager.put("CheckBoxMenuItem.acceleratorSelectionForeground", keybindingTintHovered)
        UIManager.put("CheckBoxMenuItem.selectionBackground", backgroundHovered)

        UIManager.put("CheckBoxMenuItem.icon.checkmarkColor", content)
        UIManager.put("CheckBoxMenuItem.icon.selectionCheckmarkColor", contentHovered)

        UIManager.put("RadioButtonMenuItem.icon.checkmarkColor", content)
        UIManager.put("RadioButtonMenuItem.icon.selectionCheckmarkColor", contentHovered)

        UIManager.put("PopupMenu.background", menuBackground)
        UIManager.put("PopupMenu.borderCornerRadius", POPUP_MENU_CORNER_RADIUS)
        UIManager.put("PopupMenu.borderColor", menuBorder)
    }

    /** Refreshes the menu infrastructure attached to all open windows. */
    private fun syncMenuPopupDefaults() {
        Window.getWindows().forEach { window ->
            resolveMenuBar(window)?.let(::syncMenuBarDefaults)
        }
    }

    private fun resolveMenuBar(window: Window) = when (window) {
        is JFrame -> window.jMenuBar
        is JDialog -> window.jMenuBar
        is RootPaneContainer -> window.rootPane.jMenuBar
        else -> null
    }

    /** Refreshes one menu bar instance and its submenu tree. */
    private fun syncMenuBarDefaults(menuBar: JMenuBar) {
        // FlatMenuBarUI may otherwise let the parent/title background show through.
        menuBar.isOpaque = true

        UIManager.getColor("MenuBar.background")?.let { menuBar.background = it }
        UIManager.getColor("MenuBar.foreground")?.let { menuBar.foreground = it }

        repeat(menuBar.menuCount) { index ->
            menuBar.getMenu(index)?.let(::syncMenuDefaults)
        }
        menuBar.revalidate()
        menuBar.repaint()
    }

    /** Recursively refreshes submenu trees. */
    private fun syncMenuDefaults(menu: JMenu) {
        syncPopupMenuDefaults(menu.popupMenu)
        repeat(menu.itemCount) { index ->
            (menu.getItem(index) as? JMenu)?.let(::syncMenuDefaults)
        }
    }

    /** Pushes updated defaults into one live popup menu instance. */
    private fun syncPopupMenuDefaults(popupMenu: JPopupMenu) {
        SwingUtilities.updateComponentTreeUI(popupMenu)

        UIManager.getColor("PopupMenu.background")?.let { popupMenu.background = it }
        UIManager.getColor("PopupMenu.foreground")?.let { popupMenu.foreground = it }
        UIManager.getBorder("PopupMenu.border")?.let { popupMenu.border = it }

        popupMenu.components.filterIsInstance<JMenuItem>().forEach(::syncMenuItemDefaults)
        popupMenu.revalidate()
        popupMenu.repaint()
    }

    /** Re-applies the resolved foreground to an existing menu item instance. */
    private fun syncMenuItemDefaults(menuItem: JMenuItem) {
        val uiKey = when (menuItem) {
            is JCheckBoxMenuItem -> "CheckBoxMenuItem"
            is JRadioButtonMenuItem -> "RadioButtonMenuItem"
            is JMenu -> "Menu"
            else -> "MenuItem"
        }

        UIManager.getColor("$uiKey.foreground")?.let { menuItem.foreground = it }
        menuItem.revalidate()
        menuItem.repaint()
    }

    /** Converts a Compose color into a Swing UIResource color used by FlatLaf defaults. */
    private fun Color.toUiResource() = ColorUIResource(toPlatformColor())
}