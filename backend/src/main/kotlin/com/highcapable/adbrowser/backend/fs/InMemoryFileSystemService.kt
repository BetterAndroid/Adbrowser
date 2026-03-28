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
 */
package com.highcapable.adbrowser.backend.fs

import com.highcapable.adbrowser.backend.domain.FsEntry
import com.highcapable.adbrowser.backend.domain.FsEntryType
import com.highcapable.adbrowser.backend.domain.FsPermission
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory filesystem implementation for current scaffold stage.
 */
class InMemoryFileSystemService : FileSystemService {

    private val stores = ConcurrentHashMap<String, MutableMap<String, MutableList<FsEntry>>>()

    override fun listEntries(deviceId: String, path: String): List<FsEntry> {
        val normalized = normalizePath(path)
        val store = store(deviceId)
        return store[normalized]?.toList().orEmpty()
    }

    override fun getAttributes(deviceId: String, path: String): FsEntry? {
        val normalized = normalizePath(path)
        if (normalized == "/") {
            return FsEntry(
                path = "/",
                name = "/",
                type = FsEntryType.Directory,
                sizeBytes = 0,
                lastModifiedEpochMillis = System.currentTimeMillis(),
                permission = FsPermission.Full
            )
        }
        val parent = parentPath(normalized)
        return store(deviceId)[parent]
            ?.firstOrNull { it.path == normalized }
    }

    override fun search(deviceId: String, path: String, keyword: String): List<FsEntry> {
        val root = normalizePath(path)
        val target = keyword.trim()
        if (target.isBlank()) return emptyList()
        val ignoreCase = true
        return store(deviceId)
            .asSequence()
            .filter { (folder, _) -> folder.startsWith(root) }
            .flatMap { (_, children) -> children.asSequence() }
            .filter { it.name.contains(target, ignoreCase = ignoreCase) }
            .take(300)
            .toList()
    }

    override fun createFolder(deviceId: String, parentPath: String, name: String): Boolean {
        val safeName = name.trim()
        if (safeName.isBlank()) return false
        val parent = normalizePath(parentPath)
        val targetPath = childPath(parent, safeName)
        val store = store(deviceId)
        val parentChildren = store.getOrPut(parent) { mutableListOf() }
        if (parentChildren.any { it.name == safeName }) return false

        parentChildren += FsEntry(
            path = targetPath,
            name = safeName,
            type = FsEntryType.Directory,
            sizeBytes = 0,
            lastModifiedEpochMillis = System.currentTimeMillis(),
            permission = FsPermission.Full
        )
        store.putIfAbsent(targetPath, mutableListOf())
        return true
    }

    override fun delete(deviceId: String, path: String): Boolean {
        val normalized = normalizePath(path)
        if (normalized == "/") return false
        val parent = parentPath(normalized)
        val store = store(deviceId)
        val removed = store[parent]?.removeIf { it.path == normalized } ?: false
        if (!removed) return false
        store.keys
            .filter { it == normalized || it.startsWith("$normalized/") }
            .toList()
            .forEach { store.remove(it) }
        return true
    }

    override fun rename(deviceId: String, path: String, newName: String): Boolean {
        val safeName = newName.trim()
        if (safeName.isBlank()) return false
        val normalized = normalizePath(path)
        if (normalized == "/") return false

        val parent = parentPath(normalized)
        val store = store(deviceId)
        val siblings = store[parent] ?: return false
        val index = siblings.indexOfFirst { it.path == normalized }
        if (index < 0) return false

        val old = siblings[index]
        val newPath = childPath(parent, safeName)
        siblings[index] = old.copy(path = newPath, name = safeName, lastModifiedEpochMillis = System.currentTimeMillis())

        if (old.type == FsEntryType.Directory) {
            val children = store.remove(normalized)
            if (children != null) {
                store[newPath] = children.map { child ->
                    if (child.path.startsWith(normalized)) child.copy(path = child.path.replaceFirst(normalized, newPath)) else child
                }.toMutableList()
            }
        }
        return true
    }

    override fun updatePermission(deviceId: String, path: String, permission: FsPermission): Boolean {
        val normalized = normalizePath(path)
        if (normalized == "/") return true
        val parent = parentPath(normalized)
        val store = store(deviceId)
        val siblings = store[parent] ?: return false
        val index = siblings.indexOfFirst { it.path == normalized }
        if (index < 0) return false
        siblings[index] = siblings[index].copy(permission = permission)
        return true
    }

    private fun store(deviceId: String): MutableMap<String, MutableList<FsEntry>> = stores.computeIfAbsent(deviceId) { createSeedStore() }

    private fun createSeedStore(): MutableMap<String, MutableList<FsEntry>> {
        val now = System.currentTimeMillis()
        val rootChildren = mutableListOf(
            FsEntry("/sdcard", "sdcard", FsEntryType.Directory, 0, now, FsPermission.Full),
            FsEntry("/storage", "storage", FsEntryType.Directory, 0, now, FsPermission.Full),
            FsEntry("/data", "data", FsEntryType.Directory, 0, now, FsPermission.ReadOnly),
            FsEntry("/system", "system", FsEntryType.Directory, 0, now, FsPermission.ReadOnly)
        )
        val sdcardChildren = mutableListOf(
            FsEntry("/sdcard/Download", "Download", FsEntryType.Directory, 0, now, FsPermission.Full),
            FsEntry("/sdcard/Pictures", "Pictures", FsEntryType.Directory, 0, now, FsPermission.Full),
            FsEntry("/sdcard/DCIM", "DCIM", FsEntryType.Directory, 0, now, FsPermission.Full),
            FsEntry("/sdcard/notes.txt", "notes.txt", FsEntryType.File, 4096, now, FsPermission.Full)
        )
        return mutableMapOf(
            "/" to rootChildren,
            "/sdcard" to sdcardChildren,
            "/storage" to mutableListOf(),
            "/data" to mutableListOf(),
            "/system" to mutableListOf(),
            "/sdcard/Download" to mutableListOf(),
            "/sdcard/Pictures" to mutableListOf(),
            "/sdcard/DCIM" to mutableListOf()
        )
    }

    private fun normalizePath(path: String): String {
        val noTrailing = path.trim().ifBlank { "/" }.replace("\\", "/").trimEnd('/')
        return if (noTrailing.isBlank()) "/" else if (noTrailing.startsWith('/')) noTrailing else "/$noTrailing"
    }

    private fun parentPath(path: String): String {
        if (path == "/") return "/"
        val lastSlash = path.lastIndexOf('/')
        if (lastSlash <= 0) return "/"
        return path.take(lastSlash)
    }

    private fun childPath(parent: String, name: String): String = if (parent == "/") "/$name" else "$parent/$name"
}