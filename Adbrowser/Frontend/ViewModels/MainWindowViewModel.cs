// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Collections.ObjectModel;
using System.Windows.Input;
using Adbrowser.Backend.Adb;
using Adbrowser.Backend.Adb.Models;
using Adbrowser.Backend.Fs;
using Adbrowser.Backend.Fs.Models;
using Adbrowser.Backend.Logging;
using Adbrowser.Frontend.Configuration;
using Adbrowser.Frontend.Infrastructure;
using Adbrowser.Frontend.Localization;
using Adbrowser.Shared;

namespace Adbrowser.Frontend.ViewModels;

/// <summary>
/// Main window view model for device and file browser.
/// </summary>
public sealed class MainWindowViewModel : ViewModelBase
{
    private readonly IAdbClient _adbClient;
    private readonly string _buildVersionText;
    private readonly IFileSystemService _fileSystemService;
    private readonly ILocalizationService _localizationService;
    private readonly ILogService _logService;
    private readonly RelayCommand _navigateBackCommand;
    private readonly RelayCommand _navigateForwardCommand;
    private readonly RelayCommand _navigateUpCommand;

    private readonly List<string> _navigationHistory = ["/"];
    private readonly IAppSettingsService _settingsService;
    private ClipboardEntrySnapshot? _clipboardEntry;
    private int _navigationIndex;
    private string _pathInput = "/";
    private string _searchKeyword;
    private SelectionOption _selectedSortMode;
    private SelectionOption _selectedViewMode;
    private bool _suppressViewModePersistence;

    /// <summary>
    /// Initializes localized option collections.
    /// </summary>
    public MainWindowViewModel(IAdbClient adbClient, IFileSystemService fileSystemService, ILogService logService,
        IAppSettingsService settingsService, ILocalizationService localizationService)
    {
        _adbClient = adbClient;
        _fileSystemService = fileSystemService;
        _logService = logService;
        _settingsService = settingsService;
        _localizationService = localizationService;

        _searchKeyword = string.Empty;
        ViewModes = [];
        SortModes = [];
        RebuildLocalizedOptions();

        _selectedViewMode = ViewModes[0];
        _selectedSortMode = SortModes[0];
        ApplyDisplayStylePreference();
        _buildVersionText = BuildVersionUtils.GetBuildVersionText();

        _navigateUpCommand = new RelayCommand(async () => await NavigateUpAsync(), () => CurrentPath != "/");
        _navigateBackCommand = new RelayCommand(async () => await NavigateBackAsync(), () => _navigationIndex > 0);
        _navigateForwardCommand = new RelayCommand(async () => await NavigateForwardAsync(), () => _navigationIndex < _navigationHistory.Count - 1);
    }

    /// <summary>
    /// Devices shown in the left sidebar.
    /// </summary>
    public ObservableCollection<AndroidDeviceItem> Devices { get; } = [];

    /// <summary>
    /// Current file entries for selected path.
    /// </summary>
    public ObservableCollection<DeviceFileItem> CurrentEntries { get; } = [];

    /// <summary>
    /// View mode options.
    /// </summary>
    public ObservableCollection<SelectionOption> ViewModes { get; }

    /// <summary>
    /// Sort mode options.
    /// </summary>
    public ObservableCollection<SelectionOption> SortModes { get; }

    /// <summary>
    /// Selected device.
    /// </summary>
    public AndroidDeviceItem? SelectedDevice
    {
        get;
        set
        {
            if (!SetProperty(ref field, value) || value is null) return;
            _settingsService.Current.LastDeviceSerial = value.Serial;
            _ = RefreshEntriesAsync();
        }
    }

    /// <summary>
    /// Selected file entry.
    /// </summary>
    public DeviceFileItem? SelectedEntry
    {
        get;
        set => SetProperty(ref field, value);
    }

    /// <summary>
    /// Current device path.
    /// </summary>
    public string CurrentPath
    {
        get;
        private set
        {
            if (SetProperty(ref field, value))
            {
                PathBreadcrumbSegments = BuildPathBreadcrumbSegments(value);

                if (_pathInput != value)
                {
                    PathInput = value;
                }
            }

            _navigateUpCommand.RaiseCanExecuteChanged();
        }
    } = "/";

    /// <summary>
    /// Breadcrumb segments rendered in bottom path bar.
    /// </summary>
    public IReadOnlyList<PathBreadcrumbItem> PathBreadcrumbSegments
    {
        get;
        private set => SetProperty(ref field, value);
    } = [];

    /// <summary>
    /// Path text shown in the top path input box.
    /// </summary>
    public string PathInput
    {
        get => _pathInput;
        set => SetProperty(ref _pathInput, value);
    }

