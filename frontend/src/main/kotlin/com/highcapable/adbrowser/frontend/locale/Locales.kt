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
package com.highcapable.adbrowser.frontend.locale

import androidx.compose.runtime.Composable
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import java.util.*

object Locales {
    const val EN = "en"
    const val ZH_CN = "zh-CN"
}

@Composable
fun ProvidedLocales(content: @Composable () -> Unit) {
    val lyricist = rememberStrings(
        defaultLanguageTag = Locales.EN,
        currentLanguageTag = getCurrentLanguageTagFromLocalStorage()
    )

    ProvideStrings(lyricist, content)
}

private fun getCurrentLanguageTagFromLocalStorage(): String {
    // This function should retrieve the current language tag from local storage.
    // For now, we return the default language tag.
    val locale = Locale.getDefault()
    val langRegion = "${locale.language}-${locale.country}" // e.g. "zh-CN"
    return langRegion
}