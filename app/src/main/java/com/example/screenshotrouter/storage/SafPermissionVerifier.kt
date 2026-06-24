package com.example.screenshotrouter.storage

import android.content.Context
import android.net.Uri

object SafPermissionVerifier {
    fun hasPersistedReadWritePermission(context: Context, treeUri: Uri): Boolean {
        val treeUriString = treeUri.toString()
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri.toString() == treeUriString &&
                permission.isReadPermission &&
                permission.isWritePermission
        }
    }
}
