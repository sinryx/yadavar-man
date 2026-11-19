package com.yadavar.man;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmScheduler {
    static final int BASE = 1000, SNZ = 5000;

    static PendingIntent pi(Context c, int code, String item) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("com.yadavar.man.FIRE." + code);
        if (item != null) i.putExtra("item", item);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void set(Context c, int code, long at, String item) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = pi(c, code, item);
        Intent show = new Intent(c, MainActivity.class);
        PendingIntent sp = PendingIntent.getActivity(c, 1, show, PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            } else {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, sp), p);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
    }

    static void cancel(Context c, int code) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = pi(c, code, null);
        am.cancel(p);
        p.cancel();
    }

    /** Replace every scheduled alarm (incl. native snoozes) with this list. */
    static synchronized void scheduleAll(Context c, JSONArray items) {
        JSONArray old = AlarmStore.getArr(c, "codes");
        for (int i = 0; i < old.length(); i++) cancel(c, old.optInt(i));
        JSONArray codes = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            long at = it.optLong("at");
            if (at <= now) continue;
            int code = BASE + i;
            set(c, code, at, it.toString());
            codes.put(code);
        }
        AlarmStore.putArr(c, "codes", codes);
        AlarmStore.putArr(c, "items", items);
    }

    static synchronized void snooze(Context c, JSONObject item, long at) {
        try {
            JSONArray codes = AlarmStore.getArr(c, "codes");
            int code = SNZ + (Math.abs(item.optString("k").hashCode()) % 3000);
            item.put("at", at);
            set(c, code, at, item.toString());
            codes.put(code);
            AlarmStore.putArr(c, "codes", codes);
            JSONArray items = AlarmStore.getArr(c, "items");
            items.put(item);
            AlarmStore.putArr(c, "items", items);
        } catch (Exception ignored) {}
    }

    static void restore(Context c) {
        JSONArray items = AlarmStore.getArr(c, "items");
        JSONArray keep = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it != null && it.optLong("at") > now) keep.put(it);
        }
        scheduleAll(c, keep);
    }
}
