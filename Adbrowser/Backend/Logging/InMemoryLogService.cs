// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Logging;

/// <summary>
/// Default log store implementation used by the application skeleton.
/// </summary>
public sealed class InMemoryLogService : ILogService
{
    private readonly List<LogEntry> _entries = [];
    private readonly Lock _syncRoot = new();

    /// <inheritdoc />
    public void Log(LogLevel level, string category, string message)
    {
        lock (_syncRoot)
        {
            _entries.Insert(0, new LogEntry(DateTimeOffset.Now, level, category, message));
        }
    }

    /// <inheritdoc />
    public IReadOnlyList<LogEntry> GetEntries()
    {
        lock (_syncRoot)
        {
            return _entries.ToArray();
        }
    }
}