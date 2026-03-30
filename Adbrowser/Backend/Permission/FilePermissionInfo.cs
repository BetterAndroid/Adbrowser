// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Permission;

/// <summary>
/// Represents parsed file permission details.
/// </summary>
public sealed record FilePermissionInfo(string SymbolicPermission, int NumericPermission);