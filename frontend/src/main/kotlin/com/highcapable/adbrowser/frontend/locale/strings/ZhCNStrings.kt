/*
 * Adbrowser - A modern cross-platform Android file manager powered by ADB.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/Adbrowser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2025/6/4.
 */
package com.highcapable.adbrowser.frontend.locale.strings

import cafe.adriel.lyricist.LyricistStrings
import com.highcapable.adbrowser.frontend.locale.Locales

@LyricistStrings(languageTag = Locales.ZH_CN)
val ZhCNStrings = Strings(
    appTitle = "Adbrowser",
    setupTitle = "初始配置",
    setupDescription = "在使用 Adbrowser 前，请先选择 ADB 可执行文件路径。",
    adbPathLabel = "ADB 可执行文件路径",
    continueText = "继续",
    invalidAdbPath = "当前路径无效，请选择可执行的 adb 文件。",
    devicePanelTitle = "可用设备",
    refresh = "刷新",
    noDevices = "没有已连接设备",
    path = "路径",
    up = "上一级",
    root = "根目录",
    home = "主页",
    search = "搜索",
    searchPlaceholder = "搜索文件",
    viewMode = "查看",
    sortMode = "排序",
    nameSort = "名称",
    sizeSort = "大小",
    modifiedSort = "修改时间",
    listView = "列表",
    gridView = "图标",
    createFolder = "新建文件夹",
    folderName = "文件夹名称",
    files = "文件",
    status = "状态",
    language = "语言",
    general = "通用",
    device = "设备",
    rememberLastDevice = "记忆上次连接设备",
    rememberDevicePath = "记忆每个设备浏览路径",
    save = "保存",
    adbLog = "ADB 日志",
    appLog = "应用日志",
    preferences = "偏好设置",
    logViewer = "日志查看器"
)