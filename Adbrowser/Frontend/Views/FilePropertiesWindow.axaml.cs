// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using Adbrowser.Backend.Logging;
using Adbrowser.Backend.Permission;
using Adbrowser.Frontend.Runtime;
using Adbrowser.Frontend.ViewModels;
using Avalonia.Controls;
using Avalonia.Interactivity;

namespace Adbrowser.Frontend.Views;

public partial class FilePropertiesWindow : Window
{
    private readonly FileEntrySnapshot? _snapshot;
    private readonly IPermissionService? _permissionService;
    private readonly ILogService? _logService;
    private bool _isSyncingPermissionUi;

    /// <summary>
    /// Raised when this window wants to report a user-facing status message.
    /// </summary>
    public event Action<string>? StatusChanged;

    /// <summary>
    /// Raised when permission of current file has been updated.
    /// </summary>
    public event Action<string, string>? PermissionChanged;

    public FilePropertiesWindow()
    {
        InitializeComponent();
    }

    public FilePropertiesWindow(FileEntrySnapshot snapshot, IPermissionService permissionService, ILogService logService)
    {
        InitializeComponent();
        _snapshot = snapshot;
        _permissionService = permissionService;
        _logService = logService;
        var t = AppServices.LocalizationService.GetString;

        Title = t("dialog.properties.title");
        NameLabel.Text = t("dialog.properties.fieldName");
        PathLabel.Text = t("dialog.properties.fieldPath");
        TypeLabel.Text = t("dialog.properties.fieldType");
        SizeLabel.Text = t("dialog.properties.fieldSize");
        ModifiedLabel.Text = t("dialog.properties.fieldModified");
        PermissionLabel.Text = t("dialog.properties.fieldPermission");
        PermissionBitsLabel.Text = t("dialog.properties.fieldPermissionBits");
        OwnerBitsLabel.Text = t("dialog.properties.owner");
        GroupBitsLabel.Text = t("dialog.properties.group");
        OtherBitsLabel.Text = t("dialog.properties.other");
        OwnerReadCheck.Content = t("dialog.properties.read");
        OwnerWriteCheck.Content = t("dialog.properties.write");
        OwnerExecuteCheck.Content = t("dialog.properties.execute");
        GroupReadCheck.Content = t("dialog.properties.read");
        GroupWriteCheck.Content = t("dialog.properties.write");
        GroupExecuteCheck.Content = t("dialog.properties.execute");
        OtherReadCheck.Content = t("dialog.properties.read");
        OtherWriteCheck.Content = t("dialog.properties.write");
        OtherExecuteCheck.Content = t("dialog.properties.execute");
        ApplyPermissionButton.Content = t("dialog.properties.apply");
        CloseButton.Content = t("dialog.properties.close");

        NameText.Text = snapshot.Name;
        PathText.Text = snapshot.FullPath;
        TypeText.Text = snapshot.IsDirectory ? t("dialog.properties.typeDirectory") : t("dialog.properties.typeFile");
        SizeText.Text = snapshot.IsDirectory ? "-" : snapshot.Size.ToString("N0");
        ModifiedText.Text = snapshot.ModifiedTime.ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss");

        _ = LoadPermissionAsync();
    }

    private async Task<FilePermissionInfo?> LoadPermissionAsync()
    {
        if (_snapshot is null || _permissionService is null)
        {
            return null;
        }

        try
        {
            var info = await _permissionService.GetPermissionAsync(_snapshot.Serial, _snapshot.FullPath);
            PermissionText.Text = info.SymbolicPermission;
            ModeTextBox.Text = info.NumericPermission.ToString();
            SyncBitsFromMode(info.NumericPermission);
            return info;
        }
        catch (Exception ex)
        {
            _logService?.Log(LogLevel.Error, "UI", ex.Message);
            PushStatus(ex.Message);
            PermissionText.Text = _snapshot.SymbolicPermission;

            if (int.TryParse(_snapshot.SymbolicPermission, out var mode))
            {
                SyncBitsFromMode(mode);
            }

            return null;
        }
    }

