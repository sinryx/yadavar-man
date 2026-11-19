package com.yadavar.man;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import java.io.File;
import org.json.JSONObject;

public class AlarmService extends Service {
    static final String ACTION_START = "start", ACTION_STOP = "stop", ACTION_OPENED = "opened";
    static final String CH = "ring_v2";
    static final int NID = 4242;

    private MediaPlayer mp;
    private Vibrator vib;
    private final Handler h = new Handler(Looper.getMainLooper());
    private String current;
    private PowerManager.WakeLock wl;

    @Override public IBinder onBind(Intent i) { return null; }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CH) != null) return;
        NotificationChannel ch = new NotificationChannel(CH, "زنگ یادآوری", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("یادآوری کارها");
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setBypassDnd(true);
        nm.createNotificationChannel(ch);
    }

    static PendingIntent openPi(Context c, String item, int code) {
        Intent i = new Intent(c, MainActivity.class);
        i.putExtra("alarmItem", item);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent actPi(Context c, String item, String a, int code) {
        Intent i = new Intent(c, ActionReceiver.class);
        i.setAction("com.yadavar.man.ACT." + a);
        i.putExtra("a", a);
        i.putExtra("item", item);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static Notification build(Context c, String item, boolean fullScreen) {
        ensureChannel(c);
        JSONObject it;
        try { it = new JSONObject(item); } catch (Exception e) { it = new JSONObject(); }
        String type = it.optString("type", "normal");
        int snz = it.optInt("snz", 10);
        String body = it.optString("body", "");
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(0xFF16616B)
                .setContentTitle(it.optString("title", "یادآوری"))
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(!fullScreen)
                .setContentIntent(openPi(c, item, 11));
        if ("reflection".equals(type)) {
            b.addAction(0, "همه‌چیز دارم", actPi(c, item, "all_good", 21));
        } else if ("alarm".equals(type)) {
            b.addAction(0, "خاموش کن", actPi(c, item, "done", 21));
        } else {
            b.addAction(0, "انجام شد", actPi(c, item, "done", 21));
        }
        b.addAction(0, toFa(snz) + " دقیقه بعد", actPi(c, item, "later", 22));
        b.addAction(0, "باز کردن", openPi(c, item, 23));
        if (fullScreen) b.setFullScreenIntent(openPi(c, item, 12), true);
        return b.build();
    }

    static String toFa(int n) {
        String s = String.valueOf(n), d = "۰۱۲۳۴۵۶۷۸۹"; StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) o.append(Character.isDigit(ch) ? d.charAt(ch - '0') : ch);
        return o.toString();
    }

    static void fallbackNotify(Context c, String item) {
        try { c.getSystemService(NotificationManager.class).notify(NID, build(c, item, true)); } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String act = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(act)) { stopAll(); return START_NOT_STICKY; }
        if (ACTION_OPENED.equals(act)) { h.removeCallbacks(autoOpen); if (current == null) stopSelf(); return START_NOT_STICKY; }
        if (!ACTION_START.equals(act)) { if (current == null) stopSelf(); return START_NOT_STICKY; }
        final String item = intent.getStringExtra("item");
        if (item == null) { stopSelf(); return START_NOT_STICKY; }
        stopSound();
        h.removeCallbacksAndMessages(null);
        current = item;
        Notification n = build(this, item, false);
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NID, n);
        } catch (Exception e) {
            fallbackNotify(this, item);
        }
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "yadavar:ring");
            wl.acquire(3 * 60 * 1000L);
        } catch (Exception ignored) {}
        JSONObject it;
        try { it = new JSONObject(item); } catch (Exception e) { it = new JSONObject(); }
        if (it.optBoolean("snd", true)) playSound(AlarmStore.sound(this));
        if (it.optBoolean("vib", true)) vibrate();
        h.postDelayed(autoOpen, AlarmStore.autoOpenSecs(this) * 1000L);
        h.postDelayed(this::stopSound, AlarmStore.ringSecs(this) * 1000L);
        return START_NOT_STICKY;
    }

    private final Runnable autoOpen = new Runnable() {
        @Override public void run() {
            if (current == null) return;
            try {
                NotificationManager nm = getSystemService(NotificationManager.class);
                nm.notify(NID, build(AlarmService.this, current, true));
            } catch (Exception ignored) {}
            boolean canOverlay = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(AlarmService.this);
            if (canOverlay || AlarmPlugin.isForeground()) {
                try {
                    Intent i = new Intent(AlarmService.this, MainActivity.class);
                    i.putExtra("alarmItem", current);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                } catch (Exception ignored) {}
            }
        }
    };

    static Uri soundUri(Context c, String id) {
        if (id == null) id = "raw:r_classic";
        if (id.startsWith("raw:")) {
            int res = c.getResources().getIdentifier(id.substring(4), "raw", c.getPackageName());
            if (res == 0) res = R.raw.r_classic;
            return Uri.parse("android.resource://" + c.getPackageName() + "/" + res);
        }
        if (id.startsWith("file:")) {
            File f = new File(new File(c.getFilesDir(), "sounds"), id.substring(5));
            if (f.exists()) return Uri.fromFile(f);
            return soundUri(c, "raw:r_classic");
        }
        if (id.equals("sys:alarm")) {
            Uri u = RingtoneManager.getActualDefaultRingtoneUri(c, RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            return u != null ? u : soundUri(c, "raw:r_classic");
        }
        return soundUri(c, "raw:r_classic");
    }

    static MediaPlayer makePlayer(Context c, String id, boolean loop) {
        try {
            MediaPlayer m = new MediaPlayer();
            m.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            m.setDataSource(c, soundUri(c, id));
            m.setLooping(loop);
            m.prepare();
            return m;
        } catch (Exception e) {
            if (id != null && !id.equals("raw:r_classic")) return makePlayer(c, "raw:r_classic", loop);
            return null;
        }
    }

    private void playSound(String id) {
        mp = makePlayer(this, id, true);
        if (mp != null) try { mp.start(); } catch (Exception ignored) {}
    }

    private void vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= 31) vib = ((VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
            else vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] pat = {0, 500, 400, 500, 1200};
            if (Build.VERSION.SDK_INT >= 26) vib.vibrate(VibrationEffect.createWaveform(pat, 0),
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            else vib.vibrate(pat, 0);
        } catch (Exception ignored) {}
    }

    private void stopSound() {
        if (mp != null) { try { mp.stop(); mp.release(); } catch (Exception ignored) {} mp = null; }
        if (vib != null) { try { vib.cancel(); } catch (Exception ignored) {} vib = null; }
    }

    private void stopAll() {
        h.removeCallbacksAndMessages(null);
        stopSound();
        current = null;
        if (wl != null && wl.isHeld()) try { wl.release(); } catch (Exception ignored) {}
        try { stopForeground(true); } catch (Exception ignored) {}
        try { getSystemService(NotificationManager.class).cancel(NID); } catch (Exception ignored) {}
        stopSelf();
    }

    @Override public void onDestroy() { h.removeCallbacksAndMessages(null); stopSound(); super.onDestroy(); }
}
