// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Logging;

/// <summary>
/// Provides application and ADB logs for UI consumption.
/// </summary>
public interface ILogService
{
    /// <summary>
    /// Writes a log entry to the in-memory log store.
    /// </summary>
    void Log(LogLevel level, string category, string message);

    /// <summary>
    /// Returns logs in reverse chronological order.
    /// </summary>
    IReadOnlyList<LogEntry> GetEntries();
}