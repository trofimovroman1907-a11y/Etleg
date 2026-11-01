package app.nexora.messenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(245, 246, 250);
    private static final int INK = Color.rgb(26, 31, 49);
    private static final int MUTED = Color.rgb(126, 133, 153);
    private static final int ACCENT = Color.rgb(86, 96, 207);
    private static final int LINE = Color.rgb(231, 233, 241);

    private LinearLayout root;
    private boolean guestMode = false;
    private String currentPage = "channels";

    private static final class Channel {
        final String name;
        final String handle;
        final String category;
        final String description;
        final String mark;
        final int tint;
        Channel(String name, String handle, String category, String description, String mark, int tint) {
            this.name = name;
            this.handle = handle;
            this.category = category;
            this.description = description;
            this.mark = mark;
            this.tint = tint;
        }
    }

    private final List<Channel> channels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        seedChannels();
        showWelcome();
    }

    private void seedChannels() {
        channels.add(new Channel("Город сегодня", "@city_today", "Новости",
                "Коротко о событиях, людях и местах вокруг нас.", "Г", 0xFFE8ECFF));
        channels.add(new Channel("Научная среда", "@science_daily", "Наука",
                "Открытия, технологии и понятные объяснения сложного.", "Н", 0xFFE5F4EF));
        channels.add(new Channel("Культурный код", "@culture_code", "Культура",
                "Книги, кино, музыка и идеи для вдохновения.", "К", 0xFFFFF0DF));
        channels.add(new Channel("Тихий дизайн", "@quiet_design", "Дизайн",
                "Интерфейсы, типографика и визуальные находки.", "Д", 0xFFF4E9F8));
        channels.add(new Channel("Игровая полка", "@game_shelf", "Игры",
                "Новости индустрии и заметки о хороших играх.", "И", 0xFFFFE9E9));
    }

    private void showWelcome() {
        root = baseRoot();
        LinearLayout content = column(28);
        content.setPadding(dp(24), dp(36), dp(24), dp(24));

        TextView brand = text("N", 24, Color.WHITE, true);
        brand.setGravity(Gravity.CENTER);
        brand.setBackground(round(ACCENT, 18));
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        markLp.bottomMargin = dp(28);
        content.addView(brand, markLp);

        content.addView(text("NEXORA", 13, ACCENT, true));
        TextView headline = text("Твоё общение.\nТвои правила.", 34, INK, true);
        headline.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams headLp = params(-1, -2);
        headLp.topMargin = dp(10);
        content.addView(headline, headLp);

        TextView subtitle = text("Личные разговоры, сообщества и каналы — в одном спокойном пространстве.", 16, MUTED, false);
        subtitle.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams subLp = params(-1, -2);
        subLp.topMargin = dp(14);
        subLp.bottomMargin = dp(30);
        content.addView(subtitle, subLp);

        LinearLayout feature = cardColumn();
        feature.addView(featureRow("01", "Приватность в основе", "Доступ к данным будет ограничен правилами аккаунта."));
        addDivider(feature);
        feature.addView(featureRow("02", "Свои каналы", "Читайте интересные публикации в удобном формате."));
        addDivider(feature);
        feature.addView(featureRow("03", "Без лишнего шума", "Чистый интерфейс и понятная навигация."));
        content.addView(feature);

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView login = actionButton("Войти или создать аккаунт", ACCENT, Color.WHITE);
        login.setOnClickListener(v -> showInfo("Авторизация", "Подключение регистрации, входа и подтверждения почты — следующий этап разработки. Сейчас сервер авторизации ещё не подключён."));
        LinearLayout.LayoutParams loginLp = params(-1, 54);
        loginLp.topMargin = dp(20);
        content.addView(login, loginLp);

        TextView guest = actionButton("Продолжить как гость", Color.WHITE, INK);
        guest.setBackground(roundWithStroke(Color.WHITE, LINE, 16));
        guest.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Гостевой режим")
                .setMessage("В гостевом режиме ты можешь только просматривать каналы. Отправлять сообщения, писать пользователям, звонить и загружать файлы нельзя.")
                .setPositiveButton("OK", (dialog, which) -> {
                    guestMode = true;
                    showChannels();
                })
                .show());
        LinearLayout.LayoutParams guestLp = params(-1, 54);
        guestLp.topMargin = dp(10);
        content.addView(guest, guestLp);

        TextView foot = text("NEXORA  ·  ПРЕДВАРИТЕЛЬНАЯ ВЕРСИЯ 0.1", 10, MUTED, true);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footLp = params(-1, -2);
        footLp.topMargin = dp(20);
        content.addView(foot, footLp);

        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void showChannels() {
        currentPage = "channels";
        root = baseRoot();
        root.addView(header("Каналы", "Гостевой просмотр"));
        LinearLayout body = column(14);
        body.setPadding(dp(20), dp(10), dp(20), dp(12));

        TextView intro = text("Читайте то, что интересно", 23, INK, true);
        body.addView(intro);
        body.addView(text("В гостевом режиме доступен только просмотр публикаций.", 13, MUTED, false));

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(INK);
        search.setHintTextColor(MUTED);
        search.setHint("Поиск каналов");
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(roundWithStroke(Color.WHITE, LINE, 14));
        LinearLayout.LayoutParams searchLp = params(-1, 50);
        searchLp.topMargin = dp(8);
        body.addView(search, searchLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout list = column(10);
        list.setPadding(0, dp(4), 0, dp(12));
        for (Channel channel : channels) list.addView(channelCard(channel));
        scroll.addView(list);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollLp.topMargin = dp(4);
        body.addView(scroll, scrollLp);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                list.removeAllViews();
                String query = s.toString().toLowerCase(Locale.ROOT).trim();
                for (Channel channel : channels) {
                    if (channel.name.toLowerCase(Locale.ROOT).contains(query)
                            || channel.handle.toLowerCase(Locale.ROOT).contains(query)
                            || channel.category.toLowerCase(Locale.ROOT).contains(query)) {
                        list.addView(channelCard(channel));
                    }
                }
                if (list.getChildCount() == 0) {
                    TextView empty = text("Ничего не найдено", 14, MUTED, false);
                    empty.setGravity(Gravity.CENTER);
                    list.addView(empty, params(-1, 72));
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(bottomBar("channels"));
        setContentView(root);
    }

    private void showExplore() {
        currentPage = "explore";
        root = baseRoot();
        root.addView(header("Обзор", "Подборка Nexora"));
        LinearLayout body = column(12);
        body.setPadding(dp(20), dp(14), dp(20), dp(18));
        body.addView(text("Найди свою тему", 24, INK, true));
        body.addView(text("Выбирай интересное и открывай каналы для чтения.", 13, MUTED, false));
        String[] categories = {"Новости", "Наука", "Культура", "Дизайн", "Игры"};
        for (String category : categories) {
            TextView item = actionButton(category + "   →", Color.WHITE, INK);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(18), 0, dp(18), 0);
            item.setBackground(roundWithStroke(Color.WHITE, LINE, 14));
            item.setOnClickListener(v -> {
                showChannels();
                Toast.makeText(this, "Найди каналы в категории «" + category + "»", Toast.LENGTH_SHORT).show();
            });
            body.addView(item, params(-1, 54));
        }
        View spacer = new View(this);
        body.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        body.addView(noteCard("Гостевой доступ", "Ты можешь читать открытые каналы. Для личных сообщений и звонков понадобится аккаунт."));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(bottomBar("explore"));
        setContentView(root);
    }

    private void showProfile() {
        currentPage = "profile";
        root = baseRoot();
        root.addView(header("Профиль", "Режим просмотра"));
        LinearLayout body = column(12);
        body.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView avatar = text("Г", 28, ACCENT, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(0xFFE8ECFF, 30));
        body.addView(avatar, params(68, 68));
        body.addView(text("Гость Nexora", 22, INK, true));
        body.addView(text("Без регистрации · Только чтение каналов", 13, MUTED, false));
        body.addView(noteCard("Ограничения гостя", "Личные сообщения, отправка файлов, голосовые сообщения и звонки доступны после регистрации."));
        TextView signIn = actionButton("Войти или создать аккаунт", ACCENT, Color.WHITE);
        signIn.setOnClickListener(v -> showInfo("Авторизация", "Сервер регистрации и отправки кодов подтверждения ещё не подключён. Этот экран появится после настройки backend."));
        body.addView(signIn, params(-1, 52));
        TextView back = actionButton("Выйти из гостевого режима", Color.WHITE, INK);
        back.setBackground(roundWithStroke(Color.WHITE, LINE, 14));
        back.setOnClickListener(v -> {
            guestMode = false;
            showWelcome();
        });
        body.addView(back, params(-1, 52));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(bottomBar("profile"));
        setContentView(root);
    }

    private void showChannelDetail(Channel channel) {
        currentPage = "detail";
        root = baseRoot();
        LinearLayout top = column(4);
        top.setPadding(dp(20), dp(18), dp(20), dp(14));
        TextView back = text("‹  Все каналы", 14, ACCENT, true);
        back.setOnClickListener(v -> showChannels());
        top.addView(back);
        root.addView(top);
        LinearLayout body = column(14);
        body.setPadding(dp(20), dp(8), dp(20), dp(20));
        TextView icon = text(channel.mark, 28, ACCENT, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(channel.tint, 20));
        body.addView(icon, params(68, 68));
        body.addView(text(channel.name, 26, INK, true));
        body.addView(text(channel.handle + "  ·  " + channel.category, 13, MUTED, false));
        body.addView(noteCard("О канале", channel.description));
        body.addView(noteCard("Публикации", "В этой ранней версии показан интерфейс канала. Подключение реальных публикаций и серверной ленты будет следующим этапом."));
        View spacer = new View(this);
        body.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        TextView guestNote = text("Гостевой режим: доступен только просмотр.", 12, MUTED, false);
        guestNote.setGravity(Gravity.CENTER);
        body.addView(guestNote);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private LinearLayout baseRoot() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BG);
        return layout;
    }

    private View header(String title, String subtitle) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(16), dp(20), dp(12));
        TextView logo = text("N", 19, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(ACCENT, 12));
        bar.addView(logo, params(40, 40));
        LinearLayout labels = column(2);
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(title, 18, INK, true));
        labels.addView(text(subtitle, 11, MUTED, false));
        bar.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView badge = text("G", 13, ACCENT, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(0xFFE8ECFF, 24));
        bar.addView(badge, params(34, 34));
        return bar;
    }

    private View bottomBar(String selected) {
        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setGravity(Gravity.CENTER);
        nav.addView(navItem("Каналы", "channels", selected), new LinearLayout.LayoutParams(0, dp(54), 1f));
        nav.addView(navItem("Обзор", "explore", selected), new LinearLayout.LayoutParams(0, dp(54), 1f));
        nav.addView(navItem("Профиль", "profile", selected), new LinearLayout.LayoutParams(0, dp(54), 1f));
        return nav;
    }

    private View navItem(String label, String page, String selected) {
        TextView item = text(label, 12, selected.equals(page) ? ACCENT : MUTED, selected.equals(page));
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(v -> {
            if ("channels".equals(page)) showChannels();
            else if ("explore".equals(page)) showExplore();
            else showProfile();
        });
        return item;
    }

    private View channelCard(Channel channel) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(roundWithStroke(Color.WHITE, LINE, 16));

        TextView avatar = text(channel.mark, 18, ACCENT, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(channel.tint, 14));
        card.addView(avatar, params(48, 48));

        LinearLayout labels = column(4);
        labels.setPadding(dp(12), 0, dp(8), 0);
        labels.addView(text(channel.name, 15, INK, true));
        labels.addView(text(channel.handle + "  ·  " + channel.category, 11, MUTED, false));
        card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 24, MUTED, false);
        card.addView(arrow);
        card.setOnClickListener(v -> showChannelDetail(channel));
        return card;
    }

    private View featureRow(String number, String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView num = text(number, 11, ACCENT, true);
        num.setGravity(Gravity.CENTER);
        num.setBackground(round(0xFFE8ECFF, 10));
        row.addView(num, params(36, 36));
        LinearLayout labels = column(3);
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(title, 14, INK, true));
        labels.addView(text(desc, 11, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private View noteCard(String title, String body) {
        LinearLayout card = cardColumn();
        card.addView(text(title, 14, INK, true));
        TextView desc = text(body, 13, MUTED, false);
        desc.setLineSpacing(dp(4), 1f);
        card.addView(desc);
        return card;
    }

    private LinearLayout cardColumn() {
        LinearLayout card = column(12);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundWithStroke(Color.WHITE, LINE, 16));
        return card;
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dp(2);
        lp.bottomMargin = dp(2);
        parent.addView(divider, lp);
    }

    private LinearLayout column(int gapDp) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setClipToPadding(false);
        GradientDrawable spacing = new GradientDrawable();
        spacing.setColor(Color.TRANSPARENT);
        spacing.setSize(dp(1), dp(gapDp));
        layout.setDividerDrawable(spacing);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return layout;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private TextView actionButton(String value, int background, int foreground) {
        TextView button = text(value, 14, foreground, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(round(background, 15));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable roundWithStroke(int color, int stroke, int radiusDp) {
        GradientDrawable drawable = round(color, radiusDp);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showInfo(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (guestMode && !"channels".equals(currentPage)) {
            showChannels();
            return;
        }
        super.onBackPressed();
    }
}