    /// <summary>
    /// Current status text.
    /// </summary>
    public string StatusMessage
    {
        get;
        set => SetProperty(ref field, value);
    } = string.Empty;

    /// <summary>
    /// Indicates ongoing loading status.
    /// </summary>
    public bool IsBusy
    {
        get;
        private set => SetProperty(ref field, value);
    }

    /// <summary>
    /// Current search keyword.
    /// </summary>
    public string SearchKeyword
    {
        get => _searchKeyword;
        set => SetProperty(ref _searchKeyword, value);
    }

    /// <summary>
    /// Current view mode key.
    /// </summary>
    public SelectionOption SelectedViewMode
    {
        get => _selectedViewMode;
        set
        {
            if (!SetProperty(ref _selectedViewMode, value))
            {
                return;
            }

            OnPropertyChanged(nameof(IsListViewMode));
            OnPropertyChanged(nameof(IsIconViewMode));

            if (!_suppressViewModePersistence && _settingsService.Current.RememberLastDisplayStyle)
            {
                _settingsService.Current.LastFileViewMode = value.Key;
                _ = _settingsService.SaveAsync();
            }
        }
    }

    /// <summary>
    /// Whether file pane is showing list view.
    /// </summary>
    public bool IsListViewMode => SelectedViewMode.Key == "list";

    /// <summary>
    /// Whether file pane is showing icon grid view.
    /// </summary>
    public bool IsIconViewMode => SelectedViewMode.Key == "icons";

    /// <summary>
    /// Current sort mode key.
    /// </summary>
    public SelectionOption SelectedSortMode
    {
        get => _selectedSortMode;
        set
        {
            if (SetProperty(ref _selectedSortMode, value))
            {
                ApplySort();
            }
        }
    }

    /// <summary>
    /// Window title.
    /// </summary>
    public string Title => T("main.title");

    /// <summary>
    /// Sidebar title for devices.
    /// </summary>
    public string DevicesTitle => T("main.devicesTitle");

    public string SearchKeywordWatermark => T("main.searchKeyword");
    public string SearchButtonText => T("main.search");
    public string HeaderName => T("main.header.name");
    public string HeaderSize => T("main.header.size");
    public string HeaderModified => T("main.header.modified");
    public string HeaderPermission => T("main.header.permission");

    /// <summary>
    /// Center hint text shown over file list when directory is empty or load fails.
    /// </summary>
    public string FileListHintMessage
    {
        get;
        private set
        {
            if (SetProperty(ref field, value))
            {
                OnPropertyChanged(nameof(IsFileListHintVisible));
            }
        }
    } = string.Empty;

    /// <summary>
    /// Whether file list hint overlay is visible.
    /// </summary>
    public bool IsFileListHintVisible => !string.IsNullOrWhiteSpace(FileListHintMessage);

    public string MenuAbout => T("menu.about");
    public string MenuFile => T("menu.file");
    public string MenuEdit => T("menu.edit");
    public string MenuView => T("menu.view");
    public string MenuGo => T("menu.go");
    public string MenuHelp => T("menu.help");

    public string MenuPreferences => T("menu.preferences");
    public string MenuNewFolder => T("menu.newFolder");
    public string MenuRename => T("menu.rename");
    public string MenuDelete => T("menu.delete");
    public string MenuProperties => T("menu.properties");
    public string MenuExit => T("menu.exit");
    public string MenuCut => T("menu.cut");
    public string MenuCopy => T("menu.copy");
    public string MenuPaste => T("menu.paste");
    public string MenuSelectAll => T("menu.selectAll");
    public string MenuInverseSelect => T("menu.inverseSelect");
    public string MenuRefresh => T("menu.refresh");
    public string MenuViewMode => T("menu.viewMode");
    public string MenuSortMode => T("menu.sortMode");

    public string MenuToggleStatusBarDynamic => IsStatusBarVisible
        ? T("menu.hideStatusBar")
        : T("menu.showStatusBar");

    public string MenuAdbLogs => T("menu.adbLogs");
    public string MenuAppLogs => T("menu.appLogs");
    public string MenuForward => T("menu.forward");
    public string MenuBack => T("menu.back");
    public string MenuUp => T("menu.up");
    public string MenuRoot => T("menu.root");
    public string MenuHome => T("menu.home");
    public string MenuSearch => T("menu.search");
    public string MenuComingSoon => T("menu.comingSoon");

    /// <summary>
    /// Whether status bar is visible.
    /// </summary>
    public bool IsStatusBarVisible
    {
        get;
        private set
        {
            if (SetProperty(ref field, value))
            {
                OnPropertyChanged(nameof(MenuToggleStatusBarDynamic));
            }
        }
    } = true;

