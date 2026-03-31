// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Frontend.Configuration;

/// <summary>
/// Stores user preferences persisted on local machine.
/// </summary>
public sealed class AppSettings
{
    /// <summary>
    /// UI language code (e.g. en-US, zh-CN).
    /// </summary>
    public string Language { get; set; } = "en-US";

    /// <summary>
    /// Configured adb executable path.
    /// </summary>
    public string AdbPath { get; set; } = string.Empty;

    /// <summary>
    /// Whether to remember last selected device.
    /// </summary>
    public bool RememberLastDevice { get; set; } = true;

    /// <summary>
    /// Whether to remember last visited path per device.
    /// </summary>
    public bool RememberDevicePath { get; set; } = true;

    /// <summary>
    /// Whether to try running directory listing with su first.
    /// </summary>
    public bool TrySuForDirectoryListing { get; set; }

    /// <summary>
    /// Last selected device serial.
    /// </summary>
    public string LastDeviceSerial { get; set; } = string.Empty;

    /// <summary>
    /// Stores configured home path for each device serial.
    /// </summary>
    public Dictionary<string, string> DeviceHomePaths { get; set; } = [];

    /// <summary>
    /// Stores last visited path for each device serial.
    /// </summary>
    public Dictionary<string, string> DeviceLastPaths { get; set; } = [];

    /// <summary>
    /// Left device pane width in pixels.
    /// </summary>
    public double DevicePaneWidth { get; set; } = 300;

    /// <summary>
    /// File list name column width in pixels.
    /// </summary>
    public double FileColumnWidthName { get; set; } = 360;

    /// <summary>
    /// File list size column width in pixels.
    /// </summary>
    public double FileColumnWidthSize { get; set; } = 140;

    /// <summary>
    /// File list modified-time column width in pixels.
    /// </summary>
    public double FileColumnWidthModified { get; set; } = 240;

    /// <summary>
    /// File list permission column width in pixels.
    /// </summary>
    public double FileColumnWidthPermission { get; set; } = 150;
}