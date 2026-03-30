// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Windows.Input;
using Adbrowser.Backend.Adb;
using Adbrowser.Frontend.Configuration;
using Adbrowser.Frontend.Infrastructure;
using Adbrowser.Frontend.Localization;

namespace Adbrowser.Frontend.ViewModels;

/// <summary>
/// View model for first-run adb path setup.
/// </summary>
public sealed class InitialSetupViewModel : ViewModelBase
{
    private readonly IAdbClient _adbClient;
    private readonly IAppSettingsService _settingsService;
    private readonly ILocalizationService _localizationService;

    private string _adbPath;
    private readonly RelayCommand _continueCommand;

    /// <summary>
    /// Initializes command instances.
    /// </summary>
    public InitialSetupViewModel(IAdbClient adbClient, IAppSettingsService settingsService, ILocalizationService localizationService)
    {
        _adbClient = adbClient;
        _settingsService = settingsService;
        _localizationService = localizationService;
        _adbPath = settingsService.Current.AdbPath;
        _continueCommand = new RelayCommand(async () => await ContinueAsync(), CanContinue);
    }

    /// <summary>
    /// Raised when setup succeeds.
    /// </summary>
    public event EventHandler? SetupCompleted;

    /// <summary>
    /// Current adb path.
    /// </summary>
    public string AdbPath
    {
        get => _adbPath;
        set
        {
            if (SetProperty(ref _adbPath, value))
            {
                _continueCommand.RaiseCanExecuteChanged();
            }
        }
    }

    /// <summary>
    /// Status message displayed to users.
    /// </summary>
    public string StatusMessage
    {
        get;
        private set => SetProperty(ref field, value);
    } = string.Empty;

    /// <summary>
    /// Indicates whether validation is running.
    /// </summary>
    public bool IsBusy
    {
        get;
        private set
        {
            if (SetProperty(ref field, value))
            {
                _continueCommand.RaiseCanExecuteChanged();
            }
        }
    }

    /// <summary>
    /// Continues to main UI after path validation.
    /// </summary>
    public ICommand ContinueCommand => _continueCommand;

    /// <summary>
    /// Localized window title.
    /// </summary>
    public string Title => T("setup.title");

    /// <summary>
    /// Localized description text.
    /// </summary>
    public string Description => T("setup.description");

    /// <summary>
    /// Localized adb label text.
    /// </summary>
    public string AdbPathLabel => T("setup.adbPath");

    /// <summary>
    /// Localized continue button text.
    /// </summary>
    public string ContinueText => T("setup.continue");

    private async Task ContinueAsync()
    {
        IsBusy = true;
        StatusMessage = string.Empty;

        _adbClient.AdbExecutablePath = AdbPath.Trim();
        var result = await _adbClient.ValidateAdbPathAsync();

        if (!result.IsSuccess)
        {
            StatusMessage = result.ErrorMessage ?? T("common.unknownError");
            IsBusy = false;
            return;
        }

        _settingsService.Current.AdbPath = _adbClient.AdbExecutablePath;
        await _settingsService.SaveAsync();
        IsBusy = false;
        SetupCompleted?.Invoke(this, EventArgs.Empty);
    }

    private bool CanContinue() => !IsBusy && !string.IsNullOrWhiteSpace(AdbPath);

    private string T(string key) => _localizationService.GetString(key);
}