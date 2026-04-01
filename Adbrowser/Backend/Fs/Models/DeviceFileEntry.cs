// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Fs.Models;

/// <summary>
/// Represents a file or directory entry on an Android device.
/// </summary>
public sealed record DeviceFileEntry(
    string Path,
    string Name,
    bool IsDirectory,
    bool IsSymlink,
    long Size,
    DateTimeOffset ModifiedTime,
    string Permission);