// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Frontend.Runtime;
using Avalonia.Controls;
using Avalonia.Interactivity;

namespace Adbrowser.Frontend.Views;

public partial class ConfirmDialog : Window
{
    public ConfirmDialog()
        : this(
            AppServices.LocalizationService.GetString("dialog.confirm.title"),
            string.Empty,
            AppServices.LocalizationService.GetString("dialog.common.yes"),
            AppServices.LocalizationService.GetString("dialog.common.no"))
    {
    }

    public ConfirmDialog(string title, string message, string yesText, string noText)
    {
        InitializeComponent();
        Title = title;
        MessageText.Text = message;
        YesButton.Content = yesText;
        NoButton.Content = noText;
    }

    private void OnYesClick(object? sender, RoutedEventArgs e)
    {
        Close(true);
    }

    private void OnNoClick(object? sender, RoutedEventArgs e)
    {
        Close(false);
    }
}