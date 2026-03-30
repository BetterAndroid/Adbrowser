// Copyright (C) 2019 HighCapable
// SPDX-License-Identifier: AGPL-3.0-or-later

using System.Collections.ObjectModel;
using System.Windows.Input;
using Adbrowser.Backend.Logging;
using Adbrowser.Frontend.Infrastructure;
using Adbrowser.Frontend.Localization;

namespace Adbrowser.Frontend.ViewModels;

/// <summary>
/// View model for application/adb log viewer.
/// </summary>
public sealed class LogViewerViewModel(ILogService logService, ILocalizationService localizationService) : ViewModelBase
{
    /// <summary>
    /// Log entry collection.
    /// </summary>
    public ObservableCollection<LogEntryItem> Entries { get; } = [];

    /// <summary>
    /// Window title.
    /// </summary>
    public string Title => T("logs.title");

    /// <summary>
    /// Refresh command.
    /// </summary>
    public ICommand RefreshCommand => new RelayCommand(Refresh);

    public string RefreshText => T("logs.refresh");
    public string HeaderTime => T("logs.header.time");
    public string HeaderLevel => T("logs.header.level");
    public string HeaderCategory => T("logs.header.category");
    public string HeaderMessage => T("logs.header.message");

    /// <summary>
    /// Loads current logs.
    /// </summary>
    public void Refresh()
    {
        Entries.Clear();

        foreach (var entry in logService.GetEntries())
        {
            Entries.Add(new LogEntryItem(
                entry.Time.ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss"),
                entry.Level.ToString(),
                entry.Category,
                entry.Message));
        }
    }

    private string T(string key) => localizationService.GetString(key);
}

/// <summary>
/// UI item for rendering a log entry.
/// </summary>
public sealed record LogEntryItem(string Time, string Level, string Category, string Message);