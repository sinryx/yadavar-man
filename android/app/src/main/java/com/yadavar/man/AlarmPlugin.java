package com.yadavar.man;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Base64;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.ActivityCallback;
import androidx.activity.result.ActivityResult;
import android.speech.RecognizerIntent;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "Alarm")
public class AlarmPlugin extends Plugin {
    static AlarmPlugin instance;
    static volatile boolean fg = false;
    private MediaPlayer preview;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override public void load() { if (!(getActivity() instanceof QuickActivity)) instance = this; AlarmService.ensureChannel(getContext()); }
    @Override protected void handleOnResume() { if (!(getActivity() instanceof QuickActivity)) fg = true; }
    @Override protected void handleOnPause() { if (!(getActivity() instanceof QuickActivity)) fg = false; stopPreviewInt(); }
    @Override protected void handleOnDestroy() { if (instance == this) instance = null; }

    static boolean isForeground() { return fg; }
    static void emitLaunch() { AlarmPlugin p = instance; if (p != null) p.notifyListeners("launch", new JSObject(), true); }
    static void emitQueue() { AlarmPlugin p = instance; if (p != null) p.notifyListeners("queue", new JSObject(), true); }

    @PluginMethod
    public void scheduleAll(PluginCall call) {
        try {
            JSArray items = call.getArray("items", new JSArray());
            AlarmScheduler.scheduleAll(getContext(), new JSONArray(items.toString()));
            JSObject r = new JSObject(); r.put("count", items.length()); call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage()); }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent s = new Intent(getContext(), AlarmService.class);
        s.setAction(AlarmService.ACTION_STOP);
        try { getContext().startService(s); } catch (Exception ignored) {}
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).clearLockFlags();
        call.resolve();
    }

    @PluginMethod
    public void takeLaunch(PluginCall call) {
        JSObject r = new JSObject();
        String s = AlarmStore.takeLaunch(getContext());
        if (s != null) try { r.put("item", new JSObject(s)); } catch (Exception ignored) {}
        call.resolve(r);
    }

    @PluginMethod
    public void drainQueue(PluginCall call) {
        JSObject r = new JSObject();
        try { r.put("actions", new JSArray(AlarmStore.drain(getContext()).toString())); } catch (Exception e) { r.put("actions", new JSArray()); }
        call.resolve(r);
    }

    @PluginMethod
    public void getConfig(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        r.put("sound", AlarmStore.sound(c));
        r.put("ringSecs", AlarmStore.ringSecs(c));
        r.put("autoOpen", AlarmStore.autoOpenSecs(c));
        try { r.put("version", c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName); } catch (Exception ignored) {}
        r.put("android", Build.VERSION.RELEASE);
        r.put("maker", Build.MANUFACTURER);
        call.resolve(r);
    }

    @PluginMethod
    public void setConfig(PluginCall call) {
        Context c = getContext();
        if (call.hasOption("sound")) AlarmStore.setSound(c, call.getString("sound"));
        if (call.hasOption("ringSecs")) AlarmStore.setRingSecs(c, call.getInt("ringSecs", 60));
        if (call.hasOption("autoOpen")) AlarmStore.setAutoOpenSecs(c, call.getInt("autoOpen", 7));
        call.resolve();
    }

    @PluginMethod
    public void listSounds(PluginCall call) {
        JSArray a = new JSArray();
        File dir = new File(getContext().getFilesDir(), "sounds");
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) a.put("file:" + f.getName());
        JSObject r = new JSObject(); r.put("custom", a); call.resolve(r);
    }

    @PluginMethod
    public void saveSound(PluginCall call) {
        try {
            String name = call.getString("name", "sound").replaceAll("[^\\p{L}\\p{N}._ -]", "_");
            if (name.length() > 60) name = name.substring(name.length() - 60);
            byte[] data = Base64.decode(call.getString("data", ""), Base64.DEFAULT);
            File dir = new File(getContext().getFilesDir(), "sounds");
            dir.mkdirs();
            File f = new File(dir, name);
            try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
            MediaPlayer test = AlarmService.makePlayer(getContext(), "file:" + name, false);
            if (test == null) { f.delete(); call.reject("این فایل صوتی پخش نمی‌شود"); return; }
            test.release();
            JSObject r = new JSObject(); r.put("id", "file:" + name); call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage()); }
    }

    @PluginMethod
    public void deleteSound(PluginCall call) {
        String id = call.getString("id", "");
        if (id.startsWith("file:")) new File(new File(getContext().getFilesDir(), "sounds"), id.substring(5)).delete();
        if (id.equals(AlarmStore.sound(getContext()))) AlarmStore.setSound(getContext(), "raw:r_classic");
        call.resolve();
    }

    private void stopPreviewInt() {
        if (preview != null) { try { preview.stop(); preview.release(); } catch (Exception ignored) {} preview = null; }
    }

    @PluginMethod
    public void preview(PluginCall call) {
        stopPreviewInt();
        preview = AlarmService.makePlayer(getContext(), call.getString("id"), false);
        if (preview != null) { preview.start(); h.removeCallbacksAndMessages(null); h.postDelayed(this::stopPreviewInt, 6000); }
        call.resolve();
    }

    @PluginMethod
    public void stopPreview(PluginCall call) { stopPreviewInt(); call.resolve(); }

    @PluginMethod
    public void perms(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        r.put("notif", Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        r.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
        r.put("overlay", Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c));
        boolean fsi = true;
        if (Build.VERSION.SDK_INT >= 34) fsi = c.getSystemService(NotificationManager.class).canUseFullScreenIntent();
        r.put("fullscreen", fsi);
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        r.put("battery", Build.VERSION.SDK_INT < 23 || pm.isIgnoringBatteryOptimizations(c.getPackageName()));
        r.put("xiaomi", Build.MANUFACTURER.toLowerCase().contains("xiaomi") || Build.MANUFACTURER.toLowerCase().contains("poco") || Build.MANUFACTURER.toLowerCase().contains("redmi"));
        call.resolve(r);
    }

    @PluginMethod
    public void requestNotif(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33) ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        call.resolve();
    }

    private boolean tryStart(Intent i) {
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i); return true; } catch (Exception e) { return false; }
    }

    @PluginMethod
    public void openSetting(PluginCall call) {
        String w = call.getString("which", "app");
        String pkg = getContext().getPackageName();
        Uri u = Uri.parse("package:" + pkg);
        Intent app = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, u);
        boolean ok = false;
        switch (w) {
            case "exact":
                if (Build.VERSION.SDK_INT >= 31) ok = tryStart(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, u));
                break;
            case "overlay":
                if (Build.VERSION.SDK_INT >= 23) ok = tryStart(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, u));
                break;
            case "fullscreen":
                if (Build.VERSION.SDK_INT >= 34) ok = tryStart(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, u));
                break;
            case "battery":
                if (Build.VERSION.SDK_INT >= 23) ok = tryStart(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, u));
                break;
            case "notif":
                if (Build.VERSION.SDK_INT >= 26) {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
                    ok = tryStart(i);
                }
                break;
            case "miui_popup": {
                Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
                i.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
                i.putExtra("extra_pkgname", pkg);
                ok = tryStart(i);
                if (!ok) { Intent j = new Intent("miui.intent.action.APP_PERM_EDITOR"); j.putExtra("extra_pkgname", pkg); ok = tryStart(j); }
                break;
            }
            case "miui_autostart": {
                Intent i = new Intent();
                i.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                ok = tryStart(i);
                break;
            }
        }
        if (!ok) tryStart(app);
        call.resolve();
    }

    @PluginMethod
    public void testRing(PluginCall call) {
        try {
            JSONObject it = new JSONObject();
            it.put("title", "تست آلارم");
            it.put("body", "این یک آلارم آزمایشی است");
            it.put("k", "test|" + System.currentTimeMillis());
            it.put("type", "alarm");
            it.put("snz", 10);
            it.put("test", true);
            long at = System.currentTimeMillis() + call.getInt("secs", 5) * 1000L;
            it.put("at", at);
            AlarmScheduler.set(getContext(), 999, at, it.toString());
            call.resolve();
        } catch (Exception e) { call.reject(e.getMessage()); }
    }

    @PluginMethod
    public void listen(PluginCall call) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        String lang = call.getString("lang", "fa-IR");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "بگو چی رو یادت بندازم…");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        QuickActivity.listening = true;
        try { startActivityForResult(call, i, "voiceResult"); }
        catch (Exception e) { QuickActivity.listening = false; call.reject("no_recognizer"); }
    }

    @ActivityCallback
    private void voiceResult(PluginCall call, ActivityResult result) {
        QuickActivity.listening = false;
        if (call == null) return;
        JSObject r = new JSObject();
        String text = "";
        try {
            Intent d = result.getData();
            if (result.getResultCode() == android.app.Activity.RESULT_OK && d != null) {
                ArrayList<String> m = d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (m != null && !m.isEmpty()) text = m.get(0);
            }
        } catch (Exception ignored) {}
        r.put("text", text);
        call.resolve(r);
    }

    @PluginMethod
    public void closeQuick(PluginCall call) {
        boolean open = call.getBoolean("open", false);
        android.app.Activity a = getActivity();
        call.resolve();
        if (a == null) return;
        if (open) {
            Intent m = new Intent(a, MainActivity.class);
            m.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            m.putExtra("openView", "set");
            a.startActivity(m);
        }
        if (a instanceof QuickActivity) a.runOnUiThread(a::finish);
    }
}
