// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Frontend.Runtime;
using Adbrowser.Frontend.ViewModels;
using Avalonia.Controls;

namespace Adbrowser.Frontend.Views;

public partial class PreferencesWindow : Window
{
    public event EventHandler<PreferencesLiveApplyEventArgs>? LiveApplyRequested;

    public PreferencesWindow()
        : this(null)
    {
    }

    public PreferencesWindow(string? deviceSerial)
    {
        InitializeComponent();

        var viewModel = new PreferencesViewModel(
            AppServices.SettingsService,
            AppServices.LocalizationService,
            deviceSerial);
        viewModel.RequestClose += OnRequestClose;
        viewModel.LiveApplyRequested += (_, args) => LiveApplyRequested?.Invoke(this, args);
        DataContext = viewModel;
    }

    private void OnRequestClose(object? sender, EventArgs e)
    {
        Close();
    }
}
