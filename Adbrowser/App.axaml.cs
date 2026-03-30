// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Frontend.Runtime;
using Adbrowser.Frontend.Views;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;

namespace Adbrowser;

public sealed class App : Application
{
    /// <inheritdoc />
    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    /// <inheritdoc />
    public override void OnFrameworkInitializationCompleted()
    {
        Task.Run(static async () => await InitializeAsync()).GetAwaiter().GetResult();

        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            desktop.MainWindow = ResolveStartupWindow();
        }

        base.OnFrameworkInitializationCompleted();
    }

    private static async Task InitializeAsync()
    {
        await AppServices.InitializeAsync().ConfigureAwait(false);
    }

    private static Window ResolveStartupWindow()
    {
        var settings = AppServices.SettingsService.Current;

        if (string.IsNullOrWhiteSpace(settings.AdbPath))
        {
            return new InitialSetupWindow();
        }

        AppServices.AdbClient.AdbExecutablePath = settings.AdbPath;
        var validate = Task.Run(static async () =>
            await AppServices.AdbClient.ValidateAdbPathAsync().ConfigureAwait(false)).GetAwaiter().GetResult();
        return validate.IsSuccess ? new MainWindow() : new InitialSetupWindow();
    }
}