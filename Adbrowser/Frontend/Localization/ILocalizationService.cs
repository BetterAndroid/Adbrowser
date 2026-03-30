// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Frontend.Localization;

/// <summary>
/// Resolves localized strings for UI text.
/// </summary>
public interface ILocalizationService
{
    /// <summary>
    /// Current language code.
    /// </summary>
    string CurrentLanguage { get; }

    /// <summary>
    /// Loads language resources.
    /// </summary>
    Task SetLanguageAsync(string languageCode, CancellationToken cancellationToken = default);

    /// <summary>
    /// Gets localized text by key.
    /// </summary>
    string GetString(string key);
}