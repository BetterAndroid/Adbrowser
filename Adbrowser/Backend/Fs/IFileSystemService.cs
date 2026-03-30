// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Abstractions;
using Adbrowser.Backend.Fs.Models;

namespace Adbrowser.Backend.Fs;

/// <summary>
/// Defines Android filesystem operations.
/// </summary>
public interface IFileSystemService
{
    /// <summary>
    /// Lists file entries under the specified directory.
    /// </summary>
    Task<IReadOnlyList<DeviceFileEntry>> ListAsync(string serial, string path, CancellationToken cancellationToken = default);

    /// <summary>
    /// Searches entries under the specified path.
    /// </summary>
    Task<IReadOnlyList<DeviceFileEntry>> SearchAsync(string serial, string path, string keyword, CancellationToken cancellationToken = default);

    /// <summary>
    /// Creates a folder on device.
    /// </summary>
    Task<OperationResult> CreateFolderAsync(string serial, string parentPath, string folderName, CancellationToken cancellationToken = default);

    /// <summary>
    /// Deletes a file or directory.
    /// </summary>
    Task<OperationResult> DeleteAsync(string serial, string path, CancellationToken cancellationToken = default);

    /// <summary>
    /// Renames a file or directory.
    /// </summary>
    Task<OperationResult> RenameAsync(string serial, string path, string newName, CancellationToken cancellationToken = default);

    /// <summary>
    /// Copies a file or directory to target path.
    /// </summary>
    Task<OperationResult> CopyAsync(string serial, string sourcePath, string targetPath, CancellationToken cancellationToken = default);

    /// <summary>
    /// Moves a file or directory to target path.
    /// </summary>
    Task<OperationResult> MoveAsync(string serial, string sourcePath, string targetPath, CancellationToken cancellationToken = default);
}