    /// <summary>
    /// Commit id text shown in status bar.
    /// </summary>
    public string CommitIdText
    {
        get => _buildVersionText;
    }

    /// <summary>
    /// Refreshes device list command.
    /// </summary>
    public ICommand RefreshDevicesCommand => new RelayCommand(async () => await RefreshDevicesAsync());

    /// <summary>
    /// Refreshes current directory command.
    /// </summary>
    public ICommand RefreshEntriesCommand => new RelayCommand(async () => await RefreshEntriesAsync());

    /// <summary>
    /// Navigates to parent directory.
    /// </summary>
    public ICommand NavigateUpCommand => _navigateUpCommand;

    /// <summary>
    /// Navigates to root directory.
    /// </summary>
    public ICommand NavigateRootCommand => new RelayCommand(async () => await NavigateToAsync("/"));

    /// <summary>
    /// Navigates to device home directory.
    /// </summary>
    public ICommand NavigateHomeCommand => new RelayCommand(async () => await NavigateToHomeAsync());

    /// <summary>
    /// Navigates backward in history.
    /// </summary>
    public ICommand NavigateBackCommand => _navigateBackCommand;

    /// <summary>
    /// Navigates forward in history.
    /// </summary>
    public ICommand NavigateForwardCommand => _navigateForwardCommand;

    /// <summary>
    /// Searches files in current path.
    /// </summary>
    public ICommand SearchCommand => new RelayCommand(async () => await SearchAsync(), () => !string.IsNullOrWhiteSpace(SearchKeyword));

    /// <summary>
    /// Opens selected folder.
    /// </summary>
    public ICommand OpenEntryCommand => new RelayCommand(async () => await OpenSelectedEntryAsync());

    /// <summary>
    /// Toggles bottom status bar visibility.
    /// </summary>
    public ICommand ToggleStatusBarCommand => new RelayCommand(() => IsStatusBarVisible = !IsStatusBarVisible);

    /// <summary>
    /// Called by view after startup to load initial data.
    /// </summary>
    public async Task InitializeAsync()
    {
        PathInput = CurrentPath;
        await RefreshDevicesAsync();
    }

    /// <summary>
    /// Navigates to the path provided from the path input box.
    /// </summary>
    public async Task NavigateToPathInputAsync()
    {
        if (SelectedDevice is null)
        {
            StatusMessage = T("status.selectDeviceFirst");
            return;
        }

        await NavigateToAsync(PathInput);
    }

    /// <summary>
    /// Navigates to selected breadcrumb path.
    /// </summary>
    public async Task NavigateToBreadcrumbAsync(string? targetPath)
    {
        if (string.IsNullOrWhiteSpace(targetPath))
        {
            return;
        }

        var normalized = NormalizePath(targetPath);
        if (normalized == CurrentPath)
        {
            return;
        }

        await NavigateToAsync(normalized);
    }

    /// <summary>
    /// Creates a new folder under current path and refreshes file list.
    /// </summary>
    public async Task<bool> CreateFolderAsync(string folderName)
    {
        if (SelectedDevice is null)
        {
            StatusMessage = T("status.selectDeviceFirst");
            return false;
        }

        var name = folderName.Trim();

        if (string.IsNullOrWhiteSpace(name))
        {
            StatusMessage = T("status.invalidFolderName");
            return false;
        }

        var result = await _fileSystemService.CreateFolderAsync(SelectedDevice.Serial, CurrentPath, name);

        if (!result.IsSuccess)
        {
            StatusMessage = result.ErrorMessage ?? T("common.unknownError");
            return false;
        }

        StatusMessage = string.Format(T("status.folderCreated"), name);
        await RefreshEntriesAsync();
        return true;
    }

    /// <summary>
    /// Deletes selected entry and refreshes current path when succeeded.
    /// </summary>
    public async Task<bool> DeleteSelectedAsync()
    {
        if (SelectedDevice is null || SelectedEntry is null)
        {
            StatusMessage = T("status.selectEntryFirst");
            return false;
        }

        var fullPath = GetEntryFullPath(SelectedEntry);
        var result = await _fileSystemService.DeleteAsync(SelectedDevice.Serial, fullPath);

        if (!result.IsSuccess)
        {
            StatusMessage = result.ErrorMessage ?? T("common.unknownError");
            return false;
        }

        StatusMessage = string.Format(T("status.entryDeleted"), SelectedEntry.Name);
        await RefreshEntriesAsync();
        return true;
    }

    /// <summary>
    /// Builds selected entry details for property dialog.
    /// </summary>
    public FileEntrySnapshot? GetSelectedEntrySnapshot()
    {
        if (SelectedDevice is null || SelectedEntry is null)
        {
            return null;
        }

        return new FileEntrySnapshot(
            SelectedDevice.Serial,
            SelectedEntry.Name,
            GetEntryFullPath(SelectedEntry),
            SelectedEntry.IsDirectory,
            SelectedEntry.IsSymlink,
            SelectedEntry.Size,
            SelectedEntry.ModifiedTime,
            SelectedEntry.Permission);
    }

