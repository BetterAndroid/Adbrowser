// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Collections.Specialized;
using Adbrowser.Frontend.Runtime;
using Adbrowser.Frontend.ViewModels;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Input;
using Avalonia.Interactivity;
using Avalonia.Threading;

namespace Adbrowser.Frontend.Views;

public partial class MainWindow : Window
{
    private const string FileColNameWidthKey = "FileColNameWidth";
    private const string FileColSizeWidthKey = "FileColSizeWidth";
    private const string FileColModifiedWidthKey = "FileColModifiedWidth";
    private const string FileColPermissionWidthKey = "FileColPermissionWidth";

    private static readonly (int Index, string ResourceKey)[] FileHeaderSyncedColumns =
    [
        (0, FileColNameWidthKey),
        (2, FileColSizeWidthKey),
        (4, FileColModifiedWidthKey),
        (6, FileColPermissionWidthKey)
    ];

    private readonly MainWindowViewModel _viewModel;
    private bool _pendingScrollToTop;

    public MainWindow()
    {
        InitializeComponent();

        _viewModel = new MainWindowViewModel(
            AppServices.AdbClient,
            AppServices.FileSystemService,
            AppServices.LogService,
            AppServices.SettingsService,
            AppServices.LocalizationService);

        DataContext = _viewModel;
        _viewModel.CurrentEntries.CollectionChanged += OnCurrentEntriesChanged;
        Loaded += OnLoaded;
        Closed += OnClosed;
    }

    private async void OnLoaded(object? sender, RoutedEventArgs e)
    {
        Loaded -= OnLoaded;
        RestoreMainPaneWidth();
        RestoreFileColumnWidths();
        HookFileHeaderColumnWidthSync();
        SyncFileColumnWidthResourcesFromHeader();
        await _viewModel.InitializeAsync();
    }

    private async void OnClosed(object? sender, EventArgs e)
    {
        Closed -= OnClosed;
        _viewModel.CurrentEntries.CollectionChanged -= OnCurrentEntriesChanged;
        UnhookFileHeaderColumnWidthSync();
        SaveMainPaneWidth();
        SaveFileColumnWidths();
        await AppServices.SettingsService.SaveAsync();
    }

    private void OnPreferencesClick(object? sender, RoutedEventArgs e)
    {
        var window = new PreferencesWindow(_viewModel.SelectedDevice?.Serial)
        {
            WindowStartupLocation = WindowStartupLocation.CenterOwner
        };
        window.ShowDialog(this);
    }

    private void OnLogsClick(object? sender, RoutedEventArgs e)
    {
        var window = new LogViewerWindow
        {
            WindowStartupLocation = WindowStartupLocation.CenterOwner
        };
        window.ShowDialog(this);
    }

    private void OnExitClick(object? sender, RoutedEventArgs e)
    {
        if (Application.Current?.ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            desktop.Shutdown();
        }
        else
        {
            Close();
        }
    }

    private void OnEntryDoubleTapped(object? sender, RoutedEventArgs e)
    {
        _viewModel.OpenEntryCommand.Execute(null);
    }

    private void OnFilePanePointerPressed(object? sender, PointerPressedEventArgs e)
    {
        if (e.GetCurrentPoint(this).Properties.IsRightButtonPressed)
        {
            _viewModel.SelectedEntry = null;
        }
    }

    private void OnFilePaneDrop(object? sender, DragEventArgs e)
    {
        _viewModel.StatusMessage = AppServices.LocalizationService.GetString("status.dropDetected");
    }

    private async void OnNewFolderClick(object? sender, RoutedEventArgs e)
    {
        var t = AppServices.LocalizationService.GetString;
        var dialog = new SimpleInputDialog(
            title: t("dialog.newFolder.title"),
            prompt: t("dialog.newFolder.prompt"),
            okText: t("dialog.newFolder.create"),
            cancelText: t("dialog.common.cancel"),
            validator: static text => !string.IsNullOrWhiteSpace(text));
        dialog.InvalidInputMessage = t("dialog.input.invalid");

        var result = await dialog.ShowDialog<bool?>(this);

        if (result is true)
        {
            await _viewModel.CreateFolderAsync(dialog.Value);
        }
    }

