package com.pixelforge.ads.common

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import com.pixelforge.ads.R

/**
 * Centered “Ad is showing” hold shown for [HOLD_MS] immediately before a fullscreen ad.
 */
object AdShowingDialog {
    const val HOLD_MS = 500L

    fun showThen(activity: Activity, onReady: () -> Unit) {
        val show = Runnable { present(activity, onReady) }
        if (Looper.myLooper() == Looper.getMainLooper()) show.run()
        else Handler(Looper.getMainLooper()).post(show)
    }

    private fun present(activity: Activity, onReady: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.ads_showing_dialog)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setDimAmount(0f)
        }
        try {
            dialog.show()
        } catch (t: Throwable) {
            adsLogW("ad_showing_dialog_skip", detail = t.message)
            onReady()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { if (dialog.isShowing) dialog.dismiss() }
            onReady()
        }, HOLD_MS)
    }
}
