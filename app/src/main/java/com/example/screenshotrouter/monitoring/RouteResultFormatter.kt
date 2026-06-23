package com.example.screenshotrouter.monitoring

import android.content.Context
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.RouteResult

object RouteResultFormatter {
    fun format(context: Context, result: RouteResult): String = when (result) {
        is RouteResult.Copied -> context.getString(R.string.route_result_copied, result.destinationLabel)
        is RouteResult.Moved -> context.getString(R.string.route_result_moved, result.destinationLabel)
        is RouteResult.DeleteConsentRequired -> context.getString(
            R.string.route_result_delete_consent_required,
            result.destinationLabel
        )
        is RouteResult.NeedsUserPermission -> result.reason
        is RouteResult.Failed -> result.reason
    }
}
