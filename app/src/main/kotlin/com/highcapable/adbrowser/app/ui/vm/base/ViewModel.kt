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
 * This file is created by fankes on 2026/4/3.
 */
package com.highcapable.adbrowser.app.ui.vm.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Base class for all app view models.
 *
 * Architecture-only adapter layer that mirrors the Avalonia MVVM organization.
 */
abstract class ViewModel {

    /**
     * A [SupervisorJob] that can be used to manage the lifecycle of coroutines launched by this view model.
     */
    protected val modelJob = SupervisorJob()

    /**
     * A [CoroutineScope] that uses the [modelJob] and the Main dispatcher for launching coroutines in this view model.
     */
    protected val modelScope = CoroutineScope(modelJob + Dispatchers.Main.immediate)
}