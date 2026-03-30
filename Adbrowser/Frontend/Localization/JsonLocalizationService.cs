// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Text.Json;
using Adbrowser.Backend.Logging;

namespace Adbrowser.Frontend.Localization;

/// <summary>
/// JSON resource-based localization service.
/// </summary>
public sealed class JsonLocalizationService(ILogService logService) : ILocalizationService
{
    private Dictionary<string, string> _strings = [];

    /// <inheritdoc />
    public string CurrentLanguage { get; private set; } = "en-US";

    /// <inheritdoc />
    public async Task SetLanguageAsync(string languageCode, CancellationToken cancellationToken = default)
    {
        var resourcePath = ResolveResourcePath(languageCode);

        if (!File.Exists(resourcePath))
        {
            logService.Log(LogLevel.Warning, "Localization", $"Language resource {languageCode} not found, fallback to en-US.");
            languageCode = "en-US";
            resourcePath = ResolveResourcePath(languageCode);
        }

        await using var stream = File.OpenRead(resourcePath);
        var map = await JsonSerializer.DeserializeAsync<Dictionary<string, string>>(stream, cancellationToken: cancellationToken)
            .ConfigureAwait(false);
        _strings = map ?? [];
        CurrentLanguage = languageCode;
        logService.Log(LogLevel.Information, "Localization", $"Loaded language {languageCode}");
    }

    /// <inheritdoc />
    public string GetString(string key)
    {
        if (_strings.TryGetValue(key, out var value))
        {
            return value;
        }

        return key;
    }

    private static string ResolveResourcePath(string languageCode)
    {
        var baseDir = AppContext.BaseDirectory;
        return Path.Combine(baseDir, "Frontend", "Localization", "Resources", $"strings.{languageCode}.json");
    }
}