// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Abstractions;

namespace Adbrowser.Backend.Permission;

/// <summary>
/// Handles permission query and update operations for files.
/// </summary>
public interface IPermissionService
{
    /// <summary>
    /// Reads permission info from a device file path.
    /// </summary>
    Task<FilePermissionInfo> GetPermissionAsync(string serial, string path, CancellationToken cancellationToken = default);

    /// <summary>
    /// Updates file mode on the target device.
    /// </summary>
    Task<OperationResult> SetPermissionAsync(string serial, string path, int mode, CancellationToken cancellationToken = default);
}