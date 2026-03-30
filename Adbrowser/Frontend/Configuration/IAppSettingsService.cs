// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Frontend.Configuration;

/// <summary>
/// Reads and writes application settings.
/// </summary>
public interface IAppSettingsService
{
    /// <summary>
    /// Gets current settings in memory.
    /// </summary>
    AppSettings Current { get; }

    /// <summary>
    /// Loads settings from local storage.
    /// </summary>
    Task LoadAsync(CancellationToken cancellationToken = default);

    /// <summary>
    /// Persists settings to local storage.
    /// </summary>
    Task SaveAsync(CancellationToken cancellationToken = default);
}