    private async void OnApplyPermissionClick(object? sender, RoutedEventArgs e)
    {
        if (_snapshot is null || _permissionService is null)
        {
            return;
        }

        if (!TryParsePermissionMode(ModeTextBox.Text, out var mode))
        {
            PushStatus(AppServices.LocalizationService.GetString("dialog.properties.invalidPermission"));
            return;
        }

        try
        {
            var result = await _permissionService.SetPermissionAsync(_snapshot.Serial, _snapshot.FullPath, mode);

            if (!result.IsSuccess)
            {
                PushStatus(result.ErrorMessage ?? AppServices.LocalizationService.GetString("common.unknownError"));
                return;
            }

            PushStatus(AppServices.LocalizationService.GetString("dialog.properties.permissionUpdated"));
            var info = await LoadPermissionAsync();
            if (info is not null)
            {
                PermissionChanged?.Invoke(_snapshot.FullPath, info.SymbolicPermission);
            }
        }
        catch (Exception ex)
        {
            _logService?.Log(LogLevel.Error, "UI", ex.Message);
            PushStatus(ex.Message);
        }
    }

    private void OnCloseClick(object? sender, RoutedEventArgs e)
    {
        Close();
    }

    private void OnPermissionBitChanged(object? sender, RoutedEventArgs e)
    {
        if (_isSyncingPermissionUi)
        {
            return;
        }

        var mode = BuildModeFromBits();
        ModeTextBox.Text = mode.ToString();
        PermissionText.Text = ModeToSymbolic(mode);
    }

    private void OnModeTextBoxLostFocus(object? sender, RoutedEventArgs e)
    {
        if (!TryParsePermissionMode(ModeTextBox.Text, out var mode))
        {
            return;
        }

        SyncBitsFromMode(mode);
        PermissionText.Text = ModeToSymbolic(mode);
    }

    private void SyncBitsFromMode(int mode)
    {
        var owner = mode / 100;
        var group = (mode / 10) % 10;
        var other = mode % 10;

        _isSyncingPermissionUi = true;

        OwnerReadCheck.IsChecked = (owner & 4) != 0;
        OwnerWriteCheck.IsChecked = (owner & 2) != 0;
        OwnerExecuteCheck.IsChecked = (owner & 1) != 0;

        GroupReadCheck.IsChecked = (group & 4) != 0;
        GroupWriteCheck.IsChecked = (group & 2) != 0;
        GroupExecuteCheck.IsChecked = (group & 1) != 0;

        OtherReadCheck.IsChecked = (other & 4) != 0;
        OtherWriteCheck.IsChecked = (other & 2) != 0;
        OtherExecuteCheck.IsChecked = (other & 1) != 0;

        _isSyncingPermissionUi = false;
    }

    private int BuildModeFromBits()
    {
        var owner = 0;
        var group = 0;
        var other = 0;

        if (OwnerReadCheck.IsChecked is true) owner += 4;
        if (OwnerWriteCheck.IsChecked is true) owner += 2;
        if (OwnerExecuteCheck.IsChecked is true) owner += 1;

        if (GroupReadCheck.IsChecked is true) group += 4;
        if (GroupWriteCheck.IsChecked is true) group += 2;
        if (GroupExecuteCheck.IsChecked is true) group += 1;

        if (OtherReadCheck.IsChecked is true) other += 4;
        if (OtherWriteCheck.IsChecked is true) other += 2;
        if (OtherExecuteCheck.IsChecked is true) other += 1;

        return owner * 100 + group * 10 + other;
    }

    private static bool TryParsePermissionMode(string? modeText, out int mode)
    {
        mode = 0;

        if (!int.TryParse(modeText, out var parsed))
        {
            return false;
        }

        if (parsed < 0 || parsed > 777)
        {
            return false;
        }

        var owner = parsed / 100;
        var group = (parsed / 10) % 10;
        var other = parsed % 10;

        if (owner > 7 || group > 7 || other > 7)
        {
            return false;
        }

        mode = parsed;
        return true;
    }

    private static string ModeToSymbolic(int mode)
    {
        var owner = mode / 100;
        var group = (mode / 10) % 10;
        var other = mode % 10;
        return $"{OctalToRwx(owner)}{OctalToRwx(group)}{OctalToRwx(other)}";
    }

    private static string OctalToRwx(int value)
    {
        var read = (value & 4) != 0 ? 'r' : '-';
        var write = (value & 2) != 0 ? 'w' : '-';
        var execute = (value & 1) != 0 ? 'x' : '-';
        return new string([read, write, execute]);
    }

    private void PushStatus(string message)
    {
        StatusChanged?.Invoke(message);
    }
}