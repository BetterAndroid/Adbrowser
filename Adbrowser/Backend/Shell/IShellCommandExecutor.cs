// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Shell;

/// <summary>
/// Defines command execution behavior for device shell operations.
/// </summary>
public interface IShellCommandExecutor
{
    /// <summary>
    /// Executes shell command for file operations with the configured privilege strategy.
    /// </summary>
    Task<string> ExecuteFileOperationAsync(string serial, string command, CancellationToken cancellationToken = default);
}