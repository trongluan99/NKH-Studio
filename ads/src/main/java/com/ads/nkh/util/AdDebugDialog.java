package com.ads.nkh.util;

import android.app.Dialog;
import android.content.Context;
import com.ads.nkh.ads.NkhAd;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.ApplicationInfo;

public class AdDebugDialog {
    
    private static int dp(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static void show(Context context, View anchorView, String adType, String adId, String extraInfo, boolean isSuccess, int issueCount) {
        if (context == null) return;
        
        if (NkhAd.getInstance().getAdConfig() != null && !NkhAd.getInstance().getAdConfig().isShowAdDebugDialog()) {
            return;
        }

        // Only show in debug builds
        boolean isDebuggable = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        if (!isDebuggable) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Main Container (Card with rounded corners and shadow)
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(context, 8));
        cardBg.setStroke(dp(context, 1), Color.parseColor("#CCCCCC")); 
        card.setBackground(cardBg);
        
        // Top Section (Icon + Text)
        LinearLayout topSection = new LinearLayout(context);
        topSection.setOrientation(LinearLayout.HORIZONTAL);
        topSection.setGravity(Gravity.CENTER_VERTICAL);
        
        // Icon Container (Rounded square)
        LinearLayout iconContainer = new LinearLayout(context);
        iconContainer.setGravity(Gravity.CENTER);
        iconContainer.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
        
        GradientDrawable iconContainerBy = new GradientDrawable();
        iconContainerBy.setCornerRadius(dp(context, 6));
        iconContainerBy.setColor(isSuccess ? Color.parseColor("#E6F4EA") : Color.parseColor("#FCE8E6"));
        iconContainer.setBackground(iconContainerBy);

        // Icon Circle (Inner symbol)
        TextView iconView = new TextView(context);
        iconView.setText(isSuccess ? "✓" : "!");
        iconView.setGravity(Gravity.CENTER);
        iconView.setTextColor(Color.WHITE);
        iconView.setTypeface(null, Typeface.BOLD);
        iconView.setTextSize(10);
        
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(isSuccess ? Color.parseColor("#1E8E3E") : Color.parseColor("#D93025")); 
        iconView.setBackground(iconBg);
        
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(context, 18), dp(context, 18));
        iconContainer.addView(iconView, iconParams);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(dp(context, 26), dp(context, 26));
        containerParams.rightMargin = dp(context, 8);
        topSection.addView(iconContainer, containerParams);
        
        // Title and Subtitle container
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        
        TextView title = new TextView(context);
        title.setText("AdMob native ad validator");
        title.setTextColor(Color.parseColor("#202124"));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        textContainer.addView(title);

        TextView subtitle = new TextView(context);
        if (isSuccess) {
            subtitle.setText("No implementation issues found");
        } else {
            subtitle.setText(issueCount + " implementation issue" + (issueCount > 1 ? "s" : "") + " found");
        }
        subtitle.setTextColor(Color.parseColor("#5F6368"));
        subtitle.setTextSize(11);
        textContainer.addView(subtitle);
        
        topSection.addView(textContainer);
        card.addView(topSection);
        
        // Body details (the extra technical info if any)
        if (extraInfo != null && !extraInfo.isEmpty()) {
            TextView technicalInfo = new TextView(context);
            technicalInfo.setText(extraInfo);
            technicalInfo.setTextColor(isSuccess ? Color.parseColor("#1E8E3E") : Color.parseColor("#D93025"));
            technicalInfo.setTextSize(9);
            technicalInfo.setPadding(dp(context, 34), dp(context, 4), 0, dp(context, 4));
            card.addView(technicalInfo);
        }

        // Buttons Section
        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(Gravity.END);
        buttonContainer.setPadding(0, dp(context, 4), 0, 0);
        
        // Secondary Button (Dismiss)
        TextView secondaryBtn = new TextView(context);
        secondaryBtn.setText("Dismiss");
        secondaryBtn.setAllCaps(false);
        secondaryBtn.setTextColor(Color.parseColor("#1a73e8")); 
        secondaryBtn.setTextSize(12);
        secondaryBtn.setTypeface(null, Typeface.BOLD);
        secondaryBtn.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
        secondaryBtn.setOnClickListener(v -> dialog.dismiss());
        buttonContainer.addView(secondaryBtn);

        card.addView(buttonContainer);

        dialog.setContentView(card);
        
        // Position at anchor view or top-left
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.TOP | Gravity.START);
            
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.65); 
            
            if (anchorView != null) {
                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                params.x = location[0] + dp(context, 10);
                params.y = location[1] + dp(context, 10);
            } else {
                params.x = dp(context, 10);
                params.y = dp(context, 20);
            }
            
            params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(params);
        }
        
        try {
            dialog.show();
        } catch (Exception ignored) {}
    }
}
