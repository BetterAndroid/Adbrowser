// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Windows.Input;

namespace Adbrowser.Frontend.Infrastructure;

/// <summary>
/// Basic command implementation for UI actions.
/// </summary>
public sealed class RelayCommand(Action execute, Func<bool>? canExecute = null) : ICommand
{
    /// <inheritdoc />
    public event EventHandler? CanExecuteChanged;

    /// <inheritdoc />
    public bool CanExecute(object? parameter) => canExecute?.Invoke() ?? true;

    /// <inheritdoc />
    public void Execute(object? parameter) => execute();

    /// <summary>
    /// Raises can-execute state changed.
    /// </summary>
    public void RaiseCanExecuteChanged()
    {
        CanExecuteChanged?.Invoke(this, EventArgs.Empty);
    }
}

/// <summary>
/// Generic command implementation with parameter support.
/// </summary>
public sealed class RelayCommand<T>(Action<T?> execute, Func<T?, bool>? canExecute = null) : ICommand
{
    /// <inheritdoc />
    public event EventHandler? CanExecuteChanged;

    /// <inheritdoc />
    public bool CanExecute(object? parameter)
    {
        return canExecute is null || canExecute(ConvertParameter(parameter));
    }

    /// <inheritdoc />
    public void Execute(object? parameter)
    {
        execute(ConvertParameter(parameter));
    }

    /// <summary>
    /// Raises can-execute state changed.
    /// </summary>
    public void RaiseCanExecuteChanged()
    {
        CanExecuteChanged?.Invoke(this, EventArgs.Empty);
    }

    private static T? ConvertParameter(object? parameter)
    {
        return parameter switch
        {
            null => default,
            T typed => typed,
            _ => (T?)parameter
        };
    }
}