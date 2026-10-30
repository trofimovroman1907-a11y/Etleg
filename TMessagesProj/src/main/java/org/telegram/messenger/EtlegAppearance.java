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

        SharedConfig.bubbleRadius = getMessageBubbleRadiusDp();
        final boolean dark = Theme.isCurrentThemeDark();

        if (dark) {
            set(Theme.key_windowBackgroundWhite, 0xFF111E21);
            set(Theme.key_windowBackgroundGray, 0xFF0C171A);
            set(Theme.key_windowBackgroundGrayShadow, 0x3A000000);
            set(Theme.key_dialogBackground, 0xD9253539);
            set(Theme.key_dialogBackgroundGray, 0xC92B4145);

            set(Theme.key_actionBarDefault, 0xCC1A3438);
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
            set(Theme.key_chat_inBubble, 0xE6294044);
            set(Theme.key_chat_inBubbleSelected, 0xFF29464A);
            set(Theme.key_chat_inBubbleShadow, 0x22000000);
            set(Theme.key_chat_outBubble, 0xE62A625C);
            set(Theme.key_chat_outBubbleSelected, 0xFF28655F);
            set(Theme.key_chat_outBubbleShadow, 0x22000000);
            set(Theme.key_chat_messageTextIn, 0xFFE8F1EF);
            set(Theme.key_chat_messageTextOut, 0xFFEAF6F3);
            set(Theme.key_chat_messagePanelBackground, 0xCC1C3337);
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
            set(Theme.key_windowBackgroundWhite, 0xFFF0F5F4);
            set(Theme.key_windowBackgroundGray, 0xFFE7EFED);
            set(Theme.key_windowBackgroundGrayShadow, 0x160D3435);
            set(Theme.key_dialogBackground, 0xE6FFFFFF);
            set(Theme.key_dialogBackgroundGray, 0xDDFBFCFC);

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
            set(Theme.key_chat_inBubble, 0xEFFFFFFF);
            set(Theme.key_chat_inBubbleSelected, 0xFFE6F1EF);
            set(Theme.key_chat_inBubbleShadow, 0x140D3435);
            set(Theme.key_chat_outBubble, 0xE8D6ECE6);
            set(Theme.key_chat_outBubbleSelected, 0xFFC5E4DC);
            set(Theme.key_chat_outBubbleShadow, 0x140D3435);
            set(Theme.key_chat_messageTextIn, 0xFF203437);
            set(Theme.key_chat_messageTextOut, 0xFF183D3A);
            set(Theme.key_chat_messagePanelBackground, 0xDDFFFFFF);
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

        if (dark) {

            // Shared Etleg surfaces: dialogs, lists, forms, switches and profile pages.
            set(Theme.key_dialogTextBlack, 0xFFE5EFED);
            set(Theme.key_dialogTextLink, 0xFF83D2C7);
            set(Theme.key_dialogTextBlue, 0xFF83D2C7);
            set(Theme.key_dialogTextGray, 0xFF9CB0AE);
            set(Theme.key_dialogTextGray2, 0xFF8DA5A3);
            set(Theme.key_dialogTextHint, 0xFF829A98);
            set(Theme.key_dialogInputField, 0xFF263B3F);
            set(Theme.key_dialogInputFieldActivated, 0xFF58B4A8);
            set(Theme.key_dialogButton, 0xFF70C7BB);
            set(Theme.key_dialogIcon, 0xFF9DB8B4);
            set(Theme.key_dialogGrayLine, 0xFF30464A);
            set(Theme.key_dialogSearchBackground, 0xD92B4145);
            set(Theme.key_dialogSearchHint, 0xFF8DA5A3);
            set(Theme.key_dialogSearchIcon, 0xFF9DB8B4);
            set(Theme.key_dialogSearchText, 0xFFE5EFED);
            set(Theme.key_dialogFloatingButton, 0xFF2B827A);
            set(Theme.key_dialogFloatingButtonPressed, 0xFF246F69);
            set(Theme.key_dialogFloatingIcon, 0xFFFFFFFF);
            set(Theme.key_windowBackgroundWhiteInputField, 0xFF263B3F);
            set(Theme.key_windowBackgroundWhiteInputFieldActivated, 0xFF58B4A8);
            set(Theme.key_windowBackgroundWhiteHintText, 0xFF829A98);
            set(Theme.key_windowBackgroundWhiteValueText, 0xFFB7C9C6);
            set(Theme.key_windowBackgroundWhiteBlueIcon, 0xFF83D2C7);
            set(Theme.key_listSelector, 0x332A8C85);
            set(Theme.key_settings_listSelector, 0x332A8C85);
            set(Theme.key_divider, 0xFF2A4044);
            set(Theme.key_graySection, 0xFF17282C);
            set(Theme.key_graySectionText, 0xFF8DA5A3);
            set(Theme.key_switchTrack, 0xFF40585B);
            set(Theme.key_switchTrackChecked, 0xFF2B827A);
            set(Theme.key_switchTrackBlue, 0xFF40585B);
            set(Theme.key_switchTrackBlueChecked, 0xFF2B827A);
            set(Theme.key_checkbox, 0xFF2B827A);
            set(Theme.key_checkboxCheck, 0xFFFFFFFF);
            set(Theme.key_radioBackground, 0xFF40585B);
            set(Theme.key_radioBackgroundChecked, 0xFF58B4A8);
            set(Theme.key_profile_title, 0xFFE5EFED);
            set(Theme.key_profile_status, 0xFF9CB0AE);
            set(Theme.key_profile_actionIcon, 0xFFB5D0CC);
            set(Theme.key_profile_actionBackground, 0xCC22383B);
            set(Theme.key_profile_actionPressedBackground, 0xFF2B4A4D);
            set(Theme.key_profile_tabText, 0xFF9CB0AE);
            set(Theme.key_profile_tabSelectedText, 0xFF83D2C7);
            set(Theme.key_profile_tabSelectedLine, 0xFF58B4A8);
            set(Theme.key_chats_unreadCounter, 0xFF2B827A);
            set(Theme.key_chats_unreadCounterText, 0xFFFFFFFF);
            set(Theme.key_chats_date, 0xFF829A98);
            set(Theme.key_chats_draft, 0xFFFF9B8E);
            set(Theme.key_chats_sentReadCheck, 0xFF70C7BB);
            set(Theme.key_chats_menuBackground, 0xE6253539);
            set(Theme.key_chats_menuItemText, 0xFFE5EFED);
            set(Theme.key_chats_menuItemIcon, 0xFF9DB8B4);
            set(Theme.key_chats_archiveBackground, 0xD92B4145);
            set(Theme.key_chats_archivePinBackground, 0xFF36585B);
            set(Theme.key_chats_archiveIcon, 0xFFB2C7C4);
            set(Theme.key_chats_archiveText, 0xFFB2C7C4);
            set(Theme.key_chats_actionIcon, 0xFFB2C7C4);
            set(Theme.key_chats_actionBackground, 0xD9253D41);
            set(Theme.key_chats_actionPressedBackground, 0xFF304C50);
            set(Theme.key_chat_attachPhotoBackground, 0xD92B4145);
            set(Theme.key_chat_attachGalleryBackground, 0xD92B4145);
            set(Theme.key_chat_attachAudioBackground, 0xD92B4145);
            set(Theme.key_chat_attachContactBackground, 0xD92B4145);
            set(Theme.key_chat_attachContactText, 0xFFE5EFED);
            set(Theme.key_chat_attachLocationBackground, 0xD92B4145);
            set(Theme.key_chat_attachPollBackground, 0xD92B4145);
            set(Theme.key_chat_searchPanelIcons, 0xFFB2C7C4);
            set(Theme.key_chat_messagePanelVoiceBackground, 0xD92B4145);
            set(Theme.key_chat_messagePanelVoicePressed, 0xFF58B4A8);
            set(Theme.key_chat_serviceBackground, 0xB8233539);
            set(Theme.key_profile_verifiedBackground, 0xFF2B827A);
            set(Theme.key_profile_verifiedCheck, 0xFFFFFFFF);
        } else {

            // Shared Etleg surfaces: dialogs, lists, forms, switches and profile pages.
            set(Theme.key_dialogTextBlack, 0xFF203437);
            set(Theme.key_dialogTextLink, 0xFF267B78);
            set(Theme.key_dialogTextBlue, 0xFF267B78);
            set(Theme.key_dialogTextGray, 0xFF718686);
            set(Theme.key_dialogTextGray2, 0xFF7A9290);
            set(Theme.key_dialogTextHint, 0xFF8A9B99);
            set(Theme.key_dialogInputField, 0xFFF0F5F3);
            set(Theme.key_dialogInputFieldActivated, 0xFF267B78);
            set(Theme.key_dialogButton, 0xFF267B78);
            set(Theme.key_dialogIcon, 0xFF587875);
            set(Theme.key_dialogGrayLine, 0xFFE1EAE7);
            set(Theme.key_dialogSearchBackground, 0xDDEBF1EF);
            set(Theme.key_dialogSearchHint, 0xFF8A9B99);
            set(Theme.key_dialogSearchIcon, 0xFF587875);
            set(Theme.key_dialogSearchText, 0xFF203437);
            set(Theme.key_dialogFloatingButton, 0xFF267B78);
            set(Theme.key_dialogFloatingButtonPressed, 0xFF1D6663);
            set(Theme.key_dialogFloatingIcon, 0xFFFFFFFF);
            set(Theme.key_windowBackgroundWhiteInputField, 0xDDEBF1EF);
            set(Theme.key_windowBackgroundWhiteInputFieldActivated, 0xFF267B78);
            set(Theme.key_windowBackgroundWhiteHintText, 0xFF8A9B99);
            set(Theme.key_windowBackgroundWhiteValueText, 0xFF587875);
            set(Theme.key_windowBackgroundWhiteBlueIcon, 0xFF267B78);
            set(Theme.key_listSelector, 0x1F267B78);
            set(Theme.key_settings_listSelector, 0x1F267B78);
            set(Theme.key_divider, 0xFFE1EAE7);
            set(Theme.key_graySection, 0xDDEBF1EF);
            set(Theme.key_graySectionText, 0xFF718686);
            set(Theme.key_switchTrack, 0xFFCCD9D6);
            set(Theme.key_switchTrackChecked, 0xFF267B78);
            set(Theme.key_switchTrackBlue, 0xFFCCD9D6);
            set(Theme.key_switchTrackBlueChecked, 0xFF267B78);
            set(Theme.key_checkbox, 0xFF267B78);
            set(Theme.key_checkboxCheck, 0xFFFFFFFF);
            set(Theme.key_radioBackground, 0xFFCCD9D6);
            set(Theme.key_radioBackgroundChecked, 0xFF267B78);
            set(Theme.key_profile_title, 0xFF203437);
            set(Theme.key_profile_status, 0xFF718686);
            set(Theme.key_profile_actionIcon, 0xFF267B78);
            set(Theme.key_profile_actionBackground, 0xCFE6F0ED);
            set(Theme.key_profile_actionPressedBackground, 0xFFD6E8E3);
            set(Theme.key_profile_tabText, 0xFF718686);
            set(Theme.key_profile_tabSelectedText, 0xFF267B78);
            set(Theme.key_profile_tabSelectedLine, 0xFF267B78);
            set(Theme.key_chats_unreadCounter, 0xFF267B78);
            set(Theme.key_chats_unreadCounterText, 0xFFFFFFFF);
            set(Theme.key_chats_date, 0xFF8A9B99);
            set(Theme.key_chats_draft, 0xFFB65B50);
            set(Theme.key_chats_sentReadCheck, 0xFF267B78);
            set(Theme.key_chats_menuBackground, 0xFFFFFFFF);
            set(Theme.key_chats_menuItemText, 0xFF203437);
            set(Theme.key_chats_menuItemIcon, 0xFF587875);
            set(Theme.key_chats_archiveBackground, 0xDDEBF1EF);
            set(Theme.key_chats_archivePinBackground, 0xFFD5E5E1);
            set(Theme.key_chats_archiveIcon, 0xFF557774);
            set(Theme.key_chats_archiveText, 0xFF557774);
            set(Theme.key_chats_actionIcon, 0xFF267B78);
            set(Theme.key_chats_actionBackground, 0xDDEBF1EF);
            set(Theme.key_chats_actionPressedBackground, 0xFFD6E8E3);
            set(Theme.key_chat_attachPhotoBackground, 0xDDEBF1EF);
            set(Theme.key_chat_attachGalleryBackground, 0xDDEBF1EF);
            set(Theme.key_chat_attachAudioBackground, 0xDDEBF1EF);
            set(Theme.key_chat_attachContactBackground, 0xDDEBF1EF);
            set(Theme.key_chat_attachContactText, 0xFF203437);
            set(Theme.key_chat_attachLocationBackground, 0xDDEBF1EF);
            set(Theme.key_chat_attachPollBackground, 0xDDEBF1EF);
            set(Theme.key_chat_searchPanelIcons, 0xFF557774);
            set(Theme.key_chat_messagePanelVoiceBackground, 0xDDEBF1EF);
            set(Theme.key_chat_messagePanelVoicePressed, 0xFF267B78);
            set(Theme.key_chat_serviceBackground, 0xB8FFFFFF);
            set(Theme.key_profile_verifiedBackground, 0xFF267B78);
            set(Theme.key_profile_verifiedCheck, 0xFFFFFFFF);
        }
    }

    private static void set(int key, int color) {
        Theme.setDefaultColor(key, color);
        Theme.setColor(key, color, false);
    }

    public static int getMessageBubbleRadiusDp() {
        return 21;
    }

    public static int getDialogCardRadiusDp() {
        return 18;
    }
}
