package com.ads.nkh.util

import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.ads.nkh.ads.NkhAd

object AdDebugDialog {

    private fun dp(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    @JvmStatic
    fun show(
        context: Context?,
        anchorView: View?,
        adType: String?,
        adId: String?,
        extraInfo: String?,
        isSuccess: Boolean,
        issueCount: Int
    ) {
        if (context == null) return

        val adConfig = NkhAd.getInstance().adConfig
        if (adConfig != null && !adConfig.isShowAdDebugDialog) {
            return
        }

        // Only show in debug builds
        val isDebuggable = 0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        if (!isDebuggable) return

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Main Container (Card with rounded corners and shadow)
        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6))

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.WHITE)
        cardBg.cornerRadius = dp(context, 8).toFloat()
        cardBg.setStroke(dp(context, 1), Color.parseColor("#CCCCCC"))
        card.background = cardBg

        // Top Section (Icon + Text)
        val topSection = LinearLayout(context)
        topSection.orientation = LinearLayout.HORIZONTAL
        topSection.gravity = Gravity.CENTER_VERTICAL

        // Icon Container (Rounded square)
        val iconContainer = LinearLayout(context)
        iconContainer.gravity = Gravity.CENTER
        iconContainer.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))

        val iconContainerBy = GradientDrawable()
        iconContainerBy.cornerRadius = dp(context, 6).toFloat()
        iconContainerBy.setColor(
            if (isSuccess) Color.parseColor("#E6F4EA") else Color.parseColor("#FCE8E6")
        )
        iconContainer.background = iconContainerBy

        // Icon Circle (Inner symbol)
        val iconView = TextView(context)
        iconView.text = if (isSuccess) "✓" else "!"
        iconView.gravity = Gravity.CENTER
        iconView.setTextColor(Color.WHITE)
        iconView.setTypeface(null, Typeface.BOLD)
        iconView.textSize = 10f

        val iconBg = GradientDrawable()
        iconBg.shape = GradientDrawable.OVAL
        iconBg.setColor(
            if (isSuccess) Color.parseColor("#1E8E3E") else Color.parseColor("#D93025")
        )
        iconView.background = iconBg

        val iconParams = LinearLayout.LayoutParams(dp(context, 18), dp(context, 18))
        iconContainer.addView(iconView, iconParams)

        val containerParams = LinearLayout.LayoutParams(dp(context, 26), dp(context, 26))
        containerParams.rightMargin = dp(context, 8)
        topSection.addView(iconContainer, containerParams)

        // Title and Subtitle container
        val textContainer = LinearLayout(context)
        textContainer.orientation = LinearLayout.VERTICAL

        val title = TextView(context)
        title.text = "AdMob native ad validator"
        title.setTextColor(Color.parseColor("#202124"))
        title.textSize = 13f
        title.setTypeface(null, Typeface.BOLD)
        textContainer.addView(title)

        val subtitle = TextView(context)
        if (isSuccess) {
            subtitle.text = "No implementation issues found"
        } else {
            subtitle.text = "$issueCount implementation issue${if (issueCount > 1) "s" else ""} found"
        }
        subtitle.setTextColor(Color.parseColor("#5F6368"))
        subtitle.textSize = 11f
        textContainer.addView(subtitle)

        topSection.addView(textContainer)
        card.addView(topSection)

        // Body details (the extra technical info if any)
        if (!extraInfo.isNullOrEmpty()) {
            val technicalInfo = TextView(context)
            technicalInfo.text = extraInfo
            technicalInfo.setTextColor(
                if (isSuccess) Color.parseColor("#1E8E3E") else Color.parseColor("#D93025")
            )
            technicalInfo.textSize = 9f
            technicalInfo.setPadding(dp(context, 34), dp(context, 4), 0, dp(context, 4))
            card.addView(technicalInfo)
        }

        // Buttons Section
        val buttonContainer = LinearLayout(context)
        buttonContainer.orientation = LinearLayout.HORIZONTAL
        buttonContainer.gravity = Gravity.END
        buttonContainer.setPadding(0, dp(context, 4), 0, 0)

        // Secondary Button (Dismiss)
        val secondaryBtn = TextView(context)
        secondaryBtn.text = "Dismiss"
        secondaryBtn.isAllCaps = false
        secondaryBtn.setTextColor(Color.parseColor("#1a73e8"))
        secondaryBtn.textSize = 12f
        secondaryBtn.setTypeface(null, Typeface.BOLD)
        secondaryBtn.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4))
        secondaryBtn.setOnClickListener { dialog.dismiss() }
        buttonContainer.addView(secondaryBtn)

        card.addView(buttonContainer)

        dialog.setContentView(card)

        // Position at anchor view or top-left
        val window = dialog.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setGravity(Gravity.TOP or Gravity.START)

            val params = window.attributes
            params.width = (context.resources.displayMetrics.widthPixels * 0.65).toInt()

            if (anchorView != null) {
                val location = IntArray(2)
                anchorView.getLocationOnScreen(location)
                params.x = location[0] + dp(context, 10)
                params.y = location[1] + dp(context, 10)
            } else {
                params.x = dp(context, 10)
                params.y = dp(context, 20)
            }

            params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            window.attributes = params
        }

        try {
            dialog.show()
        } catch (ignored: Exception) {
        }
    }
}
