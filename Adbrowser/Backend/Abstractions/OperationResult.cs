// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

namespace Adbrowser.Backend.Abstractions;

/// <summary>
/// Represents a generic operation result.
/// </summary>
public sealed record OperationResult(bool IsSuccess, string? ErrorMessage = null)
{
    /// <summary>
    /// Creates a successful operation result.
    /// </summary>
    public static OperationResult Success() => new(true);

    /// <summary>
    /// Creates a failed operation result.
    /// </summary>
    public static OperationResult Failure(string errorMessage) => new(false, errorMessage);
}