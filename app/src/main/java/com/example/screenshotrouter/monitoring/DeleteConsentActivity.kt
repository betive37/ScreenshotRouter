package com.example.screenshotrouter.monitoring

import android.app.Activity
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.RouteResult
import com.example.screenshotrouter.data.DataStoreLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DeleteConsentActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val logRepository by lazy { DataStoreLogRepository(applicationContext) }
    private val notificationController by lazy { MonitorNotificationController(applicationContext) }

    private lateinit var screenshotUri: Uri
    private lateinit var displayName: String
    private lateinit var destinationLabel: String
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenshotUri = Uri.parse(intent.getStringExtra(EXTRA_URI).orEmpty())
        displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: getString(R.string.unknown_screenshot_name)
        destinationLabel = intent.getStringExtra(EXTRA_DESTINATION_LABEL) ?: getString(R.string.unknown_destination_label)
        launched = savedInstanceState?.getBoolean(STATE_LAUNCHED) ?: false

        if (screenshotUri == Uri.EMPTY) {
            finishWithMessage(getString(R.string.log_delete_consent_missing_uri))
            return
        }
        if (!launched) {
            launched = true
            launchDeleteRequest()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_LAUNCHED, launched)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_DELETE) return

        if (resultCode == RESULT_OK) {
            handleDeleteApproved()
        } else {
            val message = getString(R.string.route_result_delete_consent_denied, destinationLabel)
            scope.launch { logRepository.append(message) }
            notificationController.showRouteResult(RouteResult.NeedsUserPermission(message))
        }
        finish()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun launchDeleteRequest() {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> launchPendingIntent(
                    MediaStore.createDeleteRequest(contentResolver, listOf(screenshotUri))
                )
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> launchAndroidTenDeleteRequest()
                else -> finishWithMessage(getString(R.string.log_delete_consent_unavailable))
            }
        } catch (error: Exception) {
            finishWithMessage(
                getString(
                    R.string.log_delete_consent_launch_failed,
                    error.message ?: error::class.java.simpleName
                )
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun launchAndroidTenDeleteRequest() {
        try {
            val deletedRows = contentResolver.delete(screenshotUri, null, null)
            if (deletedRows > 0) {
                handleDeleteApproved()
            } else {
                finishWithMessage(getString(R.string.log_delete_consent_unavailable))
            }
        } catch (error: RecoverableSecurityException) {
            launchPendingIntent(error.userAction.actionIntent)
        }
    }

    private fun launchPendingIntent(pendingIntent: PendingIntent) {
        startIntentSenderForResult(pendingIntent.intentSender, REQUEST_DELETE, null, 0, 0, 0)
    }

    private fun handleDeleteApproved() {
        val result = RouteResult.Moved(destinationLabel)
        val message = RouteResultFormatter.format(this, result)
        scope.launch { logRepository.append(message) }
        notificationController.showRouteResult(result)
        finish()
    }

    private fun finishWithMessage(message: String) {
        scope.launch { logRepository.append(message) }
        notificationController.showRouteResult(RouteResult.NeedsUserPermission(message))
        finish()
    }

    companion object {
        private const val REQUEST_DELETE = 7101
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_DISPLAY_NAME = "extra_display_name"
        private const val EXTRA_DESTINATION_LABEL = "extra_destination_label"
        private const val STATE_LAUNCHED = "state_launched"

        fun intent(context: Context, uriString: String, displayName: String, destinationLabel: String): Intent =
            Intent(context, DeleteConsentActivity::class.java)
                .putExtra(EXTRA_URI, uriString)
                .putExtra(EXTRA_DISPLAY_NAME, displayName)
                .putExtra(EXTRA_DESTINATION_LABEL, destinationLabel)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun intent(context: Context, event: com.example.screenshotrouter.core.model.ScreenshotEvent, destinationLabel: String): Intent =
            intent(context, event.uri.toString(), event.displayName, destinationLabel)
    }
}
