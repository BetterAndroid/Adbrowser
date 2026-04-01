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
    private string _adbPath = string.Empty;
    private bool _foldersFirst;
    private LanguageOption _selectedLanguage = new("en-US", "en-US");
    private bool _showHiddenFiles;
    private bool _superuser;

    /// <summary>
    /// Creates preferences view model from current settings.
    /// </summary>
    public PreferencesViewModel(IAppSettingsService settingsService, ILocalizationService localizationService, string? serial)
        : this(settingsService, localizationService)
    {
        _selectedLanguage = Languages.FirstOrDefault(e => e.Code == settingsService.Current.Language)
                            ?? Languages.First(e => e.Code == "en-US");
        _adbPath = settingsService.Current.AdbPath;
        _superuser = settingsService.Current.Superuser;
        _showHiddenFiles = settingsService.Current.ShowHiddenFiles;
        _foldersFirst = settingsService.Current.FoldersFirst;
    }

    /// <summary>
    /// Available language options.
    /// </summary>
    public ObservableCollection<LanguageOption> Languages { get; } =
    [
        new("zh-CN", localizationService.GetString("preferences.languageOption.zhCN")),
        new("en-US", localizationService.GetString("preferences.languageOption.enUS"))
    ];

    /// <summary>
    /// Selected UI language.
    /// </summary>
    public LanguageOption SelectedLanguage
    {
        get => _selectedLanguage;
        set => SetProperty(ref _selectedLanguage, value);
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
    /// Whether to try listing directories with su.
    /// </summary>
    public bool Superuser
    {
        get => _superuser;
        set => SetProperty(ref _superuser, value);
    }

    /// <summary>
    /// Whether hidden files and folders should be shown.
    /// </summary>
    public bool ShowHiddenFiles
    {
        get => _showHiddenFiles;
        set => SetProperty(ref _showHiddenFiles, value);
    }

    /// <summary>
    /// Whether folders should be displayed before files.
    /// </summary>
    public bool FoldersFirst
    {
        get => _foldersFirst;
        set => SetProperty(ref _foldersFirst, value);
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

    public string FilesTabTitle => T("preferences.files");

    /// <summary>
    /// Device tab title.
    /// </summary>
    public string DeviceTabTitle => T("preferences.device");

    public string LanguageLabel => T("preferences.language");
    public string FileListSectionTitle => T("preferences.fileList");
    public string ShowHiddenFilesLabel => T("preferences.showHiddenFiles");
    public string FoldersFirstLabel => T("preferences.foldersFirst");
    public string EnvironmentSectionTitle => T("preferences.environment");
    public string ConnectionSectionTitle => T("preferences.connection");
    public string AdbPathLabel => T("preferences.adbPath");
    public string SuperuserLabel => T("preferences.superuser");
    public string AdbPathWatermark => "/usr/local/bin/adb";
    public string CancelText => T("dialog.common.cancel");
    public string SaveText => T("preferences.save");

    /// <summary>
    /// Raised when settings were saved and caller should close this window.
    /// </summary>
    public event EventHandler? RequestClose;

    private async Task SaveAsync()
    {
        settingsService.Current.Language = SelectedLanguage.Code;
        settingsService.Current.AdbPath = AdbPath.Trim();
        settingsService.Current.Superuser = Superuser;
        settingsService.Current.ShowHiddenFiles = ShowHiddenFiles;
        settingsService.Current.FoldersFirst = FoldersFirst;

        await settingsService.SaveAsync();
        await localizationService.SetLanguageAsync(SelectedLanguage.Code);

        StatusMessage = T("status.preferencesSaved");
        RequestClose?.Invoke(this, EventArgs.Empty);
    }

    private string T(string key) => localizationService.GetString(key);
}

/// <summary>
/// User-friendly language option.
/// </summary>
public sealed record LanguageOption(string Code, string DisplayName)
{
    /// <inheritdoc />
    public override string ToString() => DisplayName;
}