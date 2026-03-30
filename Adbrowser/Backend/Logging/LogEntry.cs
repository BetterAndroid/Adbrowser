// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Logging;

/// <summary>
/// Represents a single log entry.
/// </summary>
public sealed record LogEntry(DateTimeOffset Time, LogLevel Level, string Category, string Message);