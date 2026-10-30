package org.telegram.messenger;

import org.telegram.ui.ActionBar.Theme;

/**
 * Etleg's native visual identity.
 *
 * Kept package-gated so this appearance never changes other Telegram builds
 * that share the upstream source tree.
 */
public final class EtlegAppearance {

    private static final String PACKAGE_PREFIX = "app.etleg.messenger";
    private static volatile int packageState;

    private EtlegAppearance() {
    }

    public static boolean isEtlegApp() {
        int cached = packageState;
        if (cached != 0) {
            return cached > 0;
        }
        if (ApplicationLoader.applicationContext == null) {
            return false;
        }
        String packageName = ApplicationLoader.applicationContext.getPackageName();
        boolean matches = packageName != null && packageName.startsWith(PACKAGE_PREFIX);
        packageState = matches ? 1 : -1;
        return matches;
    }

    public static void applyTheme() {
        if (!isEtlegApp()) {
            return;
        }

        final boolean dark = Theme.isCurrentThemeDark();

        if (dark) {
            set(Theme.key_windowBackgroundWhite, 0xFF111E21);
            set(Theme.key_windowBackgroundGray, 0xFF0C171A);
            set(Theme.key_windowBackgroundGrayShadow, 0x33000000);
            set(Theme.key_dialogBackground, 0xFF1B2B2F);
            set(Theme.key_dialogBackgroundGray, 0xFF22363A);

            set(Theme.key_actionBarDefault, 0xFF14292D);
            set(Theme.key_actionBarDefaultIcon, 0xFFE8F2F0);
            set(Theme.key_actionBarDefaultTitle, 0xFFE8F2F0);
            set(Theme.key_actionBarDefaultSubtitle, 0xFF9EB6B4);
            set(Theme.key_actionBarDefaultSelector, 0x332A8C85);

            set(Theme.key_windowBackgroundWhiteBlackText, 0xFFE5EFED);
            set(Theme.key_windowBackgroundWhiteGrayText, 0xFF9CB0AE);
            set(Theme.key_windowBackgroundWhiteGrayIcon, 0xFF8DA5A3);
            set(Theme.key_windowBackgroundWhiteBlueText, 0xFF7BC8BE);
            set(Theme.key_windowBackgroundWhiteBlueButton, 0xFF2B827A);
            set(Theme.key_windowBackgroundWhiteLinkText, 0xFF83D2C7);

            set(Theme.key_chats_name, 0xFFE4EFED);
            set(Theme.key_chats_message, 0xFF9DB1AF);
            set(Theme.key_chat_inBubble, 0xFF20363A);
            set(Theme.key_chat_inBubbleSelected, 0xFF29464A);
            set(Theme.key_chat_inBubbleShadow, 0x22000000);
            set(Theme.key_chat_outBubble, 0xFF20534E);
            set(Theme.key_chat_outBubbleSelected, 0xFF28655F);
            set(Theme.key_chat_outBubbleShadow, 0x22000000);
            set(Theme.key_chat_messageTextIn, 0xFFE8F1EF);
            set(Theme.key_chat_messageTextOut, 0xFFEAF6F3);
            set(Theme.key_chat_messagePanelBackground, 0xFF182B2E);
            set(Theme.key_chat_messagePanelShadow, 0x22000000);
            set(Theme.key_chat_messagePanelText, 0xFFE7F1EF);
            set(Theme.key_chat_messagePanelHint, 0xFF8DA5A2);
            set(Theme.key_chat_messagePanelCursor, 0xFF65B8AE);
            set(Theme.key_chat_messagePanelIcons, 0xFFB1C5C2);
            set(Theme.key_chat_messagePanelSend, 0xFF58B4A8);
            set(Theme.key_chat_attachIcon, 0xFFB1C5C2);
            set(Theme.key_chat_attachActiveTab, 0xFF58B4A8);

            set(Theme.key_glass_defaultIcon, 0xFFB2C7C4);
            set(Theme.key_glass_defaultText, 0xFFB2C7C4);
            set(Theme.key_glass_tabSelected, 0xFF2B7772);
            set(Theme.key_glass_tabSelectedText, 0xFFFFFFFF);
            set(Theme.key_glass_tabUnselected, 0xFF9DB3B0);
            set(Theme.key_telegram_color_dialogsLogo, 0xFF77C8BD);
            set(Theme.key_telegram_color, 0xFF2B827A);
            set(Theme.key_telegram_color_text, 0xFFE8F2F0);
        } else {
            set(Theme.key_windowBackgroundWhite, 0xFFF2F6F5);
            set(Theme.key_windowBackgroundGray, 0xFFEAF0EF);
            set(Theme.key_windowBackgroundGrayShadow, 0x160D3435);
            set(Theme.key_dialogBackground, 0xFFFFFFFF);
            set(Theme.key_dialogBackgroundGray, 0xFFF5F8F7);

            set(Theme.key_actionBarDefault, 0xFF17464A);
            set(Theme.key_actionBarDefaultIcon, 0xFFF4FAF8);
            set(Theme.key_actionBarDefaultTitle, 0xFFF4FAF8);
            set(Theme.key_actionBarDefaultSubtitle, 0xFFBDD4D1);
            set(Theme.key_actionBarDefaultSelector, 0x3327A19A);

            set(Theme.key_windowBackgroundWhiteBlackText, 0xFF203437);
            set(Theme.key_windowBackgroundWhiteGrayText, 0xFF718686);
            set(Theme.key_windowBackgroundWhiteGrayIcon, 0xFF7A9290);
            set(Theme.key_windowBackgroundWhiteBlueText, 0xFF267B78);
            set(Theme.key_windowBackgroundWhiteBlueButton, 0xFF267B78);
            set(Theme.key_windowBackgroundWhiteLinkText, 0xFF267B78);

            set(Theme.key_chats_name, 0xFF203437);
            set(Theme.key_chats_message, 0xFF728785);
            set(Theme.key_chat_inBubble, 0xFFFFFFFF);
            set(Theme.key_chat_inBubbleSelected, 0xFFE6F1EF);
            set(Theme.key_chat_inBubbleShadow, 0x140D3435);
            set(Theme.key_chat_outBubble, 0xFFD6ECE6);
            set(Theme.key_chat_outBubbleSelected, 0xFFC5E4DC);
            set(Theme.key_chat_outBubbleShadow, 0x140D3435);
            set(Theme.key_chat_messageTextIn, 0xFF203437);
            set(Theme.key_chat_messageTextOut, 0xFF183D3A);
            set(Theme.key_chat_messagePanelBackground, 0xFFF8FBFA);
            set(Theme.key_chat_messagePanelShadow, 0x160D3435);
            set(Theme.key_chat_messagePanelText, 0xFF203437);
            set(Theme.key_chat_messagePanelHint, 0xFF8A9B99);
            set(Theme.key_chat_messagePanelCursor, 0xFF267B78);
            set(Theme.key_chat_messagePanelIcons, 0xFF587875);
            set(Theme.key_chat_messagePanelSend, 0xFF267B78);
            set(Theme.key_chat_attachIcon, 0xFF587875);
            set(Theme.key_chat_attachActiveTab, 0xFF267B78);

            set(Theme.key_glass_defaultIcon, 0xFF557774);
            set(Theme.key_glass_defaultText, 0xFF557774);
            set(Theme.key_glass_tabSelected, 0xFF267B78);
            set(Theme.key_glass_tabSelectedText, 0xFFFFFFFF);
            set(Theme.key_glass_tabUnselected, 0xFF587875);
            set(Theme.key_telegram_color_dialogsLogo, 0xFF267B78);
            set(Theme.key_telegram_color, 0xFF267B78);
            set(Theme.key_telegram_color_text, 0xFF203437);
        }
    }

    private static void set(int key, int color) {
        Theme.setColor(key, color, false);
    }

    public static int getMessageBubbleRadiusDp() {
        return 21;
    }

    public static int getDialogCardRadiusDp() {
        return 18;
    }
}
