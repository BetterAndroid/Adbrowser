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
 */
package com.highcapable.adbrowser.frontend.state

import java.util.prefs.Preferences

/**
 * Preferences persistence based on java.util.prefs.
 */
class AppPreferencesStore {

    private val prefs = Preferences.userRoot().node("com/highcapable/adbrowser")

    /** Loads user preferences from local storage. */
    fun load(): AppPreferences {
        return AppPreferences(
            adbExecutablePath = prefs.get(KEY_ADB_PATH, ""),
            rememberLastDevice = prefs.getBoolean(KEY_REMEMBER_LAST_DEVICE, true),
            rememberDevicePath = prefs.getBoolean(KEY_REMEMBER_DEVICE_PATH, true),
            deviceHomePaths = decodeMap(prefs.get(KEY_DEVICE_HOME_PATHS, "")),
            deviceLastPaths = decodeMap(prefs.get(KEY_DEVICE_LAST_PATHS, "")),
            languageTag = prefs.get(KEY_LANGUAGE_TAG, "en")
        )
    }

    /** Saves user preferences to local storage. */
    fun save(preferences: AppPreferences) {
        prefs.put(KEY_ADB_PATH, preferences.adbExecutablePath)
        prefs.putBoolean(KEY_REMEMBER_LAST_DEVICE, preferences.rememberLastDevice)
        prefs.putBoolean(KEY_REMEMBER_DEVICE_PATH, preferences.rememberDevicePath)
        prefs.put(KEY_DEVICE_HOME_PATHS, encodeMap(preferences.deviceHomePaths))
        prefs.put(KEY_DEVICE_LAST_PATHS, encodeMap(preferences.deviceLastPaths))
        prefs.put(KEY_LANGUAGE_TAG, preferences.languageTag)
        prefs.flush()
    }

    private fun encodeMap(map: Map<String, String>): String {
        return map.entries.joinToString("||") { "${escape(it.key)}==${escape(it.value)}" }
    }

    private fun decodeMap(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split("||")
            .asSequence()
            .mapNotNull { pair ->
                val idx = pair.indexOf("==")
                if (idx <= 0) return@mapNotNull null
                val key = unescape(pair.substring(0, idx))
                val value = unescape(pair.substring(idx + 2))
                if (key.isBlank()) null else key to value
            }
            .toMap()
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("|", "\\|")
            .replace("=", "\\=")
    }

    private fun unescape(value: String): String {
        val output = StringBuilder(value.length)
        var escaping = false
        value.forEach { c ->
            if (escaping) {
                output.append(c)
                escaping = false
            } else if (c == '\\') {
                escaping = true
            } else {
                output.append(c)
            }
        }
        if (escaping) output.append('\\')
        return output.toString()
    }

    private companion object {
        const val KEY_ADB_PATH = "adbPath"
        const val KEY_REMEMBER_LAST_DEVICE = "rememberLastDevice"
        const val KEY_REMEMBER_DEVICE_PATH = "rememberDevicePath"
        const val KEY_DEVICE_HOME_PATHS = "deviceHomePaths"
        const val KEY_DEVICE_LAST_PATHS = "deviceLastPaths"
        const val KEY_LANGUAGE_TAG = "languageTag"
    }
}
