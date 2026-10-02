package com.wunelezi.plus.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Debug;

import java.io.File;
import java.security.MessageDigest;

/**
 * Runtime integrity guard: signature pinning, anti-debug and root awareness.
 */
public final class SecurityGuard {

    private static final String CERT_SHA256 =
            "BC:81:2F:CD:09:86:66:EC:80:20:C5:CF:88:EF:73:EC:45:8E:1F:52:D1:F6:68:88:B7:57:1F:E1:EB:84:60:89";

    private SecurityGuard() {
    }

    public static boolean isSignatureValid(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature[] signatures = info.signatures;
            if (signatures == null || signatures.length == 0) return false;
            for (Signature signature : signatures) {
                if (CERT_SHA256.equalsIgnoreCase(sha256(signature.toByteArray()))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isDebuggable(Context context) {
        try {
            return (context.getApplicationInfo().flags
                    & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isDebuggerAttached() {
        try {
            return Debug.isDebuggerConnected() || Debug.waitingForDebugger();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isRooted() {
        try {
            String[] paths = {
                    "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su",
                    "/system/bin/.ext/su", "/system/usr/we-need-root/su",
                    "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
                    "/su/bin/su", "/magisk/.core/bin/su"
            };
            for (String p : paths) {
                if (new File(p).exists()) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Hard gate executed before anything else. Returns true when the process can continue.
     */
    public static boolean gate(Context context) {
        if (isDebuggable(context)) return false;
        if (isDebuggerAttached()) return false;
        return isSignatureValid(context);
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 3);
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) sb.append(':');
                int v = digest[i] & 0xFF;
                if (v < 0x10) sb.append('0');
                sb.append(Integer.toHexString(v).toUpperCase());
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
