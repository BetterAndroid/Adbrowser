// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Adb.Models;

/// <summary>
/// Represents a connected Android device discovered by ADB.
/// </summary>
public sealed record AndroidDevice(
    string Serial,
    string Name,
    string Model,
    bool IsOnline);