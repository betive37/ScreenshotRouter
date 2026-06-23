package com.example.screenshotrouter.storage

import android.net.Uri

sealed interface CopyOutcome {
    data class Success(val destinationUri: Uri, val bytesCopied: Long, val fileName: String) : CopyOutcome
    data class Failure(val reason: String, val cause: Throwable? = null) : CopyOutcome
}
