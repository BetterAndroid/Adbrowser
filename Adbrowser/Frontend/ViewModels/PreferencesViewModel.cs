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
    private const double DefaultDevicePaneWidth = 300;
    private const double DefaultFileColumnWidthName = 360;
    private const double DefaultFileColumnWidthSize = 140;
    private const double DefaultFileColumnWidthModified = 240;
    private const double DefaultFileColumnWidthPermission = 150;

    private string _adbPath = string.Empty;
    private bool _foldersFirst;
    private bool _rememberLastDisplayStyle;
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
        _rememberLastDisplayStyle = settingsService.Current.RememberLastDisplayStyle;
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
    /// Whether to remember last selected list/icon display style.
    /// </summary>
    public bool RememberLastDisplayStyle
    {
        get => _rememberLastDisplayStyle;
        set => SetProperty(ref _rememberLastDisplayStyle, value);
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
    /// Reset sidebar pane width command.
    /// </summary>
    public ICommand ResetSidebarSpacingCommand => new RelayCommand(() => _ = ResetSidebarSpacingAsync());

    /// <summary>
    /// Reset file list column widths command.
    /// </summary>
    public ICommand ResetFileColumnWidthsCommand => new RelayCommand(() => _ = ResetFileColumnWidthsAsync());

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
    public string RememberLastDisplayStyleLabel => T("preferences.rememberLastDisplayStyle");
    public string EnvironmentSectionTitle => T("preferences.environment");
    public string ConnectionSectionTitle => T("preferences.connection");
    public string ResetOptionsSectionTitle => T("preferences.resetOptions");
    public string ResetSidebarSpacingText => T("preferences.resetSidebarSpacing");
    public string ResetFileColumnWidthsText => T("preferences.resetFileColumnWidths");
    public string AdbPathLabel => T("preferences.adbPath");
    public string SuperuserLabel => T("preferences.superuser");
    public string AdbPathWatermark => "/usr/local/bin/adb";
    public string CancelText => T("dialog.common.cancel");
    public string SaveText => T("preferences.save");

    /// <summary>
    /// Raised when settings were saved and caller should close this window.
    /// </summary>
    public event EventHandler? RequestClose;

    /// <summary>
    /// Raised when reset actions should be applied in parent window immediately.
    /// </summary>
    public event EventHandler<PreferencesLiveApplyEventArgs>? LiveApplyRequested;

    private async Task SaveAsync()
    {
        settingsService.Current.Language = SelectedLanguage.Code;
        settingsService.Current.AdbPath = AdbPath.Trim();
        settingsService.Current.Superuser = Superuser;
        settingsService.Current.ShowHiddenFiles = ShowHiddenFiles;
        settingsService.Current.FoldersFirst = FoldersFirst;
        settingsService.Current.RememberLastDisplayStyle = RememberLastDisplayStyle;

        if (!RememberLastDisplayStyle)
        {
            settingsService.Current.LastFileViewMode = "list";
        }

        await settingsService.SaveAsync();
        await localizationService.SetLanguageAsync(SelectedLanguage.Code);

        StatusMessage = T("status.preferencesSaved");
        RequestClose?.Invoke(this, EventArgs.Empty);
    }

    private string T(string key) => localizationService.GetString(key);

    private async Task ResetSidebarSpacingAsync()
    {
        settingsService.Current.DevicePaneWidth = DefaultDevicePaneWidth;
        await settingsService.SaveAsync();
        LiveApplyRequested?.Invoke(this, new PreferencesLiveApplyEventArgs(resetSidebarSpacing: true, resetFileColumnWidths: false));
    }

    private async Task ResetFileColumnWidthsAsync()
    {
        settingsService.Current.FileColumnWidthName = DefaultFileColumnWidthName;
        settingsService.Current.FileColumnWidthSize = DefaultFileColumnWidthSize;
        settingsService.Current.FileColumnWidthModified = DefaultFileColumnWidthModified;
        settingsService.Current.FileColumnWidthPermission = DefaultFileColumnWidthPermission;
        await settingsService.SaveAsync();
        LiveApplyRequested?.Invoke(this, new PreferencesLiveApplyEventArgs(resetSidebarSpacing: false, resetFileColumnWidths: true));
    }
}

/// <summary>
/// User-friendly language option.
/// </summary>
public sealed record LanguageOption(string Code, string DisplayName)
{
    /// <inheritdoc />
    public override string ToString() => DisplayName;
}

/// <summary>
/// Indicates which live reset operation should be applied by parent window.
/// </summary>
public sealed class PreferencesLiveApplyEventArgs(bool resetSidebarSpacing, bool resetFileColumnWidths) : EventArgs
{
    /// <summary>
    /// Whether left sidebar spacing should be reset immediately.
    /// </summary>
    public bool ResetSidebarSpacing { get; } = resetSidebarSpacing;

    /// <summary>
    /// Whether file list column widths should be reset immediately.
    /// </summary>
    public bool ResetFileColumnWidths { get; } = resetFileColumnWidths;
}