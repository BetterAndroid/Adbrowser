// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Adb;
using Adbrowser.Backend.Logging;
using Adbrowser.Frontend.Configuration;

namespace Adbrowser.Backend.Shell;

/// <summary>
/// Executes shell commands through adb and applies configured privilege fallback strategy.
/// </summary>
public sealed class AdbShellCommandExecutor(
    IAdbClient adbClient,
    IAppSettingsService settingsService,
    ILogService logService) : IShellCommandExecutor
{
    /// <inheritdoc />
    public async Task<string> ExecuteFileOperationAsync(string serial, string command, CancellationToken cancellationToken = default)
    {
        if (!settingsService.Current.Superuser)
        {
            return await adbClient.ExecuteShellAsync(serial, command, cancellationToken);
        }

        try
        {
            var suCommand = $"su -c \"{EscapeForDoubleQuotedShell(command)}\"";
            return await adbClient.ExecuteShellAsync(serial, suCommand, cancellationToken);
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Warning, "Shell", $"su execution failed: {ex.Message}. Fallback to normal shell.");
            return await adbClient.ExecuteShellAsync(serial, command, cancellationToken);
        }
    }

    private static string EscapeForDoubleQuotedShell(string value)
    {
        return value
            .Replace("\\", "\\\\")
            .Replace("\"", "\\\"")
            .Replace("$", "\\$")
            .Replace("`", "\\`");
    }
}