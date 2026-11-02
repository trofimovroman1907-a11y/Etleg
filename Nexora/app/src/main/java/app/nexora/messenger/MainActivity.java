package app.nexora.messenger;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
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
    // The homeserver is intentionally fixed in the app. There is no editable URL field.
    // Replace this constant when the Nexora homeserver is deployed.
    private static final String HOMESERVER_URL = "https://matrix.org";
    private static final String PREF_THEME = "nexora_dark_theme";

    private static final int DARK_BG = Color.rgb(12, 14, 20);
    private static final int DARK_PANEL = Color.rgb(19, 22, 31);
    private static final int DARK_SURFACE = Color.rgb(27, 31, 43);
    private static final int DARK_LINE = Color.rgb(43, 49, 64);
    private static final int DARK_TEXT = Color.rgb(241, 243, 250);
    private static final int DARK_MUTED = Color.rgb(150, 158, 177);
    private static final int DARK_ACCENT = Color.rgb(145, 130, 255);
    private static final int DARK_INCOMING = Color.rgb(31, 36, 49);

    private static final int LIGHT_BG = Color.rgb(246, 247, 251);
    private static final int LIGHT_PANEL = Color.WHITE;
    private static final int LIGHT_SURFACE = Color.rgb(238, 240, 247);
    private static final int LIGHT_LINE = Color.rgb(225, 228, 237);
    private static final int LIGHT_TEXT = Color.rgb(28, 31, 43);
    private static final int LIGHT_MUTED = Color.rgb(111, 119, 139);
    private static final int LIGHT_ACCENT = Color.rgb(101, 84, 220);
    private static final int LIGHT_INCOMING = Color.WHITE;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Room> rooms = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean darkTheme = true;
    private boolean registrationMode = false;
    private int bg, panel, surface, line, ink, muted, accent, incomingBubble;
    private MatrixE2eeClient matrix;
    private String userId;
    private Room activeRoom;
    private LinearLayout root;
    private LinearLayout timeline;
    private ScrollView timelineScroll;
    private EditText messageInput;
    private List<MatrixE2eeClient.ChatMessage> currentMessages = new ArrayList<>();
    private View activeSnackbar;

    private static final class Room {
        final String id;
        final String name;
        Room(String id, String name) { this.id = id; this.name = name; }
    }

    private static final class RegistrationChallenge extends Exception {
        final JSONObject payload;
        final int status;
        RegistrationChallenge(JSONObject payload, int status) {
            super(payload.optString("error", "Требуется дополнительная проверка"));
            this.payload = payload;
            this.status = status;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        darkTheme = getPreferences(0).getBoolean(PREF_THEME, true);
        applyPalette();
        applySystemBars();
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
                    notifyUser("Сессию не удалось восстановить. Войди снова.", false);
                });
            }
        });
    }

    private void applyPalette() {
        if (darkTheme) {
            bg = DARK_BG; panel = DARK_PANEL; surface = DARK_SURFACE; line = DARK_LINE;
            ink = DARK_TEXT; muted = DARK_MUTED; accent = DARK_ACCENT; incomingBubble = DARK_INCOMING;
        } else {
            bg = LIGHT_BG; panel = LIGHT_PANEL; surface = LIGHT_SURFACE; line = LIGHT_LINE;
            ink = LIGHT_TEXT; muted = LIGHT_MUTED; accent = LIGHT_ACCENT; incomingBubble = LIGHT_INCOMING;
        }
    }

    private void applySystemBars() {
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(panel);
        int flags = darkTheme ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        getPreferences(0).edit().putBoolean(PREF_THEME, darkTheme).apply();
        applyPalette();
        applySystemBars();
        if (activeRoom != null) {
            showRoom(activeRoom);
        } else if (matrix != null) {
            showChats();
        } else {
            showLogin();
        }
    }

    private void showLogin() {
        root = base();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = col(0);
        page.setPadding(dp(24), dp(24), dp(24), dp(28));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(brandMark(), lp(48, 48));
        LinearLayout brandLabels = col(2);
        brandLabels.setPadding(dp(12), 0, 0, 0);
        brandLabels.addView(text("NEXORA", 15, ink, true));
        brandLabels.addView(text("ЛИЧНОЕ ПРОСТРАНСТВО", 9, muted, true));
        top.addView(brandLabels, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(themeButton(), lp(-2, 42));
        page.addView(top);

        View spacer = new View(this);
        page.addView(spacer, lp(1, 34));

        TextView eyebrow = pill("ЗАЩИЩЁННЫЕ ПЕРЕПИСКИ", 0x332A947B);
        page.addView(eyebrow, lp(-2, 30));

        TextView title = text(registrationMode ? "Создай свой\nаккаунт." : "Общение,\nкоторое твоё.", 34, ink, true);
        title.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams titleParams = lp(-1, -2);
        titleParams.topMargin = dp(18);
        page.addView(title, titleParams);

        TextView description = text(
                registrationMode
                        ? "Один аккаунт — твои чаты, устройства и ключи шифрования."
                        : "Пространство для личных разговоров. Новые комнаты создаются с включённым сквозным шифрованием.",
                14, muted, false);
        description.setLineSpacing(dp(4), 1.0f);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(10);
        page.addView(description, descriptionParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(shape(surface, surface, 16));
        LinearLayout.LayoutParams tabsParams = lp(-1, 50);
        tabsParams.topMargin = dp(28);
        TextView loginTab = tab("Войти", !registrationMode);
        TextView registerTab = tab("Регистрация", registrationMode);
        tabs.addView(loginTab, new LinearLayout.LayoutParams(0, -1, 1));
        tabs.addView(registerTab, new LinearLayout.LayoutParams(0, -1, 1));
        loginTab.setOnClickListener(v -> { registrationMode = false; showLogin(); });
        registerTab.setOnClickListener(v -> { registrationMode = true; showLogin(); });
        page.addView(tabs, tabsParams);

        TextView usernameLabel = label("ИМЯ ПОЛЬЗОВАТЕЛЯ");
        LinearLayout.LayoutParams labelParams = lp(-1, -2);
        labelParams.topMargin = dp(26);
        page.addView(usernameLabel, labelParams);
        EditText username = field("Например, roman", false);
        LinearLayout.LayoutParams usernameParams = lp(-1, 56);
        usernameParams.topMargin = dp(8);
        page.addView(username, usernameParams);

        TextView passwordLabel = label("ПАРОЛЬ");
        LinearLayout.LayoutParams passwordLabelParams = lp(-1, -2);
        passwordLabelParams.topMargin = dp(18);
        page.addView(passwordLabel, passwordLabelParams);
        EditText password = field("Введи пароль", true);
        LinearLayout.LayoutParams passwordParams = lp(-1, 56);
        passwordParams.topMargin = dp(8);
        page.addView(password, passwordParams);

        final EditText invitation = registrationMode ? field("Код от администратора сервера", false) : null;
        if (registrationMode) {
            TextView invitationLabel = label("КОД ПРИГЛАШЕНИЯ");
            LinearLayout.LayoutParams invitationLabelParams = lp(-1, -2);
            invitationLabelParams.topMargin = dp(18);
            page.addView(invitationLabel, invitationLabelParams);
            LinearLayout.LayoutParams invitationParams = lp(-1, 56);
            invitationParams.topMargin = dp(8);
            page.addView(invitation, invitationParams);
            TextView inviteHint = text("Nexora использует закрытую регистрацию: код нужен, если сервер настроен на приглашения.", 12, muted, false);
            inviteHint.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams inviteHintParams = lp(-1, -2);
            inviteHintParams.topMargin = dp(8);
            page.addView(inviteHint, inviteHintParams);
        }

        TextView primary = button(registrationMode ? "Создать аккаунт" : "Продолжить", accent, Color.WHITE);
        LinearLayout.LayoutParams primaryParams = lp(-1, 56);
        primaryParams.topMargin = dp(24);
        page.addView(primary, primaryParams);
        primary.setOnClickListener(v -> authenticate(
                username.getText().toString(),
                password.getText().toString(),
                registrationMode,
                invitation == null ? "" : invitation.getText().toString()
        ));

        TextView foot = text("MATRIX  ·  E2EE  ·  БЕЗ РЕКЛАМЫ", 10, muted, true);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footParams = lp(-1, -2);
        footParams.topMargin = dp(26);
        page.addView(foot, footParams);

        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        applySystemBars();
    }

    private void authenticate(String username, String password, boolean register, String inviteCode) {
        String name = username.trim();
        String secret = password;
        String token = inviteCode == null ? "" : inviteCode.trim();

        if (name.isEmpty()) {
            notifyUser("Введи имя пользователя.", false);
            return;
        }
        if (secret.isEmpty()) {
            notifyUser("Введи пароль.", false);
            return;
        }
        if (register && secret.length() < 8) {
            notifyUser("Пароль должен содержать не менее 8 символов.", false);
            return;
        }

        hideKeyboard();
        showLoading(register ? "Создаём аккаунт…" : "Подключаемся…");
        io.execute(() -> {
            try {
                if (register) registerAccount(name, secret, token);
                MatrixE2eeClient connected = MatrixE2eeClient.login(
                        this, HOMESERVER_URL, name, secret);
                matrix = connected;
                userId = connected.getUserId();
                runOnUiThread(this::loadRooms);
            } catch (Exception e) {
                String message = friendlyAuthError(e);
                runOnUiThread(() -> {
                    registrationMode = register;
                    showLogin();
                    notifyUser(message, false);
                });
            }
        });
    }

    private void registerAccount(String username, String password, String token) throws Exception {
        JSONObject request = new JSONObject();
        request.put("username", username);
        request.put("password", password);
        request.put("initial_device_display_name", "Nexora Android");

        try {
            sendRegistrationRequest(request);
        } catch (RegistrationChallenge challenge) {
            JSONArray flows = challenge.payload.optJSONArray("flows");
            String session = challenge.payload.optString("session", "");
            if (flows == null || session.isEmpty()) {
                throw new Exception("Сервер запросил проверку, которую приложение не получило.");
            }

            boolean tokenAvailable = false;
            boolean dummyAvailable = false;
            for (int i = 0; i < flows.length(); i++) {
                JSONObject flow = flows.optJSONObject(i);
                JSONArray stages = flow == null ? null : flow.optJSONArray("stages");
                if (stages == null) continue;
                boolean hasToken = false;
                boolean hasDummy = false;
                for (int j = 0; j < stages.length(); j++) {
                    String stage = stages.optString(j);
                    if ("m.login.registration_token".equals(stage)) hasToken = true;
                    if ("m.login.dummy".equals(stage)) hasDummy = true;
                }
                tokenAvailable |= hasToken;
                dummyAvailable |= hasDummy && stages.length() == 1;
            }

            JSONObject auth = new JSONObject();
            auth.put("session", session);
            if (tokenAvailable && !token.isEmpty()) {
                auth.put("type", "m.login.registration_token");
                auth.put("token", token);
            } else if (tokenAvailable) {
                throw new Exception("Для регистрации нужен код приглашения от администратора Nexora.");
            } else if (dummyAvailable) {
                auth.put("type", "m.login.dummy");
            } else {
                throw new Exception("Сервер требует другой способ проверки регистрации. Обратись к администратору.");
            }
            request.put("auth", auth);
            sendRegistrationRequest(request);
        }
    }

    private void sendRegistrationRequest(JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                HOMESERVER_URL + "/_matrix/client/v3/register").openConnection();
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

        if (status == 401 && json.has("flows")) {
            throw new RegistrationChallenge(json, status);
        }
        if (status < 200 || status >= 300) {
            String code = json.optString("errcode", "");
            String message = json.optString("error", "");
            if ("M_USER_IN_USE".equals(code)) throw new Exception("Это имя уже занято. Попробуй другое.");
            if ("M_INVALID_USERNAME".equals(code)) throw new Exception("Имя содержит недопустимые символы.");
            if ("M_FORBIDDEN".equals(code) || status == 403) {
                throw new Exception("Регистрация на сервере закрыта. Нужен активный сервер Nexora и код приглашения.");
            }
            if ("M_INVALID_PARAM".equals(code)) throw new Exception("Проверь введённые данные и код приглашения.");
            throw new Exception(message.isEmpty() ? "Сервер отклонил регистрацию (HTTP " + status + ")." : message);
        }
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        }
    }

    private String friendlyAuthError(Exception error) {
        String raw = error.getMessage();
        if (raw == null || raw.trim().isEmpty()) return "Не удалось выполнить вход. Проверь данные и подключение.";
        String lower = raw.toLowerCase();
        if (lower.contains("invalid username") || lower.contains("user not found") || lower.contains("unknown user")) {
            return "Аккаунт не найден. Проверь имя пользователя.";
        }
        if (lower.contains("invalid password") || lower.contains("wrong password") || lower.contains("forbidden")) {
            return "Не получилось войти. Проверь имя и пароль.";
        }
        if (lower.contains("connect") || lower.contains("timeout") || lower.contains("resolve host")) {
            return "Сервер сейчас недоступен. Проверь подключение к интернету.";
        }
        return safe(raw);
    }

    private void loadRooms() {
        if (matrix == null) {
            showLogin();
            return;
        }
        showLoading("Загружаем чаты…");
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
                    notifyUser("Не удалось обновить список чатов: " + safe(e.getMessage()), false);
                });
            }
        });
    }

    private void showChats() {
        root = base();
        LinearLayout page = col(0);
        page.setPadding(dp(20), dp(18), dp(20), dp(14));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(brandMark(), lp(44, 44));
        LinearLayout brandLabels = col(2);
        brandLabels.setPadding(dp(11), 0, 0, 0);
        brandLabels.addView(text("NEXORA", 15, ink, true));
        brandLabels.addView(text("ТВОИ ПЕРЕПИСКИ", 9, muted, true));
        top.addView(brandLabels, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(themeButton(), lp(-2, 42));
        page.addView(top);

        LinearLayout greeting = col(5);
        LinearLayout.LayoutParams greetingParams = lp(-1, -2);
        greetingParams.topMargin = dp(28);
        page.addView(greeting, greetingParams);
        greeting.addView(text("Привет,", 14, muted, false));
        greeting.addView(text(displayUser(), 25, ink, true));
        greeting.addView(text("Твоё пространство для общения.", 13, muted, false));

        TextView create = button("＋    Новая зашифрованная комната", accent, Color.WHITE);
        LinearLayout.LayoutParams createParams = lp(-1, 56);
        createParams.topMargin = dp(24);
        page.addView(create, createParams);
        create.setOnClickListener(v -> showCreateRoom());

        LinearLayout sectionHead = new LinearLayout(this);
        sectionHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sectionParams = lp(-1, 40);
        sectionParams.topMargin = dp(22);
        page.addView(sectionHead, sectionParams);
        sectionHead.addView(text("ЧАТЫ", 11, muted, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView refresh = text("Обновить  ↻", 12, accent, true);
        sectionHead.addView(refresh);
        refresh.setOnClickListener(v -> loadRooms());

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout list = col(9);
        synchronized (rooms) {
            for (Room room : rooms) {
                LinearLayout card = new LinearLayout(this);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(dp(13), dp(13), dp(14), dp(13));
                card.setBackground(ripple(surface, 18));
                TextView avatar = text(initial(room.name), 17, accent, true);
                avatar.setGravity(Gravity.CENTER);
                avatar.setBackground(shape(withAlpha(accent, 38), withAlpha(accent, 38), 15));
                card.addView(avatar, lp(48, 48));

                LinearLayout labels = col(4);
                labels.setPadding(dp(12), 0, 0, 0);
                labels.addView(text(room.name, 15, ink, true));
                LinearLayout meta = new LinearLayout(this);
                meta.setGravity(Gravity.CENTER_VERTICAL);
                meta.addView(text("●", 7, 0xFF51C49A, true));
                TextView e2ee = text("  Сквозное шифрование", 11, muted, false);
                meta.addView(e2ee);
                labels.addView(meta);
                card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
                TextView arrow = text("›", 26, muted, false);
                card.addView(arrow);
                card.setOnClickListener(v -> openRoom(room));
                list.addView(card, lp(-1, -2));
            }
        }

        if (list.getChildCount() == 0) {
            LinearLayout empty = col(9);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(32), dp(24), dp(32));
            empty.setBackground(shape(panel, line, 20));
            TextView icon = text("✦", 28, accent, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, lp(-1, 42));
            TextView emptyTitle = text("Здесь пока тихо", 16, ink, true);
            emptyTitle.setGravity(Gravity.CENTER);
            empty.addView(emptyTitle);
            TextView emptyText = text("Создай первую комнату и пригласи собеседника по Matrix ID.", 12, muted, false);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setLineSpacing(dp(3), 1f);
            empty.addView(emptyText);
            list.addView(empty, lp(-1, -2));
        }

        scroll.addView(list);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, 0, 1);
        listParams.topMargin = dp(4);
        page.addView(scroll, listParams);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(14), 0, 0);
        TextView account = text(userId == null ? "Аккаунт" : userId, 11, muted, false);
        bottom.addView(account, new LinearLayout.LayoutParams(0, -2, 1));
        TextView logout = text("Выйти", 12, muted, true);
        bottom.addView(logout);
        logout.setOnClickListener(v -> logout());
        page.addView(bottom);

        root.addView(page, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);
        applySystemBars();
    }

    private void showCreateRoom() {
        Dialog dialog = new Dialog(this);
        LinearLayout box = col(12);
        box.setPadding(dp(22), dp(22), dp(22), dp(20));
        box.setBackground(shape(panel, line, 22));
        box.addView(text("Новая комната", 20, ink, true));
        box.addView(text("Переписка будет создана с E2EE.", 12, muted, false));
        EditText name = field("Название комнаты", false);
        box.addView(name, lp(-1, 54));
        EditText invite = field("Matrix ID участника (необязательно)", false);
        box.addView(invite, lp(-1, 54));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView cancel = button("Отмена", surface, ink);
        TextView create = button("Создать", accent, Color.WHITE);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, dp(50), 1);
        actions.addView(cancel, actionLp);
        LinearLayout.LayoutParams createLp = new LinearLayout.LayoutParams(0, dp(50), 1);
        createLp.leftMargin = dp(8);
        actions.addView(create, createLp);
        box.addView(actions);
        cancel.setOnClickListener(v -> dialog.dismiss());
        create.setOnClickListener(v -> {
            String roomName = name.getText().toString().trim();
            String member = invite.getText().toString().trim();
            if (roomName.isEmpty()) {
                notifyUser("Введи название комнаты.", false);
                return;
            }
            dialog.dismiss();
            io.execute(() -> {
                try {
                    List<String> invitees = member.isEmpty()
                            ? Collections.emptyList() : Collections.singletonList(member);
                    String roomId = matrix.createEncryptedPrivateRoom(roomName, invitees);
                    Room room = new Room(roomId, roomName);
                    synchronized (rooms) { rooms.add(0, room); }
                    runOnUiThread(() -> openRoom(room));
                } catch (Exception e) {
                    runOnUiThread(() -> notifyUser("Не удалось создать комнату: " + safe(e.getMessage()), false));
                }
            });
        });

        dialog.setContentView(box);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = getResources().getDisplayMetrics().widthPixels - dp(36);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.62f;
            window.setAttributes(params);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(36),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void openRoom(Room room) {
        activeRoom = room;
        currentMessages = new ArrayList<>();
        showRoom(room);
        io.execute(() -> {
            try {
                matrix.observeTimeline(room.id, messages -> {
                    runOnUiThread(() -> {
                        if (activeRoom == room) {
                            currentMessages = messages;
                            renderTimeline(messages);
                        }
                    });
                    return Unit.INSTANCE;
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (activeRoom == room) {
                        timeline.removeAllViews();
                        timeline.addView(note("Не удалось открыть комнату", safe(e.getMessage())));
                    }
                });
            }
        });
    }

    private void showRoom(Room room) {
        root = base();
        LinearLayout page = col(0);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14), dp(12), dp(16), dp(12));
        TextView back = text("‹", 31, ink, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(surface, 14));
        top.addView(back, lp(42, 42));
        back.setOnClickListener(v -> backToChats());

        TextView avatar = text(initial(room.name), 15, accent, true);
        avatar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams avatarParams = lp(42, 42);
        avatarParams.leftMargin = dp(9);
        top.addView(avatar, avatarParams);
        avatar.setBackground(shape(withAlpha(accent, 38), withAlpha(accent, 38), 14));

        LinearLayout labels = col(3);
        labels.setPadding(dp(10), 0, 0, 0);
        labels.addView(text(room.name, 15, ink, true));
        LinearLayout secure = new LinearLayout(this);
        secure.setGravity(Gravity.CENTER_VERTICAL);
        secure.addView(text("●", 7, 0xFF51C49A, true));
        secure.addView(text("  E2EE активно", 10, muted, false));
        labels.addView(secure);
        top.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(themeButton(), lp(-2, 40));
        page.addView(top);
        page.addView(separator());

        timelineScroll = new ScrollView(this);
        timelineScroll.setFillViewport(true);
        timeline = col(10);
        timeline.setPadding(dp(15), dp(16), dp(15), dp(16));
        timelineScroll.addView(timeline);
        page.addView(timelineScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderTimeline(currentMessages);

        LinearLayout composerArea = new LinearLayout(this);
        composerArea.setPadding(dp(12), dp(10), dp(12), dp(12));
        composerArea.setBackgroundColor(panel);
        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        messageInput = field("Написать сообщение…", false);
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, dp(50), 1));
        TextView send = button("↑", accent, Color.WHITE);
        LinearLayout.LayoutParams sendParams = lp(50, 50);
        sendParams.leftMargin = dp(9);
        composer.addView(send, sendParams);
        send.setOnClickListener(v -> sendMessage(send));
        composerArea.addView(composer, lp(-1, -2));
        page.addView(composerArea);

        root.addView(page, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);
        applySystemBars();
    }

    private void renderTimeline(List<MatrixE2eeClient.ChatMessage> messages) {
        if (activeRoom == null || timeline == null) return;
        timeline.removeAllViews();
        if (messages == null || messages.isEmpty()) {
            LinearLayout empty = col(7);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(38), dp(18), dp(30));
            TextView icon = text("✦", 25, accent, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, lp(-1, 36));
            TextView title = text("Начало переписки", 15, ink, true);
            title.setGravity(Gravity.CENTER);
            empty.addView(title);
            TextView sub = text("Все сообщения в этой комнате защищены сквозным шифрованием.", 12, muted, false);
            sub.setGravity(Gravity.CENTER);
            sub.setLineSpacing(dp(3), 1f);
            empty.addView(sub);
            timeline.addView(empty);
            return;
        }

        for (MatrixE2eeClient.ChatMessage message : messages) {
            boolean mine = message.isOwn();
            LinearLayout column = col(4);
            if (!mine) {
                String sender = message.getSender();
                String shortName = sender == null ? "Участник" : sender;
                int colon = shortName.indexOf(':');
                if (colon > 0) shortName = shortName.substring(0, colon);
                if (shortName.startsWith("@")) shortName = shortName.substring(1);
                column.addView(text(shortName, 10, muted, true));
            }
            TextView bubble = text(message.getBody(), 14, mine ? Color.WHITE : ink, false);
            bubble.setLineSpacing(dp(3), 1.03f);
            bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
            bubble.setBackground(shape(mine ? accent : incomingBubble, mine ? accent : line, 17));
            column.addView(bubble);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(mine ? Gravity.RIGHT : Gravity.LEFT);
            row.addView(column, new LinearLayout.LayoutParams(-2, -2));
            timeline.addView(row);
        }
        if (timelineScroll != null) timelineScroll.post(() -> timelineScroll.fullScroll(View.FOCUS_DOWN));
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
                runOnUiThread(() -> notifyUser("Сообщение отправлено.", true));
            } catch (Exception e) {
                runOnUiThread(() -> notifyUser("Не удалось отправить: " + safe(e.getMessage()), false));
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
        showLoading("Завершаем сеанс…");
        io.execute(() -> {
            try {
                if (matrix != null) matrix.logout();
            } catch (Exception ignored) {
                if (matrix != null) matrix.close();
            }
            matrix = null;
            userId = null;
            runOnUiThread(() -> {
                registrationMode = false;
                showLogin();
                notifyUser("Ты вышел из аккаунта.", true);
            });
        });
    }

    private void showLoading(String message) {
        root = base();
        LinearLayout box = col(12);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.addView(brandMark(), lp(58, 58));
        box.addView(text("NEXORA", 14, ink, true));
        TextView loading = text(message, 13, muted, false);
        loading.setGravity(Gravity.CENTER);
        box.addView(loading);
        root.addView(box, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        applySystemBars();
    }

    private LinearLayout base() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(bg);
        return layout;
    }

    private LinearLayout col(int gap) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        if (gap > 0) {
            GradientDrawable divider = new GradientDrawable();
            divider.setColor(Color.TRANSPARENT);
            divider.setSize(dp(1), dp(gap));
            layout.setDividerDrawable(divider);
            layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        }
        return layout;
    }

    private View brandMark() {
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(shape(accent, accent, 16));
        TextView n = text("N", 24, Color.WHITE, true);
        n.setGravity(Gravity.CENTER);
        badge.addView(n, new FrameLayout.LayoutParams(-1, -1));
        return badge;
    }

    private TextView themeButton() {
        TextView toggle = text(darkTheme ? "☼  Светлая" : "☾  Тёмная", 11, ink, true);
        toggle.setGravity(Gravity.CENTER);
        toggle.setPadding(dp(12), 0, dp(12), 0);
        toggle.setBackground(ripple(surface, 18));
        toggle.setOnClickListener(v -> toggleTheme());
        return toggle;
    }

    private TextView label(String value) {
        return text(value, 10, muted, true);
    }

    private TextView pill(String value, int background) {
        TextView view = text(value, 9, accent, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(shape(withAlpha(accent, 34), withAlpha(accent, 34), 20));
        return view;
    }

    private TextView tab(String value, boolean selected) {
        TextView view = text(value, 13, selected ? ink : muted, selected);
        view.setGravity(Gravity.CENTER);
        view.setBackground(selected ? shape(panel, line, 13) : shape(Color.TRANSPARENT, Color.TRANSPARENT, 13));
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(true);
        return view;
    }

    private EditText field(String hint, boolean secret) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(14);
        edit.setTextColor(ink);
        edit.setHintTextColor(muted);
        edit.setHint(hint);
        edit.setPadding(dp(16), 0, dp(16), 0);
        edit.setBackground(ripple(surface, 15));
        edit.setImeOptions(secret ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
        if (secret) edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        return edit;
    }

    private TextView button(String label, int background, int foreground) {
        TextView view = text(label, 14, foreground, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(ripple(background, 16));
        return view;
    }

    private View note(String title, String body) {
        LinearLayout card = col(7);
        card.setPadding(dp(15), dp(15), dp(15), dp(15));
        card.setBackground(shape(panel, line, 17));
        card.addView(text(title, 14, ink, true));
        TextView description = text(body, 12, muted, false);
        description.setLineSpacing(dp(3), 1f);
        card.addView(description);
        return card;
    }

    private View separator() {
        View lineView = new View(this);
        lineView.setBackgroundColor(line);
        return lineView;
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != color && stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private RippleDrawable ripple(int color, int radius) {
        return new RippleDrawable(
                ColorStateList.valueOf(withAlpha(ink, darkTheme ? 36 : 22)),
                shape(color, color, radius),
                null
        );
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width),
                height < 0 ? height : dp(height));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String displayUser() {
        if (userId == null) return "с возвращением";
        String local = userId;
        int colon = local.indexOf(':');
        if (colon > 0) local = local.substring(0, colon);
        if (local.startsWith("@")) local = local.substring(1);
        return local;
    }

    private String initial(String value) {
        return value == null || value.isEmpty() ? "N" : value.substring(0, 1).toUpperCase();
    }

    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) return "неизвестная ошибка";
        return value.length() > 140 ? value.substring(0, 140) : value;
    }

    // Branded in-app snackbar. This deliberately replaces Android system Toasts.
    private void notifyUser(String message, boolean success) {
        FrameLayout content = findViewById(android.R.id.content);
        if (content == null) return;
        if (activeSnackbar != null && activeSnackbar.getParent() instanceof ViewGroup) {
            ((ViewGroup) activeSnackbar.getParent()).removeView(activeSnackbar);
        }

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(15), dp(12), dp(17), dp(12));
        bar.setBackground(shape(darkTheme ? DARK_SURFACE : LIGHT_TEXT,
                darkTheme ? DARK_LINE : LIGHT_TEXT, 18));
        bar.setElevation(dp(8));

        TextView icon = text(success ? "✓" : "!", 14,
                success ? 0xFF6DDBB0 : 0xFFFFB36E, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(withAlpha(success ? 0xFF6DDBB0 : 0xFFFFB36E, 34),
                withAlpha(success ? 0xFF6DDBB0 : 0xFFFFB36E, 34), 50));
        bar.addView(icon, lp(28, 28));

        TextView label = text(message, 12, Color.WHITE, true);
        label.setMaxLines(3);
        label.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(10);
        bar.addView(label, labelParams);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                -1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.leftMargin = dp(16);
        params.rightMargin = dp(16);
        params.bottomMargin = dp(24);
        content.addView(bar, params);
        activeSnackbar = bar;
        bar.setAlpha(0f);
        bar.setTranslationY(dp(16));
        bar.animate().alpha(1f).translationY(0).setDuration(180).start();

        mainHandler.postDelayed(() -> {
            if (bar.getParent() instanceof ViewGroup) {
                bar.animate().alpha(0f).translationY(dp(12)).setDuration(160).withEndAction(() -> {
                    if (bar.getParent() instanceof ViewGroup) {
                        ((ViewGroup) bar.getParent()).removeView(bar);
                    }
                    if (activeSnackbar == bar) activeSnackbar = null;
                }).start();
            }
        }, 2800);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View focused = getCurrentFocus();
            if (manager != null && focused != null) manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void showLoadingAfterAuth(String message) {
        runOnUiThread(() -> showLoading(message));
    }

    private void toast(String message) {
        notifyUser(message, false);
    }

    private void logoutAndShowLogin() {
        matrix = null;
        userId = null;
        showLogin();
    }

    @Override public void onBackPressed() {
        if (activeRoom != null) backToChats();
        else if (registrationMode) {
            registrationMode = false;
            showLogin();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        io.execute(() -> {
            try { if (matrix != null) matrix.close(); } catch (Exception ignored) {}
        });
        io.shutdown();
        super.onDestroy();
    }
}
