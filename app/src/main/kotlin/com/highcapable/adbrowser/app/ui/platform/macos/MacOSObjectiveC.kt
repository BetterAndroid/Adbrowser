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

import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer

/**
 * Minimal Objective-C runtime bridge used to patch native macOS menu titles.
 *
 * JDK/JBR only exposes visibility/enabled toggles for the built-in application menu items.
 * To localize those titles we have to reach the Cocoa menu objects directly.
 */
internal object MacOSObjectiveC {

    private val objc = NativeLibrary.getInstance("objc")
    private val objcGetClass = objc.getFunction("objc_getClass")
    private val selRegisterName = objc.getFunction("sel_registerName")
    private val selGetName = objc.getFunction("sel_getName")
    private val objcMsgSend = objc.getFunction("objc_msgSend")

    fun nsClass(name: String) = objcGetClass.invokePointer(arrayOf(name)).orNull()

    fun selector(name: String) = selRegisterName.invokePointer(arrayOf(name)).orNull()

    fun selectorName(selector: Pointer?): String? {
        val validSelector = selector.orNull() ?: return null
        return selGetName.invokeString(arrayOf(validSelector), false)
    }

    fun sendPointer(receiver: Pointer?, selectorName: String, vararg args: Any?): Pointer? {
        val validReceiver = receiver.orNull() ?: return null
        val selector = selector(selectorName).orNull() ?: return null

        return objcMsgSend.invokePointer(arrayOf(validReceiver, selector, *args)).orNull()
    }

    fun sendLong(receiver: Pointer?, selectorName: String, vararg args: Any?): Long {
        val validReceiver = receiver.orNull() ?: return 0L
        val selector = selector(selectorName).orNull() ?: return 0L

        return objcMsgSend.invokeLong(arrayOf(validReceiver, selector, *args))
    }

    fun sendVoid(receiver: Pointer?, selectorName: String, vararg args: Any?) {
        val validReceiver = receiver.orNull() ?: return
        val selector = selector(selectorName).orNull() ?: return
        objcMsgSend.invokeVoid(arrayOf(validReceiver, selector, *args))
    }

    fun nsString(value: String) = sendPointer(
        receiver = nsClass("NSString"),
        selectorName = "stringWithUTF8String:",
        value
    )

    fun stringValue(string: Pointer?): String? {
        val validString = string.orNull() ?: return null
        val utf8String = sendPointer(validString, "UTF8String").orNull() ?: return null

        return utf8String.getString(0)
    }

    private fun Pointer?.orNull() = this?.takeUnless { Pointer.nativeValue(it) == 0L }
}