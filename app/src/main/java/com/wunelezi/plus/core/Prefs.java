package com.wunelezi.plus.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Obfuscated preference store. Values are RC4 protected before being persisted.
 */
public final class Prefs {

    private static final String FILE = "w_p";
    private static final String KEY = "w9Q2mL7xR4tZ1cV6";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        this.sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public void put(String k, String v) {
        try {
            sp.edit().putString(enc(k), Rc4Util.encryRC4String(v, KEY, "UTF-8")).apply();
        } catch (Throwable ignored) {
        }
    }

    public String get(String k, String def) {
        try {
            String raw = sp.getString(enc(k), null);
            if (raw == null) return def;
            return Rc4Util.decryRC4(raw, KEY, "UTF-8");
        } catch (Throwable ignored) {
            return def;
        }
    }

    public void putBool(String k, boolean v) {
        put(k, v ? "1" : "0");
    }

    public boolean getBool(String k, boolean def) {
        String v = get(k, def ? "1" : "0");
        return "1".equals(v);
    }

    public void remove(String k) {
        try {
            sp.edit().remove(enc(k)).apply();
        } catch (Throwable ignored) {
        }
    }

    private String enc(String k) {
        try {
            return Rc4Util.encryRC4String(k, KEY, "UTF-8");
        } catch (Throwable t) {
            return k;
        }
    }
}
