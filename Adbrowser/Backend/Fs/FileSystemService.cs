// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Abstractions;
using Adbrowser.Backend.Fs.Models;
using Adbrowser.Backend.Logging;
using Adbrowser.Backend.Shell;

namespace Adbrowser.Backend.Fs;

/// <summary>
/// Bootstrap filesystem service using ADB shell abstraction.
/// </summary>
public sealed class FileSystemService(IShellCommandExecutor shellCommandExecutor, ILogService logService) : IFileSystemService
{
    /// <inheritdoc />
    public async Task<IReadOnlyList<DeviceFileEntry>> ListAsync(string serial, string path, CancellationToken cancellationToken = default)
    {
        logService.Log(LogLevel.Trace, "FS", $"List path '{path}' for '{serial}'.");

        var safePath = EscapeShell(path);
        var lsCommand = $"ls -la '{safePath}'";
        var output = await shellCommandExecutor.ExecuteFileOperationAsync(serial, lsCommand, cancellationToken);

        var entries = ParseLsOutput(path, output);

        if (entries.Count == 0)
        {
            logService.Log(LogLevel.Warning, "FS", $"No entries parsed from '{path}'. Raw output: {output}");
        }

        return entries;
    }

    /// <inheritdoc />
    public async Task<IReadOnlyList<DeviceFileEntry>> SearchAsync(string serial, string path, string keyword,
        CancellationToken cancellationToken = default)
    {
        var entries = await ListAsync(serial, path, cancellationToken);
        var result = entries.Where(e => e.Name.Contains(keyword, StringComparison.OrdinalIgnoreCase)).ToArray();
        logService.Log(LogLevel.Information, "FS", $"Search '{keyword}' returned {result.Length} entries.");
        return result;
    }

    /// <inheritdoc />
    public async Task<OperationResult> CreateFolderAsync(string serial, string parentPath, string folderName,
        CancellationToken cancellationToken = default)
    {
        try
        {
            var parent = EscapeShell(parentPath.TrimEnd('/'));
            var name = EscapeShell(folderName);
            var command = $"mkdir -p '{parent}/{name}'";
            await shellCommandExecutor.ExecuteFileOperationAsync(serial, command, cancellationToken);

            logService.Log(LogLevel.Information, "FS", $"Created folder '{folderName}' under '{parentPath}'.");
            return OperationResult.Success();
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Error, "FS", ex.Message);
            return OperationResult.Failure(ex.Message);
        }
    }

    /// <inheritdoc />
    public async Task<OperationResult> DeleteAsync(string serial, string path, CancellationToken cancellationToken = default)
    {
        try
        {
            await shellCommandExecutor.ExecuteFileOperationAsync(serial, $"rm -rf '{EscapeShell(path)}'", cancellationToken);
            logService.Log(LogLevel.Warning, "FS", $"Deleted path '{path}'.");
            return OperationResult.Success();
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Error, "FS", ex.Message);
            return OperationResult.Failure(ex.Message);
        }
    }

    /// <inheritdoc />
    public async Task<OperationResult> RenameAsync(string serial, string path, string newName, CancellationToken cancellationToken = default)
    {
        try
        {
            var targetPath = BuildTargetPath(path, newName);
            await shellCommandExecutor.ExecuteFileOperationAsync(serial, $"mv '{EscapeShell(path)}' '{EscapeShell(targetPath)}'", cancellationToken);
            logService.Log(LogLevel.Information, "FS", $"Renamed '{path}' to '{targetPath}'.");
            return OperationResult.Success();
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Error, "FS", ex.Message);
            return OperationResult.Failure(ex.Message);
        }
    }

    /// <inheritdoc />
    public async Task<OperationResult> CopyAsync(string serial, string sourcePath, string targetPath, CancellationToken cancellationToken = default)
    {
        try
        {
            await shellCommandExecutor.ExecuteFileOperationAsync(serial, $"cp -a '{EscapeShell(sourcePath)}' '{EscapeShell(targetPath)}'",
                cancellationToken);
            logService.Log(LogLevel.Information, "FS", $"Copied '{sourcePath}' to '{targetPath}'.");
            return OperationResult.Success();
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Error, "FS", ex.Message);
            return OperationResult.Failure(ex.Message);
        }
    }

    /// <inheritdoc />
    public async Task<OperationResult> MoveAsync(string serial, string sourcePath, string targetPath, CancellationToken cancellationToken = default)
    {
        try
        {
            await shellCommandExecutor.ExecuteFileOperationAsync(serial, $"mv '{EscapeShell(sourcePath)}' '{EscapeShell(targetPath)}'",
                cancellationToken);
            logService.Log(LogLevel.Information, "FS", $"Moved '{sourcePath}' to '{targetPath}'.");
            return OperationResult.Success();
        }
        catch (Exception ex)
        {
            logService.Log(LogLevel.Error, "FS", ex.Message);
            return OperationResult.Failure(ex.Message);
        }
    }

    private static string EscapeShell(string value)
    {
        return value.Replace("'", "'\\''");
    }

    private static string BuildTargetPath(string sourcePath, string newName)
    {
        var normalized = sourcePath.TrimEnd('/');
        var index = normalized.LastIndexOf('/');
        if (index <= 0)
        {
            return $"/{newName}";
        }

        var parent = normalized[..index];
        return $"{parent}/{newName}";
    }

    private static IReadOnlyList<DeviceFileEntry> ParseLsOutput(string parentPath, string output)
    {
        var result = new List<DeviceFileEntry>();
        var lines = output.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries);

        foreach (var raw in lines)
        {
            var line = raw.Trim();
            if (line.StartsWith("total ", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            if (line.Length < 11)
            {
                continue;
            }

            var permission = line[..10];
            if (!(permission[0] is 'd' or '-' or 'l'))
            {
                continue;
            }

            // Keep filename intact by limiting split count; the last token contains full name.
            var tokens = line.Split(' ', 9, StringSplitOptions.RemoveEmptyEntries);
            if (tokens.Length < 8)
            {
                continue;
            }

            if (!long.TryParse(tokens[4], out var size))
            {
                continue;
            }

            var nameIndex = tokens.Length - 1;
            var name = tokens[nameIndex];
            if (string.IsNullOrWhiteSpace(name)
                || name is "." or ".."
                || name.EndsWith(" ->", StringComparison.Ordinal))
            {
                continue;
            }

            if (name.Contains(" -> ", StringComparison.Ordinal))
            {
                name = name[..name.IndexOf(" -> ", StringComparison.Ordinal)];
            }

            var date = DateTimeOffset.Now;

            if (nameIndex > 5)
            {
                var combined = string.Join(' ', tokens[5..nameIndex]);
                if (DateTimeOffset.TryParse(combined, out var parsed))
                {
                    date = parsed;
                }
            }

            result.Add(new DeviceFileEntry(
                parentPath,
                name,
                permission[0] == 'd',
                size,
                date,
                permission[1..]));
        }

        return result;
    }
}