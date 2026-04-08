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

    public static void show(Context context, View anchorView, String adType, String adId, String extraInfo) {
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
        card.setPadding(25, 25, 25, 25);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(16);
        cardBg.setStroke(2, Color.parseColor("#E0E0E0")); // Light Gray Stroke
        card.setBackground(cardBg);
        
        // Top Section (Icon + Text)
        LinearLayout topSection = new LinearLayout(context);
        topSection.setOrientation(LinearLayout.HORIZONTAL);
        topSection.setGravity(Gravity.CENTER_VERTICAL);
        
        // Red Icon Circle
        TextView iconView = new TextView(context);
        iconView.setText("!");
        iconView.setGravity(Gravity.CENTER);
        iconView.setTextColor(Color.WHITE);
        iconView.setTypeface(null, Typeface.BOLD);
        iconView.setTextSize(10);
        
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(Color.parseColor("#D32F2F")); // Material Red
        iconView.setBackground(iconBg);
        
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(40, 40);
        iconParams.rightMargin = 20;
        topSection.addView(iconView, iconParams);
        
        // Title and Subtitle container
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        
        TextView title = new TextView(context);
        title.setText("Ad Debug Validator");
        title.setTextColor(Color.parseColor("#202124"));
        title.setTextSize(12);
        title.setTypeface(null, Typeface.BOLD);
        textContainer.addView(title);
        
        topSection.addView(textContainer);
        card.addView(topSection);
        
        // Body details (the technical info)
        TextView technicalInfo = new TextView(context);
        technicalInfo.setText("ID: " + adId + (extraInfo != null && !extraInfo.isEmpty() ? "\n" + extraInfo : ""));
        technicalInfo.setTextColor(Color.parseColor("#34A853"));
        technicalInfo.setTextSize(10);
        technicalInfo.setPadding(0, 10, 0, 10);
        card.addView(technicalInfo);

        // Buttons Section
        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(Gravity.END);
        
        // Dismiss Button
        TextView dismissBtn = new TextView(context);
        dismissBtn.setText("Dismiss");
        dismissBtn.setAllCaps(false);
        dismissBtn.setTextColor(Color.parseColor("#1a73e8")); // Google Blue
        dismissBtn.setTextSize(12);
        dismissBtn.setTypeface(null, Typeface.BOLD);
        dismissBtn.setPadding(20, 10, 20, 10);
        dismissBtn.setOnClickListener(v -> dialog.dismiss());
        buttonContainer.addView(dismissBtn);
        
        card.addView(buttonContainer);

        dialog.setContentView(card);
        
        // Position at anchor view or top-left
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.TOP | Gravity.START);
            
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.60); // Width 60%
            
            if (anchorView != null) {
                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                params.x = location[0] + 50; // Offset a bit from left
                params.y = location[1] + 50; // Offset a bit from top of ad
            } else {
                params.x = 30;
                params.y = 30;
            }
            
            params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(params);
        }
        
        try {
            dialog.show();
        } catch (Exception ignored) {}
    }
}
