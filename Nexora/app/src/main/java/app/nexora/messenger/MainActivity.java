package app.nexora.messenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 246, 250);
    private static final int INK = Color.rgb(28, 29, 43);
    private static final int MUTED = Color.rgb(119, 122, 143);
    private static final int ACCENT = Color.rgb(105, 75, 205);
    private static final int LINE = Color.rgb(230, 230, 239);

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Room> rooms = new ArrayList<>();

    private MatrixE2eeClient matrix;
    private String homeserver = "https://matrix.org";
    private String userId;
    private Room activeRoom;
    private LinearLayout root;
    private LinearLayout timeline;
    private ScrollView timelineScroll;
    private EditText messageInput;

    private static final class Room {
        final String id;
        final String name;
        Room(String id, String name) { this.id = id; this.name = name; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        homeserver = getPreferences(0).getString("homeserver", homeserver);
        // Remove the legacy REST token. All new sessions are owned by the Matrix Rust SDK.
        SecureSessionStore.clear(this, "token");
        showLoading("Проверяем защищённую сессию…");
        io.execute(() -> {
            try {
                MatrixE2eeClient restored = MatrixE2eeClient.restore(this);
                if (restored == null) {
                    runOnUiThread(this::showLogin);
                    return;
                }
                matrix = restored;
                userId = matrix.getUserId();
                runOnUiThread(this::loadRooms);
            } catch (Exception e) {
                SecureSessionStore.clear(this, "matrix_sdk_session_v1");
                runOnUiThread(() -> {
                    showLogin();
                    toast("Сессию не удалось восстановить. Войди снова.");
                });
            }
        });
    }

    private void showLogin() {
        root = base();
        LinearLayout page = col(14);
        page.setPadding(dp(24), dp(32), dp(24), dp(24));
        page.addView(text("NEXORA", 14, ACCENT, true));
        TextView title = text("Твоё общение.\nТвои правила.", 32, INK, true);
        LinearLayout.LayoutParams titleParams = lp(-1, -2);
        titleParams.topMargin = dp(12);
        page.addView(title, titleParams);
        TextView hint = text("Подключись к серверу Matrix. Сообщения в зашифрованных комнатах обрабатываются официальным Matrix SDK прямо на устройстве.", 14, MUTED, false);
        hint.setLineSpacing(dp(4), 1f);
        page.addView(hint);

        EditText server = field("Адрес сервера, например https://chat.example.org", false);
        server.setText(homeserver);
        page.addView(server, lp(-1, 52));
        EditText username = field("Имя пользователя", false);
        page.addView(username, lp(-1, 52));
        EditText password = field("Пароль", true);
        page.addView(password, lp(-1, 52));

        TextView login = button("Войти", ACCENT, Color.WHITE);
        page.addView(login, lp(-1, 54));
        TextView register = button("Создать аккаунт", Color.WHITE, INK);
        register.setBackground(shape(Color.WHITE, LINE, 15));
        page.addView(register, lp(-1, 54));

        TextView note = text("Комнаты, созданные в Nexora, создаются с E2EE. Отправка в незашифрованные комнаты блокируется.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        page.addView(note);

        login.setOnClickListener(v -> authenticate(server.getText().toString(),
                username.getText().toString(), password.getText().toString(), false));
        register.setOnClickListener(v -> authenticate(server.getText().toString(),
                username.getText().toString(), password.getText().toString(), true));

        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void authenticate(String server, String username, String password, boolean register) {
        String baseUrl = server.trim().replaceAll("/+$", "");
        if (!baseUrl.startsWith("https://")) {
            toast("Укажи адрес сервера с https://");
            return;
        }
        if (username.trim().isEmpty() || password.isEmpty()) {
            toast("Заполни имя и пароль");
            return;
        }
        homeserver = baseUrl;
        showLoading(register ? "Создаём аккаунт…" : "Подключаемся через Matrix SDK…");
        io.execute(() -> {
            try {
                if (register) registerAccount(baseUrl, username.trim(), password);
                MatrixE2eeClient connected = MatrixE2eeClient.login(
                        this, baseUrl, username.trim(), password);
                matrix = connected;
                userId = connected.getUserId();
                getPreferences(0).edit().putString("homeserver", baseUrl).apply();
                runOnUiThread(this::loadRooms);
            } catch (Exception e) {
                String message = safe(e.getMessage());
                runOnUiThread(() -> {
                    showLogin();
                    toast("Не удалось войти: " + message);
                });
            }
        });
    }

    private void registerAccount(String server, String username, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        body.put("auth", new JSONObject());
        requestJson(server + "/_matrix/client/v3/register", body);
    }

    private JSONObject requestJson(String url, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();
        JSONObject json = response.isEmpty() ? new JSONObject() : new JSONObject(response);
        if (status < 200 || status >= 300) {
            String error = json.optString("error", "HTTP " + status);
            throw new Exception(error + " (" + status + ")");
        }
        return json;
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString("UTF-8");
        }
    }

    private void loadRooms() {
        if (matrix == null) {
            showLogin();
            return;
        }
        showLoading("Загружаем защищённые чаты…");
        io.execute(() -> {
            try {
                List<MatrixE2eeClient.RoomSummary> sdkRooms = matrix.joinedRooms();
                synchronized (rooms) {
                    rooms.clear();
                    for (MatrixE2eeClient.RoomSummary room : sdkRooms) {
                        rooms.add(new Room(room.getId(), room.getName()));
                    }
                }
                runOnUiThread(this::showChats);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showChats();
                    toast("Не удалось обновить список комнат: " + safe(e.getMessage()));
                });
            }
        });
    }

    private void showChats() {
        root = base();
        root.addView(header("Nexora", userId == null ? "Защищённые сообщения" : userId));
        LinearLayout body = col(10);
        body.setPadding(dp(18), dp(12), dp(18), dp(12));

        TextView add = button("＋  Новая зашифрованная комната", ACCENT, Color.WHITE);
        add.setOnClickListener(v -> showCreateRoom());
        body.addView(add, lp(-1, 50));

        TextView status = text("Matrix SDK · E2EE через Olm/Megolm", 12, MUTED, false);
        body.addView(status);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = col(8);
        list.setPadding(0, dp(4), 0, dp(12));
        synchronized (rooms) {
            for (Room room : rooms) {
                LinearLayout card = new LinearLayout(this);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                card.setBackground(shape(Color.WHITE, LINE, 15));
                TextView avatar = text(initial(room.name), 18, ACCENT, true);
                avatar.setGravity(Gravity.CENTER);
                avatar.setBackground(shape(0xFFEDE9FF, 0xFFEDE9FF, 18));
                card.addView(avatar, lp(46, 46));
                LinearLayout labels = col(3);
                labels.setPadding(dp(12), 0, 0, 0);
                labels.addView(text(room.name, 15, INK, true));
                labels.addView(text("Открыть защищённую переписку", 12, MUTED, false));
                card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
                card.setOnClickListener(v -> openRoom(room));
                list.addView(card, lp(-1, -2));
            }
        }
        if (list.getChildCount() == 0) {
            list.addView(note("Пока нет чатов", "Создай зашифрованную комнату или попроси друга пригласить тебя в E2EE-комнату."));
        }
        scroll.addView(list);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView refresh = text("Обновить список", 13, MUTED, true);
        refresh.setGravity(Gravity.CENTER);
        refresh.setPadding(0, dp(10), 0, dp(10));
        refresh.setOnClickListener(v -> loadRooms());
        body.addView(refresh);

        TextView logout = text("Выйти из аккаунта", 13, MUTED, true);
        logout.setGravity(Gravity.CENTER);
        logout.setPadding(0, dp(10), 0, dp(10));
        logout.setOnClickListener(v -> logout());
        body.addView(logout);

        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showCreateRoom() {
        LinearLayout form = col(10);
        form.setPadding(dp(8), dp(8), dp(8), dp(8));
        EditText name = field("Название комнаты", false);
        EditText invite = field("Matrix ID участника (необязательно)", false);
        form.addView(name);
        form.addView(invite);
        new AlertDialog.Builder(this)
                .setTitle("Новая E2EE-комната")
                .setView(form)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String roomName = name.getText().toString().trim();
                    String member = invite.getText().toString().trim();
                    if (roomName.isEmpty()) {
                        toast("Укажи название");
                        return;
                    }
                    io.execute(() -> {
                        try {
                            List<String> invitees = member.isEmpty()
                                    ? Collections.emptyList() : Collections.singletonList(member);
                            String roomId = matrix.createEncryptedPrivateRoom(roomName, invitees);
                            Room room = new Room(roomId, roomName);
                            synchronized (rooms) { rooms.add(0, room); }
                            runOnUiThread(() -> openRoom(room));
                        } catch (Exception e) {
                            runOnUiThread(() -> toast("Не удалось создать комнату: " + safe(e.getMessage())));
                        }
                    });
                })
                .show();
    }

    private void openRoom(Room room) {
        activeRoom = room;
        root = base();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), dp(10));
        TextView back = text("‹", 32, ACCENT, true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> backToChats());
        top.addView(back, lp(40, 48));

        LinearLayout titleCol = col(2);
        titleCol.addView(text(room.name, 17, INK, true));
        titleCol.addView(text("Сквозное шифрование · E2EE", 11, MUTED, false));
        top.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top);

        timelineScroll = new ScrollView(this);
        timeline = col(8);
        timeline.setPadding(dp(14), dp(12), dp(14), dp(12));
        timelineScroll.addView(timeline);
        root.addView(timelineScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(12), dp(8), dp(12), dp(8));
        composer.setBackgroundColor(Color.WHITE);
        messageInput = field("Сообщение", false);
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView send = button("➤", ACCENT, Color.WHITE);
        LinearLayout.LayoutParams sendParams = lp(52, 48);
        sendParams.leftMargin = dp(8);
        composer.addView(send, sendParams);
        send.setOnClickListener(v -> sendMessage(send));
        root.addView(composer);

        setContentView(root);
        timeline.addView(note("Подключаем защищённую ленту…", "Сообщения загружаются и расшифровываются Matrix SDK на устройстве."));

        io.execute(() -> {
            try {
                matrix.observeTimeline(room.id, messages -> {
                    runOnUiThread(() -> renderTimeline(messages));
                    return Unit.INSTANCE;
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (activeRoom == room) {
                        timeline.removeAllViews();
                        timeline.addView(note("Комната не открыта", safe(e.getMessage())));
                    }
                });
            }
        });
    }

    private void renderTimeline(List<MatrixE2eeClient.ChatMessage> messages) {
        if (activeRoom == null || timeline == null) return;
        timeline.removeAllViews();
        if (messages.isEmpty()) {
            timeline.addView(note("Начало переписки", "Отправь первое сообщение."));
        }
        for (MatrixE2eeClient.ChatMessage message : messages) {
            boolean mine = message.isOwn();
            LinearLayout bubbleColumn = col(3);
            if (!mine) {
                bubbleColumn.addView(text(message.getSender(), 10, MUTED, true));
            }
            TextView bubble = text(message.getBody(), 14, mine ? Color.WHITE : INK, false);
            bubble.setLineSpacing(dp(3), 1f);
            bubble.setPadding(dp(13), dp(10), dp(13), dp(10));
            bubble.setBackground(shape(mine ? ACCENT : Color.WHITE, mine ? ACCENT : LINE, 15));
            bubbleColumn.addView(bubble);

            LinearLayout row = new LinearLayout(this);
            row.setGravity(mine ? Gravity.RIGHT : Gravity.LEFT);
            row.addView(bubbleColumn, new LinearLayout.LayoutParams(-2, -2));
            timeline.addView(row);
        }
        timelineScroll.post(() -> timelineScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage(TextView sendButton) {
        if (matrix == null || activeRoom == null || messageInput == null) return;
        String body = messageInput.getText().toString().trim();
        if (body.isEmpty()) return;
        String roomId = activeRoom.id;
        messageInput.setText("");
        sendButton.setEnabled(false);
        io.execute(() -> {
            try {
                matrix.sendEncryptedText(roomId, body);
                runOnUiThread(() -> toast("Зашифрованное сообщение отправлено"));
            } catch (Exception e) {
                runOnUiThread(() -> toast("Не удалось отправить: " + safe(e.getMessage())));
            } finally {
                runOnUiThread(() -> sendButton.setEnabled(true));
            }
        });
    }

    private void backToChats() {
        activeRoom = null;
        io.execute(() -> {
            try { if (matrix != null) matrix.closeTimeline(); } catch (Exception ignored) {}
            runOnUiThread(this::showChats);
        });
    }

    private void logout() {
        showLoading("Выходим из аккаунта…");
        io.execute(() -> {
            try {
                if (matrix != null) matrix.logout();
            } catch (Exception ignored) {
                if (matrix != null) matrix.close();
            }
            matrix = null;
            userId = null;
            getPreferences(0).edit().remove("user").apply();
            runOnUiThread(this::showLogin);
        });
    }

    private void showLoading(String message) {
        root = base();
        LinearLayout box = col(14);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.addView(text("NEXORA", 14, ACCENT, true));
        box.addView(text(message, 15, INK, false));
        root.addView(box, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private LinearLayout base() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BG);
        return layout;
    }

    private LinearLayout col(int gap) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable divider = new GradientDrawable();
        divider.setColor(Color.TRANSPARENT);
        divider.setSize(dp(1), dp(gap));
        layout.setDividerDrawable(divider);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return layout;
    }

    private View header(String title, String subtitle) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(12));
        TextView mark = text("N", 23, ACCENT, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(0xFFEDE9FF, 0xFFEDE9FF, 13));
        header.addView(mark, lp(44, 44));
        LinearLayout labels = col(2);
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(title, 18, INK, true));
        labels.addView(text(subtitle, 11, MUTED, false));
        header.addView(labels);
        return header;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private EditText field(String hint, boolean secret) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(14);
        edit.setTextColor(INK);
        edit.setHintTextColor(MUTED);
        edit.setHint(hint);
        edit.setPadding(dp(14), 0, dp(14), 0);
        edit.setBackground(shape(Color.WHITE, LINE, 14));
        if (secret) edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return edit;
    }

    private TextView button(String label, int background, int foreground) {
        TextView view = text(label, 14, foreground, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(shape(background, background == Color.WHITE ? LINE : background, 15));
        return view;
    }

    private View note(String title, String body) {
        LinearLayout card = col(7);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(shape(Color.WHITE, LINE, 15));
        card.addView(text(title, 14, INK, true));
        TextView description = text(body, 12, MUTED, false);
        description.setLineSpacing(dp(3), 1f);
        card.addView(description);
        return card;
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != color) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width),
                height < 0 ? height : dp(height));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String initial(String value) {
        return value == null || value.isEmpty() ? "N" : value.substring(0, 1).toUpperCase();
    }

    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) return "неизвестная ошибка";
        return value.length() > 140 ? value.substring(0, 140) : value;
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }

    @Override public void onBackPressed() {
        if (activeRoom != null) backToChats();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        io.execute(() -> {
            try { if (matrix != null) matrix.close(); } catch (Exception ignored) {}
        });
        io.shutdown();
        super.onDestroy();
    }
}
