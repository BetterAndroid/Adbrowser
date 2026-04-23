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
 * This file is created by fankes on 2026/4/23.
 */
package com.highcapable.adbrowser.core.common.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class ShellArgumentsTest {

    @Test
    fun createStyleBuildsQuotedPathCommand() {
        val command = ShellArguments.create()
            .add("ls", "-la")
            .addQuotes("/sdcard/My Folder")
            .build()

        assertEquals("ls -la '/sdcard/My Folder'", command)
    }

    @Test
    fun invokeStyleBuildsQuotedMoveCommand() {
        val command = ShellArguments {
            add("mv")
            addQuotes("/sdcard/a'b", "/sdcard/c\"d")
        }

        assertEquals("mv '/sdcard/a'\\''b' '/sdcard/c\"d'", command)
    }

    @Test
    fun quoteEscapesSingleQuotesForShellLiteral() {
        assertEquals("'a'\\''b'", ShellArguments.quote("a'b"))
    }

    @Test
    fun quotePreservesBackslashesInsideSingleQuotedLiteral() {
        assertEquals("'a\\b'", ShellArguments.quote("a\\b"))
    }

    @Test
    fun quotePreservesBackslashesWhileStillEscapingSingleQuotes() {
        assertEquals("'a\\'\\''b'", ShellArguments.quote("a\\'b"))
    }

    @Test
    fun addRawSupportsShellGrammarFragments() {
        val command = ShellArguments {
            add("if", "[", "-d")
            addQuotes("/sdcard/a'b")
            add("]")
            addRaw(";", "then", "echo")
            addQuotes("0:1")
            addRaw(";", "else", "echo")
            addQuotes("0:0")
            addRaw(";", "fi")
        }

        assertEquals(
            "if [ -d '/sdcard/a'\\''b' ] ; then echo '0:1' ; else echo '0:0' ; fi",
            command
        )
    }

    @Test
    fun quoteSupportsNestedSuCommandPayload() {
        val childCommand = ShellArguments {
            add("ls", "-la")
            addQuotes("/sdcard/a'b")
        }
        val command = ShellArguments {
            add("su", "-c")
            addQuotes(childCommand)
        }

        assertEquals("su -c ${ShellArguments.quote(childCommand)}", command)
    }
}