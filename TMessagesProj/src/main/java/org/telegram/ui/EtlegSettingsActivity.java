package org.telegram.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.EtlegAppearance;
import org.telegram.messenger.EtlegPreferences;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/** Etleg's own interface controls. No stock Telegram theme-page redirects. */
public class EtlegSettingsActivity extends BaseFragment {
    private LinearLayout content;
    private int cardColor;
    private int textColor;
    private int secondaryColor;
    private int accentColor;

    @Override
    public View createView(Context context) {
        EtlegAppearance.applyTheme();
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Etleg");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        fragmentView = scroll;

        updatePalette();
        addHero(context);
        addSection(context, R.string.EtlegInterfaceSection);
        addSwitchCard(context,
                new SettingRow(R.drawable.etleg_ic_density, R.string.EtlegCompactChats, R.string.EtlegCompactChatsInfo,
                        EtlegPreferences.isCompactChatList(), value -> EtlegPreferences.setCompactChatList(context, value)));
        addSwitchRow(context, new SettingRow(R.drawable.etleg_ic_labels, R.string.EtlegTabLabels, R.string.EtlegTabLabelsInfo,
                EtlegPreferences.isTabLabelsEnabled(), value -> EtlegPreferences.setTabLabelsEnabled(context, value)));
        addSwitchRow(context, new SettingRow(R.drawable.etleg_ic_motion, R.string.EtlegReducedMotion, R.string.EtlegReducedMotionInfo,
                EtlegPreferences.isReducedMotion(), value -> EtlegPreferences.setReducedMotion(context, value)));

        addSection(context, R.string.EtlegInteractionSection);
        addSwitchCard(context, new SettingRow(R.drawable.etleg_ic_haptic, R.string.EtlegHaptics, R.string.EtlegHapticsInfo,
                EtlegPreferences.isHapticEnabled(), value -> EtlegPreferences.setHapticEnabled(context, value)));

        addSection(context, R.string.EtlegAboutSection);
        addAboutCard(context);
        return fragmentView;
    }

    private interface ToggleAction {
        void onChanged(boolean value);
    }

    private static class SettingRow {
        final int icon;
        final int title;
        final int description;
        final boolean checked;
        final ToggleAction action;

        SettingRow(int icon, int title, int description, boolean checked, ToggleAction action) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.checked = checked;
            this.action = action;
        }
    }

    private void updatePalette() {
        cardColor = Theme.getColor(Theme.key_dialogBackground);
        textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        secondaryColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
    }

    private void addHero(Context context) {
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        hero.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(26), cardColor));

        TextView brand = new TextView(context);
        brand.setText("Etleg");
        brand.setTextColor(textColor);
        brand.setTextSize(28);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hero.addView(brand, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.getString(R.string.EtlegSettingsSubtitle));
        subtitle.setTextColor(secondaryColor);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        hero.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        content.addView(hero, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 16));
    }

    private void addSection(Context context, int titleRes) {
        TextView title = new TextView(context);
        title.setText(LocaleController.getString(titleRes).toUpperCase());
        title.setTextColor(secondaryColor);
        title.setTextSize(12);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLetterSpacing(0.07f);
        title.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(8), AndroidUtilities.dp(5), AndroidUtilities.dp(8));
        content.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 4));
    }

    private void addSwitchCard(Context context, SettingRow row) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), cardColor));
        addSwitchRow(context, row, card);
        content.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 14));
    }

    private void addSwitchRow(Context context, SettingRow row) {
        LinearLayout card = null;
        if (content.getChildCount() == 0 || !(content.getChildAt(content.getChildCount() - 1) instanceof LinearLayout)) {
            card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), cardColor));
            content.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 14));
        } else {
            View previous = content.getChildAt(content.getChildCount() - 1);
            if (previous.getTag() == null) {
                card = (LinearLayout) previous;
            }
        }
        if (card == null) {
            card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), cardColor));
            content.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 14));
        }
        addSwitchRow(context, row, card);
    }

    private void addSwitchRow(Context context, SettingRow row, LinearLayout card) {
        LinearLayout line = new LinearLayout(context);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(13), AndroidUtilities.dp(14), AndroidUtilities.dp(13));

        ImageView icon = new ImageView(context);
        icon.setImageResource(row.icon);
        icon.setColorFilter(accentColor);
        icon.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(13), Theme.multAlpha(accentColor, 0.12f)));
        icon.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        line.addView(icon, LayoutHelper.createLinear(42, 42, Gravity.CENTER_VERTICAL, 0, 0, 13, 0));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText(LocaleController.getString(row.title));
        title.setTextColor(textColor);
        title.setTextSize(15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labels.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView description = new TextView(context);
        description.setText(LocaleController.getString(row.description));
        description.setTextColor(secondaryColor);
        description.setTextSize(12);
        description.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        labels.addView(description, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        line.addView(labels, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(context);
        toggle.setShowText(false);
        toggle.setChecked(row.checked);
        toggle.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{0xFFFFFFFF, 0xFFF6F8F7}));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{accentColor, 0xFF9EAAA8}));
        toggle.setOnCheckedChangeListener((button, checked) -> row.action.onChanged(checked));
        line.addView(toggle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        line.setBackground(Theme.getSelectorDrawable(true));
        card.addView(line, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addAboutCard(Context context) {
        TextView about = new TextView(context);
        about.setText(LocaleController.getString(R.string.EtlegAboutText));
        about.setTextColor(secondaryColor);
        about.setTextSize(13);
        about.setLineSpacing(AndroidUtilities.dp(3), 1f);
        about.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(17), AndroidUtilities.dp(18), AndroidUtilities.dp(17));
        about.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), cardColor));
        content.addView(about, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public void onResume() {
        super.onResume();
        EtlegAppearance.applyTheme();
        updatePalette();
        if (fragmentView != null) fragmentView.invalidate();
    }
}