    /// <summary>
    /// Returns whether an entry is currently selected.
    /// </summary>
    public bool HasSelectedEntry() => SelectedEntry is not null;

    /// <summary>
    /// Updates permission text for a single visible entry without refreshing the whole list.
    /// </summary>
    public void UpdateEntryPermission(string fullPath, string symbolicPermission)
    {
        if (string.IsNullOrWhiteSpace(fullPath) || string.IsNullOrWhiteSpace(symbolicPermission))
        {
            return;
        }

        var target = CurrentEntries.FirstOrDefault(entry => string.Equals(GetEntryFullPath(entry), fullPath, StringComparison.Ordinal));
        if (target is null)
        {
            return;
        }

        target.Permission = symbolicPermission;
    }

    /// <summary>
    /// Stores selected entry in clipboard as copy operation.
    /// </summary>
    public bool CopySelected()
    {
        if (SelectedDevice is null || SelectedEntry is null)
        {
            StatusMessage = T("status.selectEntryFirst");
            return false;
        }

        _clipboardEntry = BuildClipboardEntry(isCut: false);
        StatusMessage = string.Format(T("status.copied"), SelectedEntry.Name);
        return true;
    }

    /// <summary>
    /// Stores selected entry in clipboard as cut operation.
    /// </summary>
    public bool CutSelected()
    {
        if (SelectedDevice is null || SelectedEntry is null)
        {
            StatusMessage = T("status.selectEntryFirst");
            return false;
        }

        _clipboardEntry = BuildClipboardEntry(isCut: true);
        StatusMessage = string.Format(T("status.cut"), SelectedEntry.Name);
        return true;
    }

    /// <summary>
    /// Returns whether clipboard has content for paste.
    /// </summary>
    public bool CanPaste() => _clipboardEntry is not null;

    /// <summary>
    /// Pastes clipboard entry to current path.
    /// </summary>
    public async Task<bool> PasteAsync()
    {
        if (SelectedDevice is null)
        {
            StatusMessage = T("status.selectDeviceFirst");
            return false;
        }

        if (_clipboardEntry is null)
        {
            StatusMessage = T("status.clipboardEmpty");
            return false;
        }

        if (_clipboardEntry.Serial != SelectedDevice.Serial)
        {
            StatusMessage = T("status.crossDevicePasteNotSupported");
            return false;
        }

        var targetPath = CurrentPath == "/"
            ? $"/{_clipboardEntry.Name}"
            : $"{CurrentPath.TrimEnd('/')}/{_clipboardEntry.Name}";

        var result = _clipboardEntry.IsCut
            ? await _fileSystemService.MoveAsync(SelectedDevice.Serial, _clipboardEntry.FullPath, targetPath)
            : await _fileSystemService.CopyAsync(SelectedDevice.Serial, _clipboardEntry.FullPath, targetPath);

        if (!result.IsSuccess)
        {
            StatusMessage = result.ErrorMessage ?? T("common.unknownError");
            return false;
        }

        StatusMessage = string.Format(T("status.pasted"), _clipboardEntry.Name);

        if (_clipboardEntry.IsCut)
        {
            _clipboardEntry = null;
        }

        await RefreshEntriesAsync();
        return true;
    }

    /// <summary>
    /// Renames selected entry to new name.
    /// </summary>
    public async Task<bool> RenameSelectedAsync(string newName)
    {
        if (SelectedDevice is null || SelectedEntry is null)
        {
            StatusMessage = T("status.selectEntryFirst");
            return false;
        }

        var targetName = newName.Trim();
        if (string.IsNullOrWhiteSpace(targetName))
        {
            StatusMessage = T("status.invalidName");
            return false;
        }

        var sourcePath = GetEntryFullPath(SelectedEntry);
        var result = await _fileSystemService.RenameAsync(SelectedDevice.Serial, sourcePath, targetName);

        if (!result.IsSuccess)
        {
            StatusMessage = result.ErrorMessage ?? T("common.unknownError");
            return false;
        }

        StatusMessage = string.Format(T("status.renamed"), SelectedEntry.Name, targetName);
        await RefreshEntriesAsync();
        return true;
    }

