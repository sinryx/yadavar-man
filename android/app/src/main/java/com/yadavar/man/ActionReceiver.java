package com.yadavar.man;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getStringExtra("a");
        String item = intent.getStringExtra("item");
        Intent stop = new Intent(c, AlarmService.class);
        stop.setAction(AlarmService.ACTION_STOP);
        try { c.startService(stop); } catch (Exception ignored) {}
        try {
            JSONObject it = new JSONObject(item);
            JSONObject q = new JSONObject();
            q.put("k", it.optString("k"));
            q.put("taskId", it.optString("taskId"));
            q.put("a", a);
            q.put("ts", System.currentTimeMillis());
            if ("later".equals(a)) {
                long until = System.currentTimeMillis() + Math.max(1, it.optInt("snz", 10)) * 60000L;
                q.put("until", until);
                AlarmScheduler.snooze(c, it, until);
            }
            AlarmStore.queue(c, q);
        } catch (Exception ignored) {}
        AlarmPlugin.emitQueue();
    }
}