    private async void OnDeleteClick(object? sender, RoutedEventArgs e)
    {
        var t = AppServices.LocalizationService.GetString;
        if (!_viewModel.HasSelectedEntry())
        {
            _viewModel.StatusMessage = t("status.selectEntryFirst");
            return;
        }

        var dialog = new ConfirmDialog(
            title: t("dialog.delete.title"),
            message: t("dialog.delete.confirm"),
            yesText: t("dialog.delete.confirmButton"),
            noText: t("dialog.common.cancel"));
        var confirmed = await dialog.ShowDialog<bool?>(this);

        if (confirmed is true)
        {
            await _viewModel.DeleteSelectedAsync();
        }
    }

    private async void OnPropertiesClick(object? sender, RoutedEventArgs e)
    {
        var t = AppServices.LocalizationService.GetString;
        var snapshot = _viewModel.GetSelectedEntrySnapshot();

        if (snapshot is null)
        {
            _viewModel.StatusMessage = t("status.selectEntryFirst");
            return;
        }

        var window = new FilePropertiesWindow(snapshot, AppServices.PermissionService, AppServices.LogService)
        {
            WindowStartupLocation = WindowStartupLocation.CenterOwner
        };
        window.StatusChanged += message => _viewModel.StatusMessage = message;
        window.PermissionChanged += (path, permission) => _viewModel.UpdateEntryPermission(path, permission);
        await window.ShowDialog(this);
    }

    private async void OnRenameClick(object? sender, RoutedEventArgs e)
    {
        var t = AppServices.LocalizationService.GetString;
        var snapshot = _viewModel.GetSelectedEntrySnapshot();

        if (snapshot is null)
        {
            _viewModel.StatusMessage = t("status.selectEntryFirst");
            return;
        }

        var dialog = new SimpleInputDialog(
            title: t("dialog.rename.title"),
            prompt: string.Empty,
            okText: t("dialog.rename.confirm"),
            cancelText: t("dialog.common.cancel"),
            validator: static text => !string.IsNullOrWhiteSpace(text),
            initialValue: snapshot.Name,
            selectAllOnOpen: true);
        dialog.InvalidInputMessage = t("dialog.input.invalid");
        var result = await dialog.ShowDialog<bool?>(this);

        if (result is true)
        {
            await _viewModel.RenameSelectedAsync(dialog.Value);
        }
    }

    private void OnCopyClick(object? sender, RoutedEventArgs e)
    {
        _viewModel.CopySelected();
    }

    private void OnCutClick(object? sender, RoutedEventArgs e)
    {
        _viewModel.CutSelected();
    }

    private async void OnPasteClick(object? sender, RoutedEventArgs e)
    {
        await _viewModel.PasteAsync();
    }

    private async void OnPathInputKeyDown(object? sender, KeyEventArgs e)
    {
        if (e.Key is not (Key.Enter or Key.Return))
        {
            return;
        }

        e.Handled = true;
        await _viewModel.NavigateToPathInputAsync();
    }

    private async void OnPathBreadcrumbClick(object? sender, RoutedEventArgs e)
    {
        if (sender is not Button { Tag: string path })
        {
            return;
        }

        await _viewModel.NavigateToBreadcrumbAsync(path);
    }

    private void RestoreFileColumnWidths()
    {
        var settings = AppServices.SettingsService.Current;
        ApplyColumnWidth(FileHeaderGrid.ColumnDefinitions[0], settings.FileColumnWidthName, 180);
        ApplyColumnWidth(FileHeaderGrid.ColumnDefinitions[2], settings.FileColumnWidthSize, 90);
        ApplyColumnWidth(FileHeaderGrid.ColumnDefinitions[4], settings.FileColumnWidthModified, 150);
        ApplyColumnWidth(FileHeaderGrid.ColumnDefinitions[6], settings.FileColumnWidthPermission, 110);
    }

