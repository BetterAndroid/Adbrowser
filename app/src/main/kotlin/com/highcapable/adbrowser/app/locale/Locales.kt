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
 * This file is created by fankes on 2025/6/4.
 */
package com.highcapable.adbrowser.app.locale

import androidx.compose.runtime.Composable
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import cafe.adriel.lyricist.strings
import java.util.Locale

object Locales {
    const val FOLLOW_SYSTEM = ""
    const val EN = "en"
    const val ZH_CN = "zh-CN"
}

data class LanguageOption(
    val tag: String,
    val displayName: String
)

private val SupportedLanguageTags = setOf(Locales.EN, Locales.ZH_CN)
private val SelectableLanguageTags = listOf(
    Locales.FOLLOW_SYSTEM,
    Locales.EN,
    Locales.ZH_CN
)

fun normalizeStoredLanguageTag(tag: String): String {
    val normalized = tag.trim()
    if (normalized.isBlank()) return Locales.FOLLOW_SYSTEM

    return when (normalized.lowercase()) {
        "en",
        "en-us" -> Locales.EN
        "zh-cn",
        "zh" -> Locales.ZH_CN
        else -> if (normalized in SupportedLanguageTags) normalized else Locales.FOLLOW_SYSTEM
    }
}

fun resolveAppLanguageTag(storedLanguageTag: String, systemLocale: Locale = Locale.getDefault()): String {
    val normalized = normalizeStoredLanguageTag(storedLanguageTag)
    if (normalized.isNotBlank()) return normalized

    return resolveSystemLanguageTag(systemLocale)
}

fun resolveSystemLanguageTag(systemLocale: Locale = Locale.getDefault()) = when (systemLocale.language.lowercase()) {
    "zh" -> Locales.ZH_CN
    else -> Locales.EN
}

@Composable
fun languageOptions(): List<LanguageOption> = SelectableLanguageTags.map {
    LanguageOption(
        tag = it,
        displayName = languageDisplayName(it)
    )
}

@Composable
fun languageDisplayName(tag: String): String = when (tag) {
    Locales.FOLLOW_SYSTEM -> strings.preferencesLanguageOptionFollowSystem
    Locales.EN -> strings.preferencesLanguageOptionEnglish
    Locales.ZH_CN -> strings.preferencesLanguageOptionZhCN
    else -> tag
}

@Composable
fun ProvidedLocales(settingsLanguageTag: String, content: @Composable () -> Unit) {
    val lyricist = rememberStrings(
        defaultLanguageTag = Locales.EN,
        currentLanguageTag = resolveAppLanguageTag(settingsLanguageTag)
    )

    ProvideStrings(lyricist, content)
}