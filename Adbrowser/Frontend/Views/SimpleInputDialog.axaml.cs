// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Frontend.Runtime;
using Avalonia.Controls;
using Avalonia.Interactivity;
using Avalonia.Threading;

namespace Adbrowser.Frontend.Views;

public partial class SimpleInputDialog : Window
{
    private readonly bool _selectAllOnOpen;
    private readonly Func<string, bool>? _validator;

    public SimpleInputDialog()
        : this(
            AppServices.LocalizationService.GetString("dialog.input.title"),
            string.Empty,
            AppServices.LocalizationService.GetString("dialog.common.ok"),
            AppServices.LocalizationService.GetString("dialog.common.cancel"))
    {
        InvalidInputMessage = AppServices.LocalizationService.GetString("dialog.input.invalid");
    }

    public SimpleInputDialog(
        string title,
        string prompt,
        string okText,
        string cancelText,
        Func<string, bool>? validator = null,
        string? initialValue = null,
        bool selectAllOnOpen = false)
    {
        InitializeComponent();
        Title = title;
        PromptText.Text = prompt;
        PromptText.IsVisible = !string.IsNullOrWhiteSpace(prompt);
        OkButton.Content = okText;
        CancelButton.Content = cancelText;
        _validator = validator;
        _selectAllOnOpen = selectAllOnOpen;
        InputTextBox.Text = initialValue ?? string.Empty;
        InvalidInputMessage = AppServices.LocalizationService.GetString("dialog.input.invalid");

        Opened += OnDialogOpened;
    }

    public string Value => InputTextBox.Text?.Trim() ?? string.Empty;

    public string InvalidInputMessage { get; set; } = string.Empty;

    private void OnOkClick(object? sender, RoutedEventArgs e)
    {
        var value = Value;

        if (_validator is not null && !_validator(value))
        {
            ErrorText.Text = InvalidInputMessage;
            return;
        }

        Close(true);
    }

    private void OnCancelClick(object? sender, RoutedEventArgs e)
    {
        Close(false);
    }

    private void OnDialogOpened(object? sender, EventArgs e)
    {
        Opened -= OnDialogOpened;

        // Delay focus until layout is ready so SelectAll works reliably.
        Dispatcher.UIThread.Post(() =>
        {
            InputTextBox.Focus();

            if (_selectAllOnOpen)
            {
                InputTextBox.SelectAll();
            }
        }, DispatcherPriority.Input);
    }
}