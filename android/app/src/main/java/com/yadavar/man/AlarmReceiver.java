package com.yadavar.man;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String item = intent.getStringExtra("item");
        if (item == null) return;
        Intent s = new Intent(c, AlarmService.class);
        s.setAction(AlarmService.ACTION_START);
        s.putExtra("item", item);
        try { ContextCompat.startForegroundService(c, s); } catch (Exception e) { AlarmService.fallbackNotify(c, item); }
    }
}
