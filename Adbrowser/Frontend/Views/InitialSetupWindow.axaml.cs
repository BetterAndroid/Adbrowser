// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Frontend.Runtime;
using Adbrowser.Frontend.ViewModels;
using Avalonia.Controls;
using Avalonia.Interactivity;
using Avalonia.Platform.Storage;

namespace Adbrowser.Frontend.Views;

public partial class InitialSetupWindow : Window
{
    private readonly InitialSetupViewModel _viewModel;

    public InitialSetupWindow()
    {
        InitializeComponent();

        _viewModel = new InitialSetupViewModel(
            AppServices.AdbClient,
            AppServices.SettingsService,
            AppServices.LocalizationService);
        _viewModel.SetupCompleted += OnSetupCompleted;
        DataContext = _viewModel;
    }

    private async void OnBrowseClick(object? sender, RoutedEventArgs e)
    {
        var topLevel = GetTopLevel(this);

        if (topLevel?.StorageProvider is null)
        {
            return;
        }

        var files = await topLevel.StorageProvider.OpenFilePickerAsync(new FilePickerOpenOptions
        {
            Title = AppServices.LocalizationService.GetString("setup.selectAdbExecutable"),
            AllowMultiple = false
        });

        var selected = files.FirstOrDefault();

        if (selected is not null)
        {
            _viewModel.AdbPath = selected.Path.LocalPath;
        }
    }

    private void OnSetupCompleted(object? sender, EventArgs e)
    {
        var mainWindow = new MainWindow();
        mainWindow.Show();
        Close();
    }
}