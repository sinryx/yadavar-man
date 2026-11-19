package com.yadavar.man;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.CapConfig;
import java.lang.reflect.Field;

public class QuickActivity extends BridgeActivity {
    static volatile boolean listening = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmPlugin.class);
        boolean mic = getIntent() != null && (getIntent().getBooleanExtra("mic", false) || "com.yadavar.man.QUICK_MIC".equals(getIntent().getAction()));
        try {
            CapConfig c = CapConfig.loadDefault(this);
            Field f = CapConfig.class.getDeclaredField("startPath");
            f.setAccessible(true);
            f.set(c, "/index.html#quick" + (mic ? "-mic" : ""));
            try {
                Field bg = CapConfig.class.getDeclaredField("backgroundColor");
                bg.setAccessible(true);
                bg.set(c, "#00000000");
            } catch (Exception ignored) {}
            this.config = c;
        } catch (Exception ignored) {}
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        try {
            View wv = getBridge().getWebView();
            wv.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup p = (ViewGroup) wv.getParent();
            if (p != null) p.setBackgroundColor(Color.TRANSPARENT);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!listening && !isChangingConfigurations()) finish();
    }
}
