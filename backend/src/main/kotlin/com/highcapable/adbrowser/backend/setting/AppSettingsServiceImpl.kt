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
 * This file is created by fankes on 2026/4/2.
 */
package com.highcapable.adbrowser.backend.setting

import com.highcapable.adbrowser.backend.logging.LogLevel
import com.highcapable.adbrowser.backend.logging.LogService
import com.highcapable.adbrowser.generated.AdbrowserProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * App settings service for desktop platforms.
 */
class AppSettingsServiceImpl(private val logService: LogService) : AppSettingsService {

    private companion object {

        const val SETTINGS_FILE_NAME = "settings.json"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override var current: AppSettings = AppSettings()
        private set

    /**
     * Loads settings file or creates default file if it does not exist.
     */
    override suspend fun load() = withContext(Dispatchers.IO) {
        val filePath = getSettingsFilePath()
        Files.createDirectories(filePath.parent)

        if (!Files.exists(filePath)) {
            current = AppSettings()
            save()
            return@withContext
        }

        val content = Files.readString(filePath, StandardCharsets.UTF_8)
        current = runCatching { json.decodeFromString<AppSettings>(content) }.getOrDefault(AppSettings())
        logService.log(LogLevel.Information, "Settings", "Loaded settings from $filePath")
    }

    /**
     * Saves in-memory settings to JSON file.
     */
    override suspend fun save() = withContext(Dispatchers.IO) {
        val filePath = getSettingsFilePath()
        Files.createDirectories(filePath.parent)
        val encoded = json.encodeToString(current)
        Files.writeString(filePath, encoded, StandardCharsets.UTF_8)
        logService.log(LogLevel.Trace, "Settings", "Saved settings to $filePath")
    }

    private fun getSettingsFilePath(): Path {
        val userHome = System.getProperty("user.home")
        val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
        val basePath = if (appData != null) Paths.get(appData) else Paths.get(userHome, ".config")

        return basePath.resolve(AdbrowserProperties.PROJECT_NAME).resolve(SETTINGS_FILE_NAME)
    }
}