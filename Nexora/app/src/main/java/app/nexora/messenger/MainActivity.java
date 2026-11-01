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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 246, 250);
    private static final int INK = Color.rgb(28, 29, 43);
    private static final int MUTED = Color.rgb(119, 122, 143);
    private static final int ACCENT = Color.rgb(105, 75, 205);
    private static final int LINE = Color.rgb(230, 230, 239);

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService syncIo = Executors.newSingleThreadExecutor();
    private final List<Room> rooms = new ArrayList<>();
    private LinearLayout root;
    private String homeserver;
    private String accessToken;
    private String userId;
    private String activeRoomId;
    private String activeRoomName;
    private volatile boolean syncing;
    private String syncToken;
    private EditText messageInput;
    private LinearLayout timeline;
    private TextView statusView;

    private static final class Room {
        String id;
        String name;
        String preview;
        Room(String id, String name, String preview) {
            this.id = id; this.name = name; this.preview = preview;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        homeserver = getPreferences(0).getString("homeserver", "https://matrix.org");
        try {
            accessToken = SecureSessionStore.read(this, "token");
            userId = getPreferences(0).getString("user", null);
            if (accessToken == null) {
                String legacy = getPreferences(0).getString("token", null);
                if (legacy != null) {
                    SecureSessionStore.write(this, "token", legacy);
                    getPreferences(0).edit().remove("token").apply();
                    accessToken = legacy;
                }
            }
        } catch (Exception e) {
            getPreferences(0).edit().remove("token").remove("user").apply();
            accessToken = null;
            userId = null;
        }
        if (accessToken != null && userId != null) loadRooms();
        else showLogin();
    }

    private void showLogin() {
        root = base();
        LinearLayout page = col(14);
        page.setPadding(dp(24), dp(32), dp(24), dp(24));
        TextView logo = text("NEXORA", 14, ACCENT, true);
        page.addView(logo);
        TextView title = text("Твоё общение.\nТвои правила.", 32, INK, true);
        LinearLayout.LayoutParams tl = lp(-1, -2); tl.topMargin = dp(12); page.addView(title, tl);
        TextView hint = text("Войди на свой сервер Matrix. Nexora подключается напрямую к серверу и не хранит пароль.", 14, MUTED, false);
        hint.setLineSpacing(dp(4), 1f); page.addView(hint);
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
        TextView note = text("Регистрация зависит от настроек сервера. Сквозное шифрование пока не подключено — не используй Nexora для секретной переписки.", 12, MUTED, false);
        note.setLineSpacing(dp(3), 1f); page.addView(note);
        login.setOnClickListener(v -> authenticate(server.getText().toString(), username.getText().toString(),
                password.getText().toString(), false));
        register.setOnClickListener(v -> authenticate(server.getText().toString(), username.getText().toString(),
                password.getText().toString(), true));
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void authenticate(String server, String username, String password, boolean register) {
        String base = server.trim().replaceAll("/+$", "");
        if (!base.startsWith("https://") && !base.startsWith("http://")) {
            toast("Укажи адрес сервера с https://"); return;
        }
        if (username.trim().isEmpty() || password.isEmpty()) { toast("Заполни имя и пароль"); return; }
        homeserver = base;
        showLoading(register ? "Создаём аккаунт…" : "Подключаемся…");
        io.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("username", username.trim());
                body.put("password", password);
                JSONObject response;
                if (register) {
                    body.put("auth", new JSONObject());
                    response = request("POST", homeserver + "/_matrix/client/v3/register", null, body);
                } else {
                    JSONObject identifier = new JSONObject();
                    identifier.put("type", "m.id.user");
                    identifier.put("user", username.trim());
                    body.put("identifier", identifier);
                    body.put("type", "m.login.password");
                    response = request("POST", homeserver + "/_matrix/client/v3/login", null, body);
                }
                String token = response.optString("access_token", "");
                String id = response.optString("user_id", "");
                if (token.isEmpty() || id.isEmpty()) throw new Exception("Сервер не вернул сессию.");
                SecureSessionStore.write(this, "token", token);
                accessToken = token; userId = id;
                getPreferences(0).edit().putString("homeserver", homeserver)
                        .putString("user", id).apply();
                runOnUiThread(this::loadRooms);
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "Не удалось подключиться" : e.getMessage();
                runOnUiThread(() -> { showLogin(); toast(msg); });
            }
        });
    }

    private void loadRooms() {
        showLoading("Загружаем чаты…");
        io.execute(() -> {
            try {
                JSONObject sync = request("GET", homeserver + "/_matrix/client/v3/sync?timeout=0", accessToken, null);
                syncToken = sync.optString("next_batch", null);
                parseRooms(sync);
                runOnUiThread(this::showChats);
                startSync();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("401")) logout();
                    else { showLogin(); toast("Ошибка подключения: " + safe(e.getMessage())); }
                });
            }
        });
    }

    private void startSync() {
        if (syncing) return;
        syncing = true;
        syncIo.execute(() -> {
            String next = syncToken == null ? "" : syncToken;
            try {
                while (syncing && accessToken != null) {
                    String url = homeserver + "/_matrix/client/v3/sync?timeout=25000&since=" + enc(next);
                    JSONObject data = request("GET", url, accessToken, null);
                    next = data.optString("next_batch", next);
                    syncToken = next;
                    parseRooms(data);
                    final String room = activeRoomId;
                    runOnUiThread(() -> {
                        if (accessToken != null && activeRoomId == null) refreshRoomList();
                        else if (room != null && room.equals(activeRoomId)) refreshTimeline();
                    });
                }
            } catch (Exception ignored) {
                syncing = false;
                if (accessToken != null) runOnUiThread(() -> toast("Соединение прервано. Открой список чатов для повтора."));
            }
        });
    }

    private void parseRooms(JSONObject sync) {
        JSONObject joined = sync.optJSONObject("rooms");
        if (joined == null) return;
        JSONObject join = joined.optJSONObject("join");
        if (join == null) return;
        Iterator<String> keys = join.keys();
        synchronized (rooms) {
            while (keys.hasNext()) {
                String id = keys.next();
                JSONObject data = join.optJSONObject(id);
                String name = id;
                JSONObject state = data == null ? null : data.optJSONObject("state");
                JSONArray events = state == null ? null : state.optJSONArray("events");
                if (events != null) for (int i=0;i<events.length();i++) {
                    JSONObject ev = events.optJSONObject(i);
                    if (ev != null && "m.room.name".equals(ev.optString("type"))) {
                        name = ev.optJSONObject("content") == null ? name : ev.optJSONObject("content").optString("name", name);
                    }
                }
                boolean found=false;
                for (Room r : rooms) if (r.id.equals(id)) { r.name=name; found=true; break; }
                if (!found) rooms.add(new Room(id, name, "Открыть переписку"));
            }
        }
    }

    private void showChats() {
        root = base();
        root.addView(header("Сообщения", userId == null ? "Nexora" : userId));
        LinearLayout body = col(10); body.setPadding(dp(18), dp(12), dp(18), dp(12));
        TextView add = button("＋  Начать переписку", ACCENT, Color.WHITE);
        add.setOnClickListener(v -> showCreateRoom());
        body.addView(add, lp(-1, 50));
        statusView = text("Тестовая версия · сквозное шифрование пока не подключено", 12, MUTED, false);
        body.addView(statusView);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = col(8); list.setPadding(0, dp(4), 0, dp(12));
        synchronized (rooms) {
            for (Room room : rooms) {
                LinearLayout card = new LinearLayout(this);
                card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14), dp(12), dp(14), dp(12));
                card.setBackground(shape(Color.WHITE, LINE, 15));
                TextView avatar = text(initial(room.name), 18, ACCENT, true);
                avatar.setGravity(Gravity.CENTER); avatar.setBackground(shape(0xFFEDE9FF, 0xFFEDE9FF, 18));
                card.addView(avatar, lp(46,46));
                LinearLayout labels=col(3); labels.setPadding(dp(12),0,0,0);
                labels.addView(text(room.name,15,INK,true));
                labels.addView(text(room.preview,12,MUTED,false));
                card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
                card.setOnClickListener(v -> openRoom(room));
                list.addView(card, lp(-1,-2));
            }
        }
        if (list.getChildCount()==0) list.addView(note("Пока нет чатов","Создай комнату или попроси друга пригласить тебя по Matrix ID."));
        scroll.addView(list); body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView logout = text("Выйти из аккаунта", 13, MUTED, true);
        logout.setGravity(Gravity.CENTER); logout.setPadding(0,dp(12),0,dp(12));
        logout.setOnClickListener(v -> logout()); body.addView(logout);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private void refreshRoomList() {
        if (activeRoomId == null) showChats();
    }

    private void showCreateRoom() {
        LinearLayout form=col(10); form.setPadding(dp(8),dp(8),dp(8),dp(8));
        EditText name=field("Название комнаты",false);
        EditText invite=field("Matrix ID участника (необязательно)",false);
        form.addView(name); form.addView(invite);
        new AlertDialog.Builder(this).setTitle("Новая переписка").setView(form)
                .setNegativeButton("Отмена",null).setPositiveButton("Создать",(d,w)->{
                    String roomName=name.getText().toString().trim();
                    String member=invite.getText().toString().trim();
                    if(roomName.isEmpty()){toast("Укажи название");return;}
                    io.execute(()->{
                        try{
                            JSONObject body=new JSONObject(); body.put("name",roomName); body.put("preset","private_chat");
                            body.put("visibility","private"); body.put("invite",new JSONArray());
                            JSONObject created=request("POST",homeserver+"/_matrix/client/v3/createRoom",accessToken,body);
                            String id=created.getString("room_id");
                            if(!member.isEmpty()){
                                JSONObject inviteBody=new JSONObject(); inviteBody.put("user_id",member);
                                request("POST",homeserver+"/_matrix/client/v3/rooms/"+enc(id)+"/invite",accessToken,inviteBody);
                            }
                            runOnUiThread(()->{toast("Комната создана"); loadRooms();});
                        }catch(Exception e){runOnUiThread(()->toast("Не удалось создать: "+safe(e.getMessage())));}
                    });
                }).show();
    }

    private void openRoom(Room room) {
        activeRoomId=room.id; activeRoomName=room.name;
        root=base();
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),dp(10),dp(16),dp(10));
        TextView back=text("‹",32,ACCENT,true); back.setOnClickListener(v->{activeRoomId=null;showChats();});
        top.addView(back,lp(40,48));
        LinearLayout titleCol=col(2);
        titleCol.addView(text(room.name,17,INK,true));
        titleCol.addView(text("Обычная Matrix-комната · без E2EE",11,MUTED,false));
        top.addView(titleCol,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(top);
        ScrollView scroll=new ScrollView(this); timeline=col(8); timeline.setPadding(dp(14),dp(12),dp(14),dp(12));
        scroll.addView(timeline); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer=new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(12),dp(8),dp(12),dp(8)); composer.setBackgroundColor(Color.WHITE);
        messageInput=field("Сообщение",false); composer.addView(messageInput,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView send=button("➤",ACCENT,Color.WHITE); LinearLayout.LayoutParams sp=lp(52,48);sp.leftMargin=dp(8);composer.addView(send,sp);
        send.setOnClickListener(v->sendMessage());
        root.addView(composer); setContentView(root); refreshTimeline();
    }

    private void refreshTimeline() {
        if (activeRoomId == null || timeline == null) return;
        final String room=activeRoomId;
        io.execute(()->{
            try{
                JSONObject data=request("GET",homeserver+"/_matrix/client/v3/rooms/"+enc(room)+"/messages?dir=b&limit=50",accessToken,null);
                JSONArray events=data.optJSONArray("chunk"); List<String[]> messages=new ArrayList<>();
                if(events!=null) for(int i=events.length()-1;i>=0;i--){
                    JSONObject ev=events.optJSONObject(i); if(ev==null)continue;
                    JSONObject content=ev.optJSONObject("content");
                    if("m.room.message".equals(ev.optString("type")) && content!=null){
                        String body=content.optString("body","");
                        String sender=ev.optString("sender","");
                        messages.add(new String[]{sender,body});
                    }
                }
                runOnUiThread(()->{
                    if(!room.equals(activeRoomId)||timeline==null)return;
                    timeline.removeAllViews();
                    if(messages.isEmpty())timeline.addView(note("Начало переписки","Отправь первое сообщение."));
                    for(String[] m:messages){
                        boolean mine=userId.equals(m[0]);
                        TextView bubble=text((mine?"Вы":"")+(mine?"\n":"")+m[1],14,mine?Color.WHITE:INK,false);
                        bubble.setPadding(dp(13),dp(10),dp(13),dp(10));
                        bubble.setBackground(shape(mine?ACCENT:Color.WHITE,mine?ACCENT:LINE,15));
                        LinearLayout row=new LinearLayout(this); row.setGravity(mine?Gravity.RIGHT:Gravity.LEFT);
                        row.addView(bubble,new LinearLayout.LayoutParams(-2,-2)); timeline.addView(row);
                    }
                });
            }catch(Exception e){runOnUiThread(()->toast("Не удалось загрузить сообщения"));}
        });
    }

    private void sendMessage() {
        if(messageInput==null||activeRoomId==null)return;
        String body=messageInput.getText().toString().trim(); if(body.isEmpty())return;
        String room=activeRoomId; messageInput.setText("");
        io.execute(()->{
            try{
                JSONObject content=new JSONObject(); content.put("msgtype","m.text"); content.put("body",body);
                request("PUT",homeserver+"/_matrix/client/v3/rooms/"+enc(room)+"/send/m.room.message/"+UUID.randomUUID(),accessToken,content);
                refreshTimeline();
            }catch(Exception e){runOnUiThread(()->toast("Сообщение не отправлено: "+safe(e.getMessage())));}
        });
    }

    private JSONObject request(String method,String address,String token,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(35000);
        c.setRequestProperty("Accept","application/json");
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
            try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode(); InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        StringBuilder result=new StringBuilder();
        if(stream!=null)try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
            String line;while((line=reader.readLine())!=null)result.append(line);
        }
        c.disconnect();
        if(code<200||code>=300){
            String error="HTTP "+code;
            try{error=new JSONObject(result.toString()).optString("error",error);}catch(Exception ignored){}
            throw new Exception(code+" "+error);
        }
        return result.length()==0?new JSONObject():new JSONObject(result.toString());
    }

    private void logout(){
        syncing=false; String token=accessToken;
        io.execute(()->{try{if(token!=null)request("POST",homeserver+"/_matrix/client/v3/logout",token,new JSONObject());}catch(Exception ignored){}});
        accessToken=null;userId=null;activeRoomId=null;rooms.clear();syncToken=null;
        try { SecureSessionStore.clear(this, "token"); } catch (Exception ignored) {}
        getPreferences(0).edit().remove("token").remove("user").apply();showLogin();
    }

    private void showLoading(String message){
        root=base(); LinearLayout box=col(12);box.setGravity(Gravity.CENTER);box.setPadding(dp(24),dp(24),dp(24),dp(24));
        box.addView(text("NEXORA",14,ACCENT,true));box.addView(text(message,16,INK,false));
        root.addView(box,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private LinearLayout base(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setBackgroundColor(BG);return l;}
    private LinearLayout col(int gap){LinearLayout l=new LinearLayout(this);l.setOrientation(1);GradientDrawable d=new GradientDrawable();d.setColor(Color.TRANSPARENT);d.setSize(dp(1),dp(gap));l.setDividerDrawable(d);l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);return l;}
    private View header(String title,String sub){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(18),dp(12),dp(18),dp(12));TextView mark=text("N",23,ACCENT,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(0xFFEDE9FF,0xFFEDE9FF,13));h.addView(mark,lp(44,44));LinearLayout labels=col(2);labels.setPadding(dp(12),0,0,0);labels.addView(text(title,18,INK,true));labels.addView(text(sub,11,MUTED,false));h.addView(labels);return h;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private EditText field(String hint,boolean secret){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);e.setPadding(dp(14),0,dp(14),0);e.setBackground(shape(Color.WHITE,LINE,14));if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);return e;}
    private TextView button(String s,int bg,int fg){TextView t=text(s,14,fg,true);t.setGravity(Gravity.CENTER);t.setBackground(shape(bg,bg==Color.WHITE?LINE:bg,15));return t;}
    private View note(String title,String body){LinearLayout c=col(7);c.setPadding(dp(16),dp(16),dp(16),dp(16));c.setBackground(shape(Color.WHITE,LINE,15));c.addView(text(title,14,INK,true));TextView b=text(body,12,MUTED,false);b.setLineSpacing(dp(3),1f);c.addView(b);return c;}
    private GradientDrawable shape(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=color)d.setStroke(dp(1),stroke);return d;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8").replace("+","%20");}
    private String initial(String s){return s==null||s.isEmpty()?"N":s.substring(0,1).toUpperCase();}
    private String safe(String s){return s==null?"ошибка":s.length()>120?s.substring(0,120):s;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(activeRoomId!=null){activeRoomId=null;showChats();}else super.onBackPressed();}
    @Override protected void onDestroy(){syncing=false;io.shutdownNow();syncIo.shutdownNow();super.onDestroy();}
}
