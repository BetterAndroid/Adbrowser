// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Adb;
using Adbrowser.Backend.Fs;
using Adbrowser.Backend.Logging;
using Adbrowser.Backend.Permission;
using Adbrowser.Backend.Shell;
using Adbrowser.Frontend.Configuration;
using Adbrowser.Frontend.Localization;

namespace Adbrowser.Frontend.Runtime;

/// <summary>
/// Stores singleton-like service instances for application bootstrap.
/// </summary>
public static class AppServices
{
    /// <summary>
    /// Shared application log service.
    /// </summary>
    public static ILogService LogService { get; private set; } = new InMemoryLogService();

    /// <summary>
    /// Shared ADB client service.
    /// </summary>
    public static IAdbClient AdbClient { get; private set; } = null!;

    /// <summary>
    /// Shared file system service.
    /// </summary>
    public static IFileSystemService FileSystemService { get; private set; } = null!;

    /// <summary>
    /// Shared shell command executor.
    /// </summary>
    public static IShellCommandExecutor ShellCommandExecutor { get; private set; } = null!;

    /// <summary>
    /// Shared permission service.
    /// </summary>
    public static IPermissionService PermissionService { get; private set; } = null!;

    /// <summary>
    /// Shared settings service.
    /// </summary>
    public static IAppSettingsService SettingsService { get; private set; } = null!;

    /// <summary>
    /// Shared localization service.
    /// </summary>
    public static ILocalizationService LocalizationService { get; private set; } = null!;

    /// <summary>
    /// Initializes service graph.
    /// </summary>
    public static async Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        LogService = new InMemoryLogService();
        SettingsService = new JsonAppSettingsService(LogService);
        await SettingsService.LoadAsync(cancellationToken).ConfigureAwait(false);

        LocalizationService = new JsonLocalizationService(LogService);
        await LocalizationService.SetLanguageAsync(SettingsService.Current.Language, cancellationToken).ConfigureAwait(false);

        AdbClient = new AdbClient(LogService)
        {
            AdbExecutablePath = SettingsService.Current.AdbPath
        };

        ShellCommandExecutor = new AdbShellCommandExecutor(AdbClient, SettingsService, LogService);
        FileSystemService = new FileSystemService(ShellCommandExecutor, LogService);
        PermissionService = new PermissionService(ShellCommandExecutor, LogService);
    }
}