    private void RestoreMainPaneWidth()
    {
        var settings = AppServices.SettingsService.Current;
        ApplyColumnWidth(MainLayoutGrid.ColumnDefinitions[0], settings.DevicePaneWidth, 240);
    }

    private void SaveMainPaneWidth()
    {
        var settings = AppServices.SettingsService.Current;
        settings.DevicePaneWidth = MainLayoutGrid.ColumnDefinitions[0].Width.Value;
    }

    private void SaveFileColumnWidths()
    {
        var settings = AppServices.SettingsService.Current;
        settings.FileColumnWidthName = FileHeaderGrid.ColumnDefinitions[0].Width.Value;
        settings.FileColumnWidthSize = FileHeaderGrid.ColumnDefinitions[2].Width.Value;
        settings.FileColumnWidthModified = FileHeaderGrid.ColumnDefinitions[4].Width.Value;
        settings.FileColumnWidthPermission = FileHeaderGrid.ColumnDefinitions[6].Width.Value;
    }

    private static void ApplyColumnWidth(ColumnDefinition column, double width, double minWidth)
    {
        var value = double.IsFinite(width) && width > 0 ? width : minWidth;
        if (value < minWidth)
        {
            value = minWidth;
        }

        column.Width = new GridLength(value, GridUnitType.Pixel);
    }

    private void HookFileHeaderColumnWidthSync()
    {
        foreach (var (column, _) in GetValidSyncedHeaderColumns())
        {
            column.PropertyChanged += OnFileHeaderColumnPropertyChanged;
        }
    }

    private void UnhookFileHeaderColumnWidthSync()
    {
        foreach (var (column, _) in GetValidSyncedHeaderColumns())
        {
            column.PropertyChanged -= OnFileHeaderColumnPropertyChanged;
        }
    }

    private void OnFileHeaderColumnPropertyChanged(object? sender, AvaloniaPropertyChangedEventArgs e)
    {
        if (e.Property == ColumnDefinition.WidthProperty)
        {
            SyncFileColumnWidthResourcesFromHeader();
        }
    }

    private void SyncFileColumnWidthResourcesFromHeader()
    {
        foreach (var (column, resourceKey) in GetValidSyncedHeaderColumns())
        {
            UpdateWidthResource(resourceKey, column);
        }
    }

    private IEnumerable<(ColumnDefinition Column, string ResourceKey)> GetValidSyncedHeaderColumns()
    {
        var columnDefinitions = FileHeaderGrid.ColumnDefinitions;
        foreach (var (index, resourceKey) in FileHeaderSyncedColumns)
        {
            if (index < 0 || index >= columnDefinitions.Count)
            {
                continue;
            }

            yield return (columnDefinitions[index], resourceKey);
        }
    }

    private void UpdateWidthResource(string key, ColumnDefinition column)
    {
        var width = column.Width.Value;
        if (!double.IsFinite(width) || width <= 0)
        {
            return;
        }

        Resources[key] = new GridLength(width, GridUnitType.Pixel);
    }

    private void OnCurrentEntriesChanged(object? sender, NotifyCollectionChangedEventArgs e)
    {
        if (_pendingScrollToTop)
        {
            return;
        }

        if (_viewModel.CurrentEntries.Count == 0)
        {
            return;
        }

        _pendingScrollToTop = true;
        Dispatcher.UIThread.Post(() =>
        {
            _pendingScrollToTop = false;
            ScrollFileViewsToTop();
        }, DispatcherPriority.Background);
    }

    private void ScrollFileViewsToTop()
    {
        if (_viewModel.CurrentEntries.Count == 0)
        {
            return;
        }

        var first = _viewModel.CurrentEntries[0];
        FilesListBox.ScrollIntoView(first);
        FilesIconBox.ScrollIntoView(first);
    }
}