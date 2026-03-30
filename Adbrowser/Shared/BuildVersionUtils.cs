// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Diagnostics;
using System.Globalization;
using System.Reflection;
using System.Text.RegularExpressions;

namespace Adbrowser.Shared;

/// <summary>
/// Provides build version text for UI display.
/// </summary>
public static class BuildVersionUtils
{
    /// <summary>
    /// Builds a user-facing version string in the format:
    /// Build: {shortCommit} | {buildTime}
    /// </summary>
    public static string GetBuildVersionText()
    {
        var informationalVersion = Assembly.GetExecutingAssembly()
            .GetCustomAttribute<AssemblyInformationalVersionAttribute>()?
            .InformationalVersion;
        var value = string.IsNullOrWhiteSpace(informationalVersion) ? "dev" : informationalVersion;
        var hashMatch = Regex.Match(value, @"[0-9a-fA-F]{7,40}");
        var commit = hashMatch.Success ? hashMatch.Value : value;
        var shortCommit = commit.Length > 7 ? commit[..7] : commit;

        var buildTime = TryGetGitCommitTime(commit) ?? TryGetAssemblyWriteTime() ?? "Unknown Build Time";
        return $"Build: {shortCommit} | {buildTime}";
    }

    private static string? TryGetGitCommitTime(string commit)
    {
        try
        {
            var repositoryRoot = TryFindGitRepositoryRoot(AppContext.BaseDirectory);
            if (string.IsNullOrWhiteSpace(repositoryRoot))
            {
                return null;
            }

            using var process = new Process();
            process.StartInfo = new ProcessStartInfo
            {
                FileName = "git",
                Arguments = $"-C \"{repositoryRoot}\" show -s --format=%ci {commit}",
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            process.Start();
            if (!process.WaitForExit(1200))
            {
                return null;
            }

            var output = process.StandardOutput.ReadToEnd().Trim();
            if (process.ExitCode != 0 || string.IsNullOrWhiteSpace(output))
            {
                return null;
            }

            return DateTimeOffset.TryParse(output, CultureInfo.InvariantCulture, DateTimeStyles.None, out var commitDate)
                ? commitDate.LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss")
                : null;
        }
        catch
        {
            return null;
        }
    }

    private static string? TryGetAssemblyWriteTime()
    {
        try
        {
            var assemblyPath = Assembly.GetExecutingAssembly().Location;
            if (string.IsNullOrWhiteSpace(assemblyPath) || !File.Exists(assemblyPath))
            {
                return null;
            }

            return File.GetLastWriteTime(assemblyPath).ToString("yyyy-MM-dd HH:mm:ss");
        }
        catch
        {
            return null;
        }
    }

    private static string? TryFindGitRepositoryRoot(string startPath)
    {
        try
        {
            var directory = new DirectoryInfo(startPath);
            while (directory != null)
            {
                if (Directory.Exists(Path.Combine(directory.FullName, ".git")))
                {
                    return directory.FullName;
                }

                directory = directory.Parent;
            }

            return null;
        }
        catch
        {
            return null;
        }
    }
}