package com.example.screenshotrouter.storage

import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.RouteResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDecisionTest {
    @Test
    fun copyModeReportsCopiedAndDoesNotClaimMove() {
        val result = RouteDecision.afterVerifiedCopy(RouteMode.Copy, "A", DeleteStatus.NotAttempted)
        assertEquals(RouteResult.Copied("A"), result)
    }

    @Test
    fun moveModeReportsMovedOnlyAfterDeleteSucceeds() {
        val result = RouteDecision.afterVerifiedCopy(RouteMode.Move, "A", DeleteStatus.Deleted)
        assertEquals(RouteResult.Moved("A"), result)
    }

    @Test
    fun moveModeWithUnavailableDeleteReportsPermissionNeedAfterCopy() {
        val result = RouteDecision.afterVerifiedCopy(RouteMode.Move, "B", DeleteStatus.NeedsUserPermission)
        assertEquals(RouteResult.DeleteConsentRequired("B"), result)
    }
}