    private async Task RefreshDevicesAsync()
    {
        IsBusy = true;

        try
        {
            var devices = await _adbClient.ListDevicesAsync();
            Devices.Clear();

            foreach (var item in devices)
            {
                Devices.Add(new AndroidDeviceItem(item));
            }

            SelectDefaultDevice();
            StatusMessage = T("status.devicesUpdated");
        }
        catch (Exception ex)
        {
            _logService.Log(LogLevel.Error, "UI", ex.Message);
            StatusMessage = ex.Message;
        }
        finally
        {
            IsBusy = false;
        }
    }

    private void SelectDefaultDevice()
    {
        var remembered = _settingsService.Current.LastDeviceSerial;

        if (_settingsService.Current.RememberLastDevice && !string.IsNullOrWhiteSpace(remembered))
        {
            SelectedDevice = Devices.FirstOrDefault(d => d.Serial == remembered) ?? Devices.FirstOrDefault();
        }
        else
        {
            SelectedDevice = Devices.FirstOrDefault();
        }
    }

    private async Task<bool> RefreshEntriesAsync(string? requestedPath = null)
    {
        if (SelectedDevice is null)
        {
            CurrentEntries.Clear();
            FileListHintMessage = string.Empty;
            return false;
        }

        IsBusy = true;
        var serial = SelectedDevice.Serial;
        var targetPath = requestedPath;

        try
        {
            if (string.IsNullOrWhiteSpace(targetPath)
                && _settingsService.Current.RememberDevicePath
                && _settingsService.Current.DeviceLastPaths.TryGetValue(serial, out var rememberedPath)
                && !string.IsNullOrWhiteSpace(rememberedPath))
            {
                targetPath = rememberedPath;
            }

            targetPath ??= CurrentPath;

            var entries = await _fileSystemService.ListAsync(serial, targetPath);

            CurrentPath = targetPath;
            FillEntries(entries);
            FileListHintMessage = entries.Count == 0
                ? T("main.fileListHint.emptyFolder")
                : string.Empty;
            return true;
        }
        catch (Exception ex)
        {
            _logService.Log(LogLevel.Error, "UI", ex.Message);
            CurrentPath = targetPath ?? CurrentPath;
            FillEntries([]);
            FileListHintMessage = BuildListLoadFailureHint(ex.Message);
            StatusMessage = ex.Message;
            return true;
        }
        finally
        {
            IsBusy = false;
        }
    }

    private async Task NavigateToAsync(string path)
    {
        if (SelectedDevice is null)
        {
            return;
        }

        var targetPath = NormalizePath(path);
        if (!await RefreshEntriesAsync(targetPath))
        {
            return;
        }

        PushHistory(CurrentPath);
        PersistCurrentPath();
    }

    private async Task NavigateUpAsync()
    {
        if (CurrentPath == "/")
        {
            return;
        }

        var index = CurrentPath.LastIndexOf('/');
        var parent = index <= 0 ? "/" : CurrentPath[..index];
        await NavigateToAsync(parent);
    }

    private async Task NavigateToHomeAsync()
    {
        if (SelectedDevice is null)
        {
            return;
        }

        if (_settingsService.Current.DeviceHomePaths.TryGetValue(SelectedDevice.Serial, out var home) && !string.IsNullOrWhiteSpace(home))
        {
            await NavigateToAsync(home);
            return;
        }

        await NavigateToAsync("/");
    }

    private async Task NavigateBackAsync()
    {
        if (_navigationIndex <= 0)
        {
            return;
        }

        var targetIndex = _navigationIndex - 1;
        var targetPath = _navigationHistory[targetIndex];

        if (!await RefreshEntriesAsync(targetPath))
        {
            return;
        }

        _navigationIndex = targetIndex;
        UpdateNavigationCommandStates();
    }

    private async Task NavigateForwardAsync()
    {
        if (_navigationIndex >= _navigationHistory.Count - 1)
        {
            return;
        }

        var targetIndex = _navigationIndex + 1;
        var targetPath = _navigationHistory[targetIndex];

        if (!await RefreshEntriesAsync(targetPath))
        {
            return;
        }

        _navigationIndex = targetIndex;
        UpdateNavigationCommandStates();
    }

    private async Task SearchAsync()
    {
        if (SelectedDevice is null)
        {
            return;
        }

        IsBusy = true;

        try
        {
            var entries = await _fileSystemService.SearchAsync(SelectedDevice.Serial, CurrentPath, SearchKeyword.Trim());
            FillEntries(entries);
            FileListHintMessage = string.Empty;
            StatusMessage = string.Format(T("status.searchResult"), entries.Count, SearchKeyword);
        }
        catch (Exception ex)
        {
            _logService.Log(LogLevel.Error, "UI", ex.Message);
            StatusMessage = ex.Message;
        }
        finally
        {
            IsBusy = false;
        }
    }

