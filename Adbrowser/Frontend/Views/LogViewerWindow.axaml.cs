// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later
using Avalonia.Controls;
using Avalonia.Interactivity;
using Adbrowser.Frontend.Runtime;
using Adbrowser.Frontend.ViewModels;

namespace Adbrowser.Frontend.Views;

public partial class LogViewerWindow : Window
{
    private readonly LogViewerViewModel _viewModel;

    public LogViewerWindow()
    {
        InitializeComponent();

        _viewModel = new LogViewerViewModel(AppServices.LogService, AppServices.LocalizationService);
        DataContext = _viewModel;

        if (LogGrid.Columns.Count >= 4)
        {
            LogGrid.Columns[0].Header = _viewModel.HeaderTime;
            LogGrid.Columns[1].Header = _viewModel.HeaderLevel;
            LogGrid.Columns[2].Header = _viewModel.HeaderCategory;
            LogGrid.Columns[3].Header = _viewModel.HeaderMessage;
        }

        Loaded += OnLoaded;
    }

    private void OnLoaded(object? sender, RoutedEventArgs e)
    {
        Loaded -= OnLoaded;
        _viewModel.Refresh();
    }
}
