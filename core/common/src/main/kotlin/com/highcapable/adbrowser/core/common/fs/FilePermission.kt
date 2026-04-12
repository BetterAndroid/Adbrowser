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
 * This file is created by fankes on 2026/4/12.
 */
@file:Suppress("KotlinConstantConditions")

package com.highcapable.adbrowser.core.common.fs

/**
 * Shared file permission helpers used by both frontend and backend layers.
 *
 * This intentionally centralizes all rwx/octal conversion logic so permission parsing and
 * formatting cannot silently diverge between UI models and ADB services.
 */
object FilePermission {

    /**
     * The three standard Unix permission scopes.
     */
    enum class Scope {
        Owner,
        Group,
        Other
    }

    /**
     * The three standard Unix permission bits within one scope.
     */
    enum class Access {
        Read,
        Write,
        Execute
    }

    /**
     * Represents file permission information in both symbolic (rwx) and numeric (octal) forms.
     */
    data class Info(
        val symbolicPermission: String,
        val numericPermission: Int
    )

    /**
     * Parses a chmod-style octal mode such as `755`.
     *
     * Returns `null` when the input is blank, not numeric, outside the 000..777 range, or
     * contains digits that are not valid octal permission values.
     */
    fun parseMode(modeText: String?): Int? {
        val parsed = modeText?.trim()?.toIntOrNull() ?: return null
        if (parsed !in 0..777) return null

        val owner = parsed / 100
        val group = (parsed / 10) % 10
        val other = parsed % 10
        if (owner > 7 || group > 7 || other > 7) return null

        return parsed
    }

    /** Builds a normalized [Info] snapshot from an already validated numeric mode. */
    fun fromMode(mode: Int): Info {
        val normalized = requireMode(mode)

        return Info(
            symbolicPermission = toSymbolic(normalized),
            numericPermission = normalized
        )
    }

    /**
     * Builds an [Info] snapshot from a symbolic permission string such as `rwxr-xr-x`.
     *
     * Special execute markers like `s` and `t` are treated as executable for numeric conversion,
     * matching how `ls -l` output is commonly interpreted by file tools.
     */
    fun fromSymbolic(symbolic: String): Info {
        val normalized = normalizeSymbolic(symbolic)

        return Info(
            symbolicPermission = normalized,
            numericPermission = toNumeric(normalized)
        )
    }

    /** Converts a validated octal mode such as `755` into a symbolic rwx string. */
    fun toSymbolic(mode: Int): String {
        val normalized = requireMode(mode)
        val owner = normalized / 100
        val group = (normalized / 10) % 10
        val other = normalized % 10

        return "${octalToRwx(owner)}${octalToRwx(group)}${octalToRwx(other)}"
    }

    /**
     * Converts a symbolic rwx string such as `rwxr-xr-x` into an octal mode.
     *
     * The input must be exactly 9 permission characters and does not include the file-type prefix
     * from `ls -l` output.
     */
    fun toNumeric(symbolic: String): Int {
        val normalized = normalizeSymbolic(symbolic)
        val owner = bitsToOctal(normalized[0], normalized[1], normalized[2])
        val group = bitsToOctal(normalized[3], normalized[4], normalized[5])
        val other = bitsToOctal(normalized[6], normalized[7], normalized[8])

        return owner * 100 + group * 10 + other
    }

    /** Returns whether the given access bit is currently enabled for one scope of a mode. */
    fun hasAccess(mode: Int, scope: Scope, access: Access): Boolean {
        val bits = segmentBits(requireMode(mode), scope)
        val mask = when (access) {
            Access.Read -> 4
            Access.Write -> 2
            Access.Execute -> 1
        }

        return (bits and mask) != 0
    }

    /**
     * Returns a new mode with one access bit updated.
     *
     * This is useful for UI code that wants to treat a permission mode as a single source of
     * truth while still toggling individual checkbox bits.
     */
    fun setAccess(mode: Int, scope: Scope, access: Access, enabled: Boolean): Int {
        val normalized = requireMode(mode)
        val mask = when (access) {
            Access.Read -> 4
            Access.Write -> 2
            Access.Execute -> 1
        }
        val owner = segmentBits(normalized, Scope.Owner)
        val group = segmentBits(normalized, Scope.Group)
        val other = segmentBits(normalized, Scope.Other)

        fun update(bits: Int) = if (enabled) bits or mask else bits and mask.inv()

        return when (scope) {
            Scope.Owner -> buildMode(update(owner), group, other)
            Scope.Group -> buildMode(owner, update(group), other)
            Scope.Other -> buildMode(owner, group, update(other))
        }
    }

    private fun requireMode(mode: Int) = parseMode(mode.toString())
        ?: error("Permission mode must be a valid three-digit octal value.")

    private fun normalizeSymbolic(symbolic: String): String {
        require(symbolic.length == 9) {
            "Permission string must be exactly 9 characters."
        }

        return symbolic
    }

    private fun octalToRwx(value: Int): String {
        val read = if ((value and 4) != 0) 'r' else '-'
        val write = if ((value and 2) != 0) 'w' else '-'
        val execute = if ((value and 1) != 0) 'x' else '-'

        return "$read$write$execute"
    }

    private fun segmentBits(mode: Int, scope: Scope): Int = when (scope) {
        Scope.Owner -> mode / 100
        Scope.Group -> (mode / 10) % 10
        Scope.Other -> mode % 10
    }

    private fun buildMode(owner: Int, group: Int, other: Int): Int =
        owner * 100 + group * 10 + other

    private fun bitsToOctal(read: Char, write: Char, execute: Char): Int {
        var value = 0
        if (read == 'r') value += 4
        if (write == 'w') value += 2
        if (execute == 'x' || execute == 's' || execute == 't') value += 1

        return value
    }
}