    private async Task OpenSelectedEntryAsync()
    {
        if (SelectedEntry is null || !SelectedEntry.IsDirectory)
        {
            return;
        }

        var next = CurrentPath == "/" ? $"/{SelectedEntry.Name}" : $"{CurrentPath.TrimEnd('/')}/{SelectedEntry.Name}";
        await NavigateToAsync(next);
    }

    private void FillEntries(IReadOnlyList<DeviceFileEntry> entries)
    {
        CurrentEntries.Clear();
        var showHiddenFiles = _settingsService.Current.ShowHiddenFiles;

        foreach (var item in entries)
        {
            if (!showHiddenFiles && IsHiddenEntryName(item.Name))
            {
                continue;
            }

            CurrentEntries.Add(new DeviceFileItem(item));
        }

        ApplySort();
    }

    private void ApplySort()
    {
        var sortModeKey = SelectedSortMode.Key;
        var foldersFirst = _settingsService.Current.FoldersFirst;
        var sorted = foldersFirst
            ? sortModeKey switch
            {
                "size" => CurrentEntries.OrderByDescending(e => e.IsDirectory).ThenByDescending(e => e.Size).ThenBy(e => e.Name),
                "modified" => CurrentEntries.OrderByDescending(e => e.IsDirectory).ThenByDescending(e => e.ModifiedTime).ThenBy(e => e.Name),
                _ => CurrentEntries.OrderByDescending(e => e.IsDirectory).ThenBy(e => e.Name)
            }
            : sortModeKey switch
            {
                "size" => CurrentEntries.OrderByDescending(e => e.Size).ThenBy(e => e.Name),
                "modified" => CurrentEntries.OrderByDescending(e => e.ModifiedTime).ThenBy(e => e.Name),
                _ => CurrentEntries.OrderBy(e => e.Name)
            };

        var values = sorted.ToArray();
        CurrentEntries.Clear();

        foreach (var item in values)
        {
            CurrentEntries.Add(item);
        }
    }

    /// <summary>
    /// Reloads current path and re-applies file list options.
    /// </summary>
    public async Task RefreshCurrentEntriesAsync()
    {
        await RefreshEntriesAsync(CurrentPath);
    }

    /// <summary>
    /// Applies remembered file display style from current settings.
    /// </summary>
    public void ApplyDisplayStylePreference()
    {
        var targetKey = _settingsService.Current.RememberLastDisplayStyle
            ? _settingsService.Current.LastFileViewMode
            : "list";

        var option = ViewModes.FirstOrDefault(e => e.Key == targetKey) ?? ViewModes[0];
        _suppressViewModePersistence = true;
        try
        {
            SelectedViewMode = option;
        }
        finally
        {
            _suppressViewModePersistence = false;
        }
    }

    /// <summary>
    /// Forces refresh for all localized UI texts in this view model.
    /// </summary>
    public async Task RefreshLocalizationAsync()
    {
        RebuildLocalizedOptions();

        OnPropertyChanged(nameof(Title));
        OnPropertyChanged(nameof(DevicesTitle));
        OnPropertyChanged(nameof(SearchKeywordWatermark));
        OnPropertyChanged(nameof(SearchButtonText));
        OnPropertyChanged(nameof(HeaderName));
        OnPropertyChanged(nameof(HeaderSize));
        OnPropertyChanged(nameof(HeaderModified));
        OnPropertyChanged(nameof(HeaderPermission));

        OnPropertyChanged(nameof(MenuAbout));
        OnPropertyChanged(nameof(MenuFile));
        OnPropertyChanged(nameof(MenuEdit));
        OnPropertyChanged(nameof(MenuView));
        OnPropertyChanged(nameof(MenuGo));
        OnPropertyChanged(nameof(MenuHelp));
        OnPropertyChanged(nameof(MenuPreferences));
        OnPropertyChanged(nameof(MenuNewFolder));
        OnPropertyChanged(nameof(MenuRename));
        OnPropertyChanged(nameof(MenuDelete));
        OnPropertyChanged(nameof(MenuProperties));
        OnPropertyChanged(nameof(MenuExit));
        OnPropertyChanged(nameof(MenuCut));
        OnPropertyChanged(nameof(MenuCopy));
        OnPropertyChanged(nameof(MenuPaste));
        OnPropertyChanged(nameof(MenuSelectAll));
        OnPropertyChanged(nameof(MenuInverseSelect));
        OnPropertyChanged(nameof(MenuRefresh));
        OnPropertyChanged(nameof(MenuViewMode));
        OnPropertyChanged(nameof(MenuSortMode));
        OnPropertyChanged(nameof(MenuToggleStatusBarDynamic));
        OnPropertyChanged(nameof(MenuAdbLogs));
        OnPropertyChanged(nameof(MenuAppLogs));
        OnPropertyChanged(nameof(MenuForward));
        OnPropertyChanged(nameof(MenuBack));
        OnPropertyChanged(nameof(MenuUp));
        OnPropertyChanged(nameof(MenuRoot));
        OnPropertyChanged(nameof(MenuHome));
        OnPropertyChanged(nameof(MenuSearch));
        OnPropertyChanged(nameof(MenuComingSoon));

        await RefreshEntriesAsync(CurrentPath);
    }

