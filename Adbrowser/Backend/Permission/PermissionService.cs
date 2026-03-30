// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Abstractions;
using Adbrowser.Backend.Logging;
using Adbrowser.Backend.Shell;

namespace Adbrowser.Backend.Permission;

/// <summary>
/// Permission service implementation based on ADB shell commands.
/// </summary>
public sealed class PermissionService(IShellCommandExecutor shellCommandExecutor, ILogService logService) : IPermissionService
{
    /// <inheritdoc />
    public async Task<FilePermissionInfo> GetPermissionAsync(string serial, string path, CancellationToken cancellationToken = default)
    {
        logService.Log(LogLevel.Trace, "Permission", $"Reading permission for '{path}'.");

        var output = await shellCommandExecutor.ExecuteFileOperationAsync(
            serial,
            $"ls -ld '{EscapeShell(path)}'",
            cancellationToken);

        var info = ParsePermission(output);
        logService.Log(LogLevel.Trace, "Permission", $"Read permission for '{path}': {info.SymbolicPermission} ({info.NumericPermission}).");
        return info;
    }

    /// <inheritdoc />
    public async Task<OperationResult> SetPermissionAsync(string serial, string path, int mode, CancellationToken cancellationToken = default)
    {
        try
        {
            await shellCommandExecutor.ExecuteFileOperationAsync(serial, $"chmod {mode} '{EscapeShell(path)}'", cancellationToken);
            logService.Log(LogLevel.Information, "Permission", $"Updated permission for '{path}' to {mode}.");
            return OperationResult.Success();
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Error, "Permission", ex.Message);
            return OperationResult.Failure(ex.Message);
        }
    }

    private static string EscapeShell(string value)
    {
        return value.Replace("'", "'\\''");
    }

    private static FilePermissionInfo ParsePermission(string lsOutput)
    {
        var line = lsOutput
            .Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries)
            .Select(e => e.Trim())
            .FirstOrDefault();

        if (string.IsNullOrWhiteSpace(line))
        {
            throw new InvalidOperationException("Permission query returned empty output.");
        }

        var tokens = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        if (tokens.Length == 0 || tokens[0].Length < 10)
        {
            throw new InvalidOperationException($"Unexpected permission output: {line}");
        }

        var raw = tokens[0];
        var symbolic = raw[1..10];
        var numeric = ToNumericPermission(symbolic);
        return new FilePermissionInfo(symbolic, numeric);
    }

    private static int ToNumericPermission(string symbolic)
    {
        if (symbolic.Length != 9)
        {
            throw new ArgumentException("Permission string must be 9 chars.", nameof(symbolic));
        }

        var owner = BitsToOctal(symbolic[0], symbolic[1], symbolic[2]);
        var group = BitsToOctal(symbolic[3], symbolic[4], symbolic[5]);
        var other = BitsToOctal(symbolic[6], symbolic[7], symbolic[8]);
        return owner * 100 + group * 10 + other;
    }

    private static int BitsToOctal(char read, char write, char execute)
    {
        var value = 0;
        if (read is 'r') value += 4;
        if (write is 'w') value += 2;
        if (execute is 'x' or 's' or 't') value += 1;
        return value;
    }
}