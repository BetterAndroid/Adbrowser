// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Abstractions;
using Adbrowser.Backend.Adb.Models;

namespace Adbrowser.Backend.Adb;

/// <summary>
/// Defines ADB communication capabilities used by frontend modules.
/// </summary>
public interface IAdbClient
{
    /// <summary>
    /// Gets or sets current adb executable path.
    /// </summary>
    string AdbExecutablePath { get; set; }

    /// <summary>
    /// Verifies whether the configured adb executable path is valid.
    /// </summary>
    Task<OperationResult> ValidateAdbPathAsync(CancellationToken cancellationToken = default);

    /// <summary>
    /// Lists connected devices and their online states.
    /// </summary>
    Task<IReadOnlyList<AndroidDevice>> ListDevicesAsync(CancellationToken cancellationToken = default);

    /// <summary>
    /// Executes an adb shell command for the target device.
    /// </summary>
    Task<string> ExecuteShellAsync(string serial, string command, CancellationToken cancellationToken = default);
}