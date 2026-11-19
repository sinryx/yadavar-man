package com.yadavar.man;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class QuickWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_quick);
            v.setOnClickPendingIntent(R.id.w_bar, pi(ctx, false, 7001));
            v.setOnClickPendingIntent(R.id.w_mic, pi(ctx, true, 7002));
            mgr.updateAppWidget(id, v);
        }
    }

    static PendingIntent pi(Context ctx, boolean mic, int code) {
        Intent i = new Intent(ctx, QuickActivity.class);
        i.putExtra("mic", mic);
        i.setAction(mic ? "com.yadavar.man.QUICK_MIC" : "com.yadavar.man.QUICK");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
