# Adbrowser

[![GitHub license](https://img.shields.io/github/license/BetterAndroid/Adbrowser?color=blue&style=flat-square)](https://github.com/BetterAndroid/Adbrowser/blob/main/LICENSE)
[![Telegram](https://img.shields.io/badge/discussion-Telegram-blue.svg?logo=telegram&style=flat-square)](https://t.me/BetterAndroid)
[![Telegram](https://img.shields.io/badge/discussion%20dev-Telegram-blue.svg?logo=telegram&style=flat-square)](https://t.me/HighCapable_Dev)
[![QQ](https://img.shields.io/badge/discussion%20dev-QQ-blue.svg?logo=tencent-qq&logoColor=red&style=flat-square)](https://qm.qq.com/cgi-bin/qm/qr?k=Pnsc5RY6N2mBKFjOLPiYldbAbprAU3V7&jump_from=webapi&authKey=X5EsOVzLXt1dRunge8ryTxDRrh9/IiW1Pua75eDLh9RE3KXE+bwXIYF5cWri/9lf)

A modern cross-platform Android file manager powered by ADB.

English | [简体中文](README-zh-CN.md)

| <img src="https://github.com/BetterAndroid/.github/blob/main/img-src/logo.png?raw=true" width = "30" height = "30" alt="LOGO"/> | [BetterAndroid](https://github.com/BetterAndroid) |
|---------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|

This project belongs to the above-mentioned organization, **click the link above to follow this organization** and discover more good projects.

## What's this

**Adbrowser** is a modern, cross-platform Android device file manager built
with [Jetpack Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform).  
It allows developers and testers to access Android devices through ADB and visually browse and manage files on PC — including debug app data without
root access.

The original intention of this project was that the Device File Explorer feature of Android Studio is not stable and reliable enough in some cases and
cannot run independently, and currently there is no complete cross-platform solution.

## Features

- 🔌 **ADB Integration**: Connect to authorized Android devices via ADB
- 🗂 **Full Filesystem Access**: Browse from root `/`, support for `/data/data` via `run-as` or `su`
- 💻 **Cross-platform GUI**: Runs on Windows, macOS, and Linux using compose-multiplatform based on JavaFX
- 🧭 **Tabbed File Browsing**: Multi-tab design for easy navigation
- 🧲 **Drag and Drop Support**: Drag files in/out between device and PC
- 🧠 **Smart Access Layer**: Automatic privilege fallback — root, `run-as`, or read-only
- 🛠 **Designed for Developers**: Useful for debugging, testing, and inspecting app data

## Maintenance Status

This project is a **work in progress (WIP)** and under active development.
Judging from the developers' free time, the implementation may not be very fast.

The goal is to build a fast, reliable, and intuitive ADB-based file manager for Android devices.

> ✅ Code contributions, bug reports, and feature suggestions are very welcome!

## Promotion

<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
     <h2>Hey, please stay! 👋</h2>
     <h3>Here are related projects such as Android development tools, UI design, Gradle plugins, Xposed Modules and practical software. </h3>
     <h3>If the project below can help you, please give me a star! </h3>
     <h3>All projects are free, open source, and follow the corresponding open source license agreement. </h3>
     <h1><a href="https://github.com/fankes/fankes/blob/main/project-promote/README.md">→ To see more about my projects, please click here ←</a></h1>
</div>

## Star History

![Star History Chart](https://api.star-history.com/svg?repos=BetterAndroid/Adbrowser&type=Date)

## License

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

Copyright © 2019 HighCapable