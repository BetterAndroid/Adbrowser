// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Text;
using Avalonia.Platform;

namespace Adbrowser.Frontend.Infrastructure;

/// <summary>
/// Resolves file icon SVG resources with extension-based fallback.
/// </summary>
public static class FileIconResolver
{
    private const string BasePath = "avares://Adbrowser/Frontend/Assets/Icons";

    /// <summary>
    /// Resolves icon uri for entry kind and file extension.
    /// </summary>
    public static Uri Resolve(bool isDirectory, string fileName)
    {
        if (isDirectory)
        {
            return new Uri($"{BasePath}/Folder.svg");
        }

        var extension = Path.GetExtension(fileName).TrimStart('.');
        if (!string.IsNullOrWhiteSpace(extension))
        {
            var candidateName = $"File{ToPascal(extension)}.svg";
            var candidateUri = new Uri($"{BasePath}/{candidateName}");

            if (AssetLoader.Exists(candidateUri))
            {
                return candidateUri;
            }
        }

        return new Uri($"{BasePath}/File.svg");
    }

    private static string ToPascal(string value)
    {
        var parts = value
            .Split(['-', '_', '.'], StringSplitOptions.RemoveEmptyEntries)
            .Where(part => part.Length > 0)
            .ToArray();

        if (parts.Length == 0)
        {
            return string.Empty;
        }

        var sb = new StringBuilder();

        foreach (var part in parts)
        {
            var cleaned = new string(part.Where(char.IsLetterOrDigit).ToArray());
            if (cleaned.Length == 0)
            {
                continue;
            }

            sb.Append(char.ToUpperInvariant(cleaned[0]));
            if (cleaned.Length > 1)
            {
                sb.Append(cleaned[1..].ToLowerInvariant());
            }
        }

        return sb.Length == 0 ? string.Empty : sb.ToString();
    }
}