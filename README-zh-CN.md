# Adbrowser

[![GitHub license](https://img.shields.io/github/license/BetterAndroid/Adbrowser?color=blue&style=flat-square)](https://github.com/BetterAndroid/Adbrowser/blob/main/LICENSE)
[![Telegram](https://img.shields.io/badge/discussion%20dev-Telegram-blue.svg?logo=telegram&style=flat-square)](https://t.me/BetterAndroid_Dev)
[![QQ](https://img.shields.io/badge/discussion%20dev-QQ-blue.svg?logo=tencent-qq&logoColor=red&style=flat-square)](https://qm.qq.com/cgi-bin/qm/qr?k=Pnsc5RY6N2mBKFjOLPiYldbAbprAU3V7&jump_from=webapi&authKey=X5EsOVzLXt1dRunge8ryTxDRrh9/IiW1Pua75eDLh9RE3KXE+bwXIYF5cWri/9lf)

一个由 ADB 提供支持的现代跨平台 Android 文件管理器。

[English](README.md) | 简体中文

| <img src="https://github.com/BetterAndroid/.github/blob/main/img-src/logo.png?raw=true" width = "30" height = "30" alt="LOGO"/> | [BetterAndroid](https://github.com/BetterAndroid) |
|---------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|

这个项目属于上述组织，**点击上方链接关注这个组织**，发现更多好项目。

## 这是什么

**Adbrowser** 是一种现代的、跨平台的 Android 设备文件管理器，使用 [JetPack Compose Multiplatform](https://github.com/jetbrains/compose-multiplatform)
驱动。
它允许开发人员和测试人员通过 ADB 访问 Android 设备并在 PC 上可视化浏览和管理文件 - 包括无 Root 权限访问的调试应用程序数据。

这个项目创建的初衷还是因为 Android Studio 的 Device File Explorer 功能在某些情况下不够稳定和可靠而且无法独立运行，并且目前没有一套完整的跨平台解决方案。

## 功能

- 🔌 **ADB 集成**：通过 ADB 连接到授权的 Android 设备
- 🗂 **完整的文件系统访问**: 使用 Root 浏览 `/`，支持通过 `run-as` 或 `su` 访问 `/data/data`
- 💻 **跨平台 GUI**：使用基于 Javafx 的 Compose-Multiplatform 在 Windows、macOS 和 Linux 上运行
- 🧭 **选项卡式文件浏览**：易于导航的多标签设计
- 🧲 **拖放支持**：在设备和 PC 之间拖拽文件
- 🧠 **智能访问层**: 自动启用特权模式、`run-as` 或只读
- 🛠 **为开发人员设计**：可用于调试，测试和检查应用程序数据

## 维护状态

该项目正在积极开发中 **(WIP)**。从开发者们的空闲时间来看，实际开发进度可能不会很快。

项目的目的是为 Android 设备构建一个基于 ADB 的快速，可靠和直观的文件管理器。

> ✅ 我们非常欢迎您贡献代码并参与开发，或问题报告及功能建议！

## 更多项目

<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
    <h2>嘿，还请君留步！👋</h2>
    <h3>如果你觉得这个项目能给你提供帮助，不妨继续往下看看我的更多项目吧！</h3>
    <h3>如果这些项目能为你提供帮助，不妨为我点个关注或者 star ⭐️ 吧！</h3>
    <h1><a href="https://github.com/fankes/fankes/blob/main/project-promote/README-zh-CN.md">→ 查看更多关于我的项目，请点击这里 ←</a></h1>
</div>

## Star History

![Star History Chart](https://api.star-history.com/svg?repos=BetterAndroid/Adbrowser&type=Date)

## 许可证

- [AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)

```
Copyright (C) 2019 HighCapable

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```

版权所有 © 2019 HighCapable