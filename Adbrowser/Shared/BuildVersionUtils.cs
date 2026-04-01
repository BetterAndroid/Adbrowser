// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

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
        const string shortCommit = ThisAssembly.Git.Commit;
        var buildTime = DateTime.TryParse(ThisAssembly.Git.CommitDate, out var buildDate)
            ? buildDate.ToString("yyyy-MM-dd HH:mm:ss")
            : "<Unknown>";

        return $"Build: {shortCommit} | {buildTime}";
    }
}