    private void PushHistory(string path)
    {
        if (_navigationIndex < _navigationHistory.Count - 1)
        {
            _navigationHistory.RemoveRange(_navigationIndex + 1, _navigationHistory.Count - _navigationIndex - 1);
        }

        if (_navigationHistory.Count > 0 && _navigationHistory[^1] == path)
        {
            return;
        }

        _navigationHistory.Add(path);
        _navigationIndex = _navigationHistory.Count - 1;
        UpdateNavigationCommandStates();
    }

    private void UpdateNavigationCommandStates()
    {
        _navigateBackCommand.RaiseCanExecuteChanged();
        _navigateForwardCommand.RaiseCanExecuteChanged();
    }

    private void PersistCurrentPath()
    {
        if (SelectedDevice is null || !_settingsService.Current.RememberDevicePath)
        {
            return;
        }

        _settingsService.Current.DeviceLastPaths[SelectedDevice.Serial] = CurrentPath;
        _ = _settingsService.SaveAsync();
    }

    private string GetEntryFullPath(DeviceFileItem entry)
    {
        return CurrentPath == "/"
            ? $"/{entry.Name}"
            : $"{CurrentPath.TrimEnd('/')}/{entry.Name}";
    }

    private ClipboardEntrySnapshot BuildClipboardEntry(bool isCut)
    {
        var entry = SelectedEntry!;
        var serial = SelectedDevice!.Serial;
        return new ClipboardEntrySnapshot(serial, entry.Name, GetEntryFullPath(entry), isCut);
    }

    private static string NormalizePath(string path)
    {
        if (string.IsNullOrWhiteSpace(path))
        {
            return "/";
        }

        var normalized = path.Trim();

        if (!normalized.StartsWith('/'))
        {
            normalized = $"/{normalized}";
        }

        return normalized.Replace("//", "/");
    }

    private static IReadOnlyList<PathBreadcrumbItem> BuildPathBreadcrumbSegments(string path)
    {
        if (string.IsNullOrWhiteSpace(path) || path == "/")
        {
            return [];
        }

        var segments = path.Split('/', StringSplitOptions.RemoveEmptyEntries);
        if (segments.Length == 0)
        {
            return [];
        }

        var result = new List<PathBreadcrumbItem>();

        var current = string.Empty;

        for (var i = 0; i < segments.Length; i++)
        {
            var segment = segments[i];
            current = $"{current}/{segment}";
            result.Add(new PathBreadcrumbItem(segment, current, false, i > 0));
        }

        return result;
    }

    private string T(string key) => _localizationService.GetString(key);

    private void RebuildLocalizedOptions()
    {
        var selectedViewKey = _selectedViewMode?.Key ?? "list";
        var selectedSortKey = _selectedSortMode?.Key ?? "name";

        var viewModes = new[]
        {
            new SelectionOption("list", T("main.viewMode.list")),
            new SelectionOption("icons", T("main.viewMode.icons"))
        };

        var sortModes = new[]
        {
            new SelectionOption("name", T("main.sortMode.name")),
            new SelectionOption("size", T("main.sortMode.size")),
            new SelectionOption("modified", T("main.sortMode.modified"))
        };

        ViewModes.Clear();
        foreach (var mode in viewModes)
        {
            ViewModes.Add(mode);
        }

        SortModes.Clear();
        foreach (var mode in sortModes)
        {
            SortModes.Add(mode);
        }

        _selectedViewMode = ViewModes.FirstOrDefault(e => e.Key == selectedViewKey) ?? ViewModes[0];
        _selectedSortMode = SortModes.FirstOrDefault(e => e.Key == selectedSortKey) ?? SortModes[0];
        OnPropertyChanged(nameof(SelectedViewMode));
        OnPropertyChanged(nameof(SelectedSortMode));
        OnPropertyChanged(nameof(IsListViewMode));
        OnPropertyChanged(nameof(IsIconViewMode));
    }

    private static bool IsHiddenEntryName(string name)
    {
        return !string.IsNullOrWhiteSpace(name) && name.StartsWith(".", StringComparison.Ordinal);
    }

    private string BuildListLoadFailureHint(string errorMessage)
    {
        if (IsPathNotFoundError(errorMessage))
        {
            return T("main.fileListHint.pathNotFound");
        }

        if (IsPermissionDeniedError(errorMessage))
        {
            return T("main.fileListHint.permissionDenied");
        }

        return T("main.fileListHint.loadFailed");
    }

