package com.resume.builder.core.update

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG_UPDATE = "TAG_UPDATE"

/** Play's update query is an IPC to the Play Store; a wedged Store must not hold Home forever. */
private const val UPDATE_CHECK_TIMEOUT_MS = 10_000L

/** Pause between Play's update UI being cancelled and shown again. */
private const val REPROMPT_DELAY_MS = 400L

/**
 * Google Play immediate (force) in-app update, driven from Home.
 * [blockUntilUpdated] returns only once Play no longer requires an update.
 */
class PlayForceUpdate(private val activity: Activity) {
    private val manager = AppUpdateManagerFactory.create(activity)

    suspend fun isImmediateUpdateRequired(): Boolean {
        val info = queryInfo() ?: return false
        val required = isForced(info)
        Log.i(TAG_UPDATE, "availability=${info.updateAvailability()} force=$required")
        return required
    }

    /**
     * Shows Play's immediate update UI. Cancel or failure shows it again, every time the host is
     * back in front, so the app stays unusable until the update is installed. Starting the flow
     * while the host is in the background would fail, so each attempt waits for [lifecycle] to
     * reach RESUMED first.
     */
    suspend fun blockUntilUpdated(lifecycle: Lifecycle) {
        Log.i(TAG_UPDATE, "blocking Home until update is installed")
        while (true) {
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            val info = queryInfo()
            if (info == null || !isForced(info)) {
                Log.i(TAG_UPDATE, "force update cleared")
                return
            }
            val code = try {
                manager.startUpdateFlow(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                ).await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.w(TAG_UPDATE, "startUpdateFlow failed", t)
                Activity.RESULT_CANCELED
            }
            Log.i(TAG_UPDATE, "update flow result=$code")
            delay(REPROMPT_DELAY_MS)
        }
    }

    private suspend fun queryInfo(): AppUpdateInfo? = try {
        withTimeoutOrNull(UPDATE_CHECK_TIMEOUT_MS) { manager.appUpdateInfo.await() }
            .also { if (it == null) Log.w(TAG_UPDATE, "update check timed out") }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (t: Throwable) {
        Log.w(TAG_UPDATE, "update check failed", t)
        null
    }

    private fun isForced(info: AppUpdateInfo): Boolean {
        val availability = info.updateAvailability()
        if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) return true
        return availability == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
    }
}

/** Play force-update state for the current main-screen session. */
enum class ForceUpdateGate { Checking, Clear, Required }
