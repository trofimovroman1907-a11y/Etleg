package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.HapticFeedbackConstants;
import android.view.View;

/** App-local preferences for Etleg-only interface behavior. */
public final class EtlegPreferences {
    private static final String PREFS = "etleg_ui";
    private static final String COMPACT_CHAT_LIST = "compact_chat_list";
    private static final String REDUCED_MOTION = "reduced_motion";
    private static final String HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String TAB_LABELS = "tab_labels";
    private static final String AUTO_HIDE_DOCK = "auto_hide_dock";

    private EtlegPreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Context appContext() {
        return ApplicationLoader.applicationContext;
    }

    private static boolean isEtleg() {
        return EtlegAppearance.isEtlegApp() && appContext() != null;
    }

    public static boolean isCompactChatList() {
        Context context = appContext();
        return isEtleg() && context != null && prefs(context).getBoolean(COMPACT_CHAT_LIST, false);
    }

    public static void setCompactChatList(Context context, boolean value) {
        if (EtlegAppearance.isEtlegApp() && context != null) prefs(context).edit().putBoolean(COMPACT_CHAT_LIST, value).apply();
    }

    public static boolean isReducedMotion() {
        Context context = appContext();
        return isEtleg() && context != null && prefs(context).getBoolean(REDUCED_MOTION, false);
    }

    public static void setReducedMotion(Context context, boolean value) {
        if (EtlegAppearance.isEtlegApp() && context != null) prefs(context).edit().putBoolean(REDUCED_MOTION, value).apply();
    }

    public static boolean isHapticEnabled() {
        Context context = appContext();
        return isEtleg() && context != null && prefs(context).getBoolean(HAPTIC_FEEDBACK, true);
    }

    public static void setHapticEnabled(Context context, boolean value) {
        if (EtlegAppearance.isEtlegApp() && context != null) prefs(context).edit().putBoolean(HAPTIC_FEEDBACK, value).apply();
    }

    public static boolean isTabLabelsEnabled() {
        Context context = appContext();
        return isEtleg() && context != null && prefs(context).getBoolean(TAB_LABELS, true);
    }

    public static void setTabLabelsEnabled(Context context, boolean value) {
        if (EtlegAppearance.isEtlegApp() && context != null) prefs(context).edit().putBoolean(TAB_LABELS, value).apply();
    }

    public static boolean isAutoHideDockEnabled() {
        Context context = appContext();
        return isEtleg() && context != null && prefs(context).getBoolean(AUTO_HIDE_DOCK, true);
    }

    public static void setAutoHideDockEnabled(Context context, boolean value) {
        if (EtlegAppearance.isEtlegApp() && context != null) prefs(context).edit().putBoolean(AUTO_HIDE_DOCK, value).apply();
    }

    public static int getChatRowHeight(int normalHeight) {
        return isCompactChatList() ? Math.max(64, normalHeight - 12) : normalHeight;
    }

    public static void performHapticFeedback(View view) {
        if (view != null && isHapticEnabled()) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }
}
