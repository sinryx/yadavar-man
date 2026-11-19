package com.yadavar.man;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmStore {
    static SharedPreferences p(Context c) { return c.getSharedPreferences("alarm", Context.MODE_PRIVATE); }
    static synchronized JSONArray getArr(Context c, String k) {
        try { return new JSONArray(p(c).getString(k, "[]")); } catch (Exception e) { return new JSONArray(); }
    }
    static synchronized void putArr(Context c, String k, JSONArray a) { p(c).edit().putString(k, a.toString()).commit(); }
    static synchronized void queue(Context c, JSONObject o) { JSONArray a = getArr(c, "queue"); a.put(o); putArr(c, "queue", a); }
    static synchronized JSONArray drain(Context c) { JSONArray a = getArr(c, "queue"); putArr(c, "queue", new JSONArray()); return a; }
    static void setLaunch(Context c, String item) { p(c).edit().putString("launch", item).commit(); }
    static synchronized String takeLaunch(Context c) { String s = p(c).getString("launch", null); p(c).edit().remove("launch").commit(); return s; }
    static String sound(Context c) { return p(c).getString("sound", "raw:r_classic"); }
    static void setSound(Context c, String s) { p(c).edit().putString("sound", s).commit(); }
    static int ringSecs(Context c) { return p(c).getInt("ringSecs", 60); }
    static void setRingSecs(Context c, int s) { p(c).edit().putInt("ringSecs", s).commit(); }
    static int autoOpenSecs(Context c) { return p(c).getInt("autoOpen", 7); }
    static void setAutoOpenSecs(Context c, int s) { p(c).edit().putInt("autoOpen", s).commit(); }
}
