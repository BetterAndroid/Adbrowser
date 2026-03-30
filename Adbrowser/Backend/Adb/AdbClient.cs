// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Diagnostics;
using Adbrowser.Backend.Abstractions;
using Adbrowser.Backend.Adb.Models;
using Adbrowser.Backend.Logging;

namespace Adbrowser.Backend.Adb;

/// <summary>
/// Basic ADB client implementation for project bootstrap.
/// </summary>
public sealed class AdbClient(ILogService logService) : IAdbClient
{
    /// <inheritdoc />
    public string AdbExecutablePath { get; set; } = string.Empty;

    /// <inheritdoc />
    public async Task<OperationResult> ValidateAdbPathAsync(CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(AdbExecutablePath))
        {
            return OperationResult.Failure("ADB path is empty.");
        }

        if (!File.Exists(AdbExecutablePath))
        {
            return OperationResult.Failure("ADB executable was not found.");
        }

        if (!OperatingSystem.IsWindows())
        {
            try
            {
                var mode = File.GetUnixFileMode(AdbExecutablePath);
                const UnixFileMode executableFlag = UnixFileMode.UserExecute | UnixFileMode.GroupExecute | UnixFileMode.OtherExecute;

                if ((mode & executableFlag) == 0)
                {
                    return OperationResult.Failure("ADB executable is not executable.");
                }
            }
            catch
            {
                // Ignore mode check failure and rely on process start below.
            }
        }

        try
        {
            var response = await RunAdbAsync(["version"], cancellationToken);

            if (response.ExitCode != 0)
            {
                var message = string.IsNullOrWhiteSpace(response.StandardError)
                    ? response.StandardOutput
                    : response.StandardError;
                return OperationResult.Failure($"ADB validation failed: {message.Trim()}");
            }
        }
        catch (Exception ex)
        {
            return OperationResult.Failure($"Failed to run adb: {ex.Message}");
        }

        logService.Log(LogLevel.Information, "ADB", $"Validated ADB path: {AdbExecutablePath}");
        return OperationResult.Success();
    }

    /// <inheritdoc />
    public async Task<IReadOnlyList<AndroidDevice>> ListDevicesAsync(CancellationToken cancellationToken = default)
    {
        logService.Log(LogLevel.Trace, "ADB", "Listing devices via adb.");
        var response = await RunAdbAsync(["devices", "-l"], cancellationToken);

        if (response.ExitCode != 0)
        {
            var message = string.IsNullOrWhiteSpace(response.StandardError)
                ? response.StandardOutput
                : response.StandardError;
            throw new InvalidOperationException($"Failed to list devices: {message.Trim()}");
        }

        var result = new List<AndroidDevice>();
        var lines = response.StandardOutput
            .Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries)
            .Select(e => e.Trim())
            .ToArray();

        foreach (var line in lines)
        {
            if (line.StartsWith("List of devices attached", StringComparison.OrdinalIgnoreCase)
                || line.StartsWith('*'))
            {
                continue;
            }

            var tokens = line.Split([' ', '\t'], StringSplitOptions.RemoveEmptyEntries);
            if (tokens.Length < 2)
            {
                continue;
            }

            var serial = tokens[0];
            var state = tokens[1];
            var isOnline = string.Equals(state, "device", StringComparison.OrdinalIgnoreCase);

            var model = string.Empty;
            var name = string.Empty;

            foreach (var token in tokens.Skip(2))
            {
                if (token.StartsWith("model:", StringComparison.OrdinalIgnoreCase))
                {
                    model = token[6..].Replace('_', ' ');
                }
                else if (token.StartsWith("device:", StringComparison.OrdinalIgnoreCase))
                {
                    name = token[7..].Replace('_', ' ');
                }
            }

            if (string.IsNullOrWhiteSpace(name))
            {
                name = !string.IsNullOrWhiteSpace(model) ? model : serial;
            }

            if (string.IsNullOrWhiteSpace(model))
            {
                model = name;
            }

            result.Add(new AndroidDevice(serial, name, model, isOnline));
        }

        return result;
    }

    /// <inheritdoc />
    public async Task<string> ExecuteShellAsync(string serial, string command, CancellationToken cancellationToken = default)
    {
        logService.Log(LogLevel.Trace, "ADB", $"[{serial}] $ {command}");

        var response = await RunAdbAsync(["-s", serial, "shell", command], cancellationToken);

        if (response.ExitCode == 0) return response.StandardOutput;
        var message = string.IsNullOrWhiteSpace(response.StandardError)
            ? response.StandardOutput
            : response.StandardError;
        throw new InvalidOperationException($"ADB shell command failed: {message.Trim()}");
    }

    private async Task<AdbResponse> RunAdbAsync(IReadOnlyList<string> arguments, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(AdbExecutablePath))
        {
            throw new InvalidOperationException("ADB path is not configured.");
        }

        var startInfo = new ProcessStartInfo
        {
            FileName = AdbExecutablePath,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        foreach (var argument in arguments)
        {
            startInfo.ArgumentList.Add(argument);
        }

        using var process = new Process();
        process.StartInfo = startInfo;

        if (!process.Start())
        {
            throw new InvalidOperationException("Failed to start adb process.");
        }

        var stdOutTask = process.StandardOutput.ReadToEndAsync(cancellationToken);
        var stdErrTask = process.StandardError.ReadToEndAsync(cancellationToken);
        await process.WaitForExitAsync(cancellationToken);

        return new AdbResponse(
            process.ExitCode,
            await stdOutTask,
            await stdErrTask);
    }
}

internal sealed record AdbResponse(int ExitCode, string StandardOutput, string StandardError);