    private static bool IsPathNotFoundError(string errorMessage)
    {
        if (string.IsNullOrWhiteSpace(errorMessage))
        {
            return false;
        }

        var value = errorMessage.ToLowerInvariant();
        return value.Contains("no such file or directory", StringComparison.Ordinal)
               || value.Contains("not found", StringComparison.Ordinal);
    }

    private static bool IsPermissionDeniedError(string errorMessage)
    {
        if (string.IsNullOrWhiteSpace(errorMessage))
        {
            return false;
        }

        var value = errorMessage.ToLowerInvariant();
        return value.Contains("permission denied", StringComparison.Ordinal)
               || value.Contains("operation not permitted", StringComparison.Ordinal)
               || value.Contains("not permitted", StringComparison.Ordinal);
    }
}

/// <summary>
/// Snapshot used by file properties dialog.
/// </summary>
public sealed record FileEntrySnapshot(
    string Serial,
    string Name,
    string FullPath,
    bool IsDirectory,
    bool IsSymlink,
    long Size,
    DateTimeOffset ModifiedTime,
    string SymbolicPermission);

/// <summary>
/// Clipboard snapshot used by copy/cut/paste operations.
/// </summary>
public sealed record ClipboardEntrySnapshot(
    string Serial,
    string Name,
    string FullPath,
    bool IsCut);

/// <summary>
/// Localized option item for selector controls.
/// </summary>
public sealed record SelectionOption(string Key, string Display)
{
    /// <inheritdoc />
    public override string ToString() => Display;
}

/// <summary>
/// Path breadcrumb item model.
/// </summary>
public sealed record PathBreadcrumbItem(string DisplayName, string FullPath, bool IsRoot, bool ShowLeadingArrow)
{
    /// <summary>
    /// Whether breadcrumb name should be shown.
    /// </summary>
    public bool ShowName => !IsRoot;
}

/// <summary>
/// Sidebar model for Android device item.
/// </summary>
public sealed class AndroidDeviceItem(AndroidDevice source)
{
    /// <summary>
    /// Device serial id.
    /// </summary>
    public string Serial { get; } = source.Serial;

    /// <summary>
    /// Device display name.
    /// </summary>
    public string Name { get; } = source.Name;

    /// <summary>
    /// Device model name.
    /// </summary>
    public string Model { get; } = source.Model;

    /// <summary>
    /// Whether device is online.
    /// </summary>
    public bool IsOnline { get; } = source.IsOnline;

    /// <summary>
    /// Human-readable status text.
    /// </summary>
    public string StatusText => IsOnline ? "online" : "offline";

    /// <summary>
    /// Status dot brush hex.
    /// </summary>
    public string StatusBrush => IsOnline ? "#2EA043" : "#E5534B";
}

/// <summary>
/// UI model for file list item.
/// </summary>
public sealed class DeviceFileItem : ObservableObject
{
    private string _permission;

    public DeviceFileItem(DeviceFileEntry source)
    {
        Name = source.Name;
        Path = source.Path;
        IsDirectory = source.IsDirectory;
        IsSymlink = source.IsSymlink;
        Size = source.Size;
        ModifiedTime = source.ModifiedTime;
        _permission = source.Permission;
        IconUri = FileIconResolver.Resolve(IsDirectory, IsSymlink, Name).ToString();
    }

    /// <summary>
    /// File or directory name.
    /// </summary>
    public string Name { get; }

    /// <summary>
    /// Path of this entry.
    /// </summary>
    public string Path { get; }

    /// <summary>
    /// Whether this entry is directory.
    /// </summary>
    public bool IsDirectory { get; }

    /// <summary>
    /// Whether this entry is a symbolic link.
    /// </summary>
    public bool IsSymlink { get; }

    /// <summary>
    /// Size in bytes.
    /// </summary>
    public long Size { get; }

    /// <summary>
    /// Last modified time.
    /// </summary>
    public DateTimeOffset ModifiedTime { get; }

    /// <summary>
    /// Permission string.
    /// </summary>
    public string Permission
    {
        get => _permission;
        set => SetProperty(ref _permission, value);
    }

    /// <summary>
    /// SVG icon uri.
    /// </summary>
    public string IconUri { get; }

    /// <summary>
    /// Display name.
    /// </summary>
    public string DisplayName => Name;

    /// <summary>
    /// Display text for size column.
    /// </summary>
    public string SizeText => IsDirectory ? "-" : Size.ToString("N0");

    /// <summary>
    /// Display text for mtime column.
    /// </summary>
    public string ModifiedText => ModifiedTime.ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss");
}