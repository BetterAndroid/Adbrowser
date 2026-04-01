// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Collections.ObjectModel;
using System.Windows.Input;
using Adbrowser.Frontend.Configuration;
using Adbrowser.Frontend.Infrastructure;
using Adbrowser.Frontend.Localization;

namespace Adbrowser.Frontend.ViewModels;

/// <summary>
/// View model for preferences window.
/// </summary>
public sealed class PreferencesViewModel(
    IAppSettingsService settingsService,
    ILocalizationService localizationService) : ViewModelBase
{
    private string _language = "en-US";
    private string _adbPath = string.Empty;
    private bool _rememberLastDevice;
    private bool _rememberDevicePath;
    private bool _superuser;
    private string _homePath = "/";

    /// <summary>
    /// Raised when settings were saved and caller should close this window.
    /// </summary>
    public event EventHandler? RequestClose;

    /// <summary>
    /// Creates preferences view model from current settings.
    /// </summary>
    public PreferencesViewModel(IAppSettingsService settingsService, ILocalizationService localizationService, string? serial)
        : this(settingsService, localizationService)
    {
        _language = settingsService.Current.Language;
        _adbPath = settingsService.Current.AdbPath;
        _rememberLastDevice = settingsService.Current.RememberLastDevice;
        _rememberDevicePath = settingsService.Current.RememberDevicePath;
        _superuser = settingsService.Current.Superuser;

        if (!string.IsNullOrWhiteSpace(serial)
            && settingsService.Current.DeviceHomePaths.TryGetValue(serial, out var home)
            && !string.IsNullOrWhiteSpace(home))
        {
            _homePath = home;
        }

        CurrentDeviceSerial = serial ?? string.Empty;
    }

    /// <summary>
    /// Available language options.
    /// </summary>
    public ObservableCollection<string> Languages { get; } = ["en-US", "zh-CN"];

    /// <summary>
    /// Current device serial for home path configuration.
    /// </summary>
    public string CurrentDeviceSerial { get; private set; } = string.Empty;

    /// <summary>
    /// Selected UI language.
    /// </summary>
    public string Language
    {
        get => _language;
        set => SetProperty(ref _language, value);
    }

    /// <summary>
    /// ADB executable path.
    /// </summary>
    public string AdbPath
    {
        get => _adbPath;
        set => SetProperty(ref _adbPath, value);
    }

    /// <summary>
    /// Whether remember last selected device.
    /// </summary>
    public bool RememberLastDevice
    {
        get => _rememberLastDevice;
        set => SetProperty(ref _rememberLastDevice, value);
    }

    /// <summary>
    /// Whether remember each device path.
    /// </summary>
    public bool RememberDevicePath
    {
        get => _rememberDevicePath;
        set => SetProperty(ref _rememberDevicePath, value);
    }

    /// <summary>
    /// Whether to try listing directories with su.
    /// </summary>
    public bool Superuser
    {
        get => _superuser;
        set => SetProperty(ref _superuser, value);
    }

    /// <summary>
    /// Home path for the selected device.
    /// </summary>
    public string HomePath
    {
        get => _homePath;
        set => SetProperty(ref _homePath, value);
    }

    /// <summary>
    /// Result status text.
    /// </summary>
    public string StatusMessage
    {
        get;
        private set => SetProperty(ref field, value);
    } = string.Empty;

    /// <summary>
    /// Save command.
    /// </summary>
    public ICommand SaveCommand => new RelayCommand(async () => await SaveAsync());

    /// <summary>
    /// Cancel command.
    /// </summary>
    public ICommand CancelCommand => new RelayCommand(() => RequestClose?.Invoke(this, EventArgs.Empty));

    /// <summary>
    /// Window title.
    /// </summary>
    public string Title => T("preferences.title");

    /// <summary>
    /// General tab title.
    /// </summary>
    public string GeneralTabTitle => T("preferences.general");

    /// <summary>
    /// Device tab title.
    /// </summary>
    public string DeviceTabTitle => T("preferences.device");

    public string LanguageLabel => T("preferences.language");
    public string AdbPathLabel => T("preferences.adbPath");
    public string RememberLastDeviceLabel => T("preferences.rememberLastDevice");
    public string RememberDevicePathLabel => T("preferences.rememberDevicePath");
    public string SuperuserLabel => T("preferences.superuser");
    public string DeviceHomePathLabel => T("preferences.deviceHomePath");
    public string AdbPathWatermark => "/usr/local/bin/adb";
    public string DeviceHomePathWatermark => "/data/local/tmp";
    public string CancelText => T("dialog.common.cancel");
    public string SaveText => T("preferences.save");

    private async Task SaveAsync()
    {
        settingsService.Current.Language = Language;
        settingsService.Current.AdbPath = AdbPath.Trim();
        settingsService.Current.RememberLastDevice = RememberLastDevice;
        settingsService.Current.RememberDevicePath = RememberDevicePath;
        settingsService.Current.Superuser = Superuser;

        if (!string.IsNullOrWhiteSpace(CurrentDeviceSerial) && !string.IsNullOrWhiteSpace(HomePath))
        {
            settingsService.Current.DeviceHomePaths[CurrentDeviceSerial] = HomePath.Trim();
        }

        await settingsService.SaveAsync();
        await localizationService.SetLanguageAsync(Language);

        StatusMessage = T("status.preferencesSaved");
        RequestClose?.Invoke(this, EventArgs.Empty);
    }

    private string T(string key) => localizationService.GetString(key);
}