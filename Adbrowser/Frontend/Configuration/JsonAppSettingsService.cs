// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Text.Json;
using Adbrowser.Backend.Logging;

namespace Adbrowser.Frontend.Configuration;

/// <summary>
/// JSON-based settings service for desktop platforms.
/// </summary>
public sealed class JsonAppSettingsService(ILogService logService) : IAppSettingsService
{
    private static readonly JsonSerializerOptions SerializerOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    /// <inheritdoc />
    public AppSettings Current { get; private set; } = new();

    /// <inheritdoc />
    public async Task LoadAsync(CancellationToken cancellationToken = default)
    {
        var filePath = GetSettingsFilePath();
        var folder = Path.GetDirectoryName(filePath)!;
        Directory.CreateDirectory(folder);

        if (!File.Exists(filePath))
        {
            Current = new AppSettings();
            await SaveAsync(cancellationToken).ConfigureAwait(false);
            return;
        }

        await using var stream = File.OpenRead(filePath);
        var loaded = await JsonSerializer.DeserializeAsync<AppSettings>(stream, SerializerOptions, cancellationToken).ConfigureAwait(false);
        Current = loaded ?? new AppSettings();
        logService.Log(LogLevel.Information, "Settings", $"Loaded settings from {filePath}");
    }

    /// <inheritdoc />
    public async Task SaveAsync(CancellationToken cancellationToken = default)
    {
        var filePath = GetSettingsFilePath();
        var folder = Path.GetDirectoryName(filePath)!;
        Directory.CreateDirectory(folder);

        await using var stream = File.Create(filePath);
        await JsonSerializer.SerializeAsync(stream, Current, SerializerOptions, cancellationToken).ConfigureAwait(false);
        await stream.FlushAsync(cancellationToken).ConfigureAwait(false);
        logService.Log(LogLevel.Trace, "Settings", $"Saved settings to {filePath}");
    }

    private static string GetSettingsFilePath()
    {
        var basePath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        return Path.Combine(basePath, "Adbrowser", "settings.json");
    }
}