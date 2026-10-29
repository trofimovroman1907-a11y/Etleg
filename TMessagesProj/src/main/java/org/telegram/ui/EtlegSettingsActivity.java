/* Etleg customization hub. The actual theme controls remain Telegram's own. */
package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class EtlegSettingsActivity extends BaseFragment {
    private final ArrayList<TextSettingsCell> rows = new ArrayList<>();
    private final ArrayList<TextView> headers = new ArrayList<>();
    private LinearLayout content;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.EtlegSettingsTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        rows.clear();
        headers.clear();
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(content, new ScrollView.LayoutParams(-1, -2));
        fragmentView = scrollView;

        addHeader(context, R.string.EtlegAppearanceSection);
        addRow(context, R.string.EtlegThemesAndAccent, () -> openTheme(1));
        addRow(context, R.string.EtlegChatBackground, () -> openTheme(2));
        addRow(context, R.string.EtlegBubbleCorners, () -> openTheme(3));
        addRow(context, R.string.EtlegTextSize, () -> openTheme(4));
        addRow(context, R.string.EtlegChatList, () -> openTheme(5));
        addRow(context, R.string.EtlegNightMode, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_NIGHT)));
        addHeader(context, R.string.EtlegMotionSection);
        addRow(context, R.string.EtlegMotionAndPower, () -> presentFragment(new LiteModeSettingsActivity()));
        applyColors();
        return fragmentView;
    }

    private void openTheme(int section) {
        presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC).openEtlegSection(section));
    }

    private void addHeader(Context context, int label) {
        TextView header = new TextView(context);
        header.setText(LocaleController.getString(label));
        header.setTextSize(14);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        header.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), 0);
        content.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        headers.add(header);
    }

    private void addRow(Context context, int label, Runnable action) {
        TextSettingsCell row = new TextSettingsCell(context);
        row.setText(LocaleController.getString(label), false);
        row.setBackground(Theme.getSelectorDrawable(true));
        row.setOnClickListener(v -> action.run());
        content.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
        rows.add(row);
    }

    private void applyColors() {
        if (fragmentView == null) return;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        for (TextView header : headers) header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        for (TextSettingsCell row : rows) {
            row.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            row.setBackground(Theme.getSelectorDrawable(true));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        applyColors();
    }
}
