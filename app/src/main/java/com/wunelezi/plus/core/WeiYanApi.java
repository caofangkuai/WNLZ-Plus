package com.wunelezi.plus.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Remote verification: update check, announcement, card key login and unbind.
 */
public final class WeiYanApi {

    private static final String BASE = "https://wy.llua.cn/v2/";
    private static final String TOKEN = "3bef2dacee0deb19e5df09fa477f1115";
    private static final String RC4_KEY = "FYs0EFgl0gs85070";
    private static final String SIGN_SALT = "6c2LPtDCx8c8U6LY";

    private static final String ID_NOTICE = "95B19A16BC8";
    private static final String ID_UPDATE = "F91E22F0CF0";
    private static final String ID_LOGIN = "C51F16E81E7";
    private static final String ID_UNBIND = "4210AA536BA";

    private static final String LOCAL_VERSION = "1.0";

    public interface UpdateCallback {
        void onDone(boolean needUpdate, String show, String url, boolean must);
    }

    public interface NoticeCallback {
        void onDone(String notice);
    }

    public interface LoginCallback {
        void onDone(boolean success, String message, long vipSeconds);
    }

    public interface UnbindCallback {
        void onDone(boolean success, String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private WeiYanApi() {
    }

    public static void checkUpdate(final Context context, final UpdateCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean need = false;
                String show = "";
                String url = "";
                boolean must = false;
                try {
                    String content = request(context, ID_UPDATE, null);
                    JSONObject root = new JSONObject(content);
                    if ("200".equals(root.optString("code"))) {
                        JSONObject json = new JSONObject(root.optString("msg"));
                        String version = json.optString("version");
                        show = json.optString("updateshow");
                        url = json.optString("updateurl");
                        must = "y".equals(json.optString("updatemust"));
                        need = !LOCAL_VERSION.equals(version);
                    }
                } catch (Throwable ignored) {
                }
                final boolean fNeed = need;
                final String fShow = show;
                final String fUrl = url;
                final boolean fMust = must;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDone(fNeed, fShow, fUrl, fMust);
                    }
                });
            }
        }, "upd").start();
    }

    public static void fetchNotice(final Context context, final NoticeCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String notice = null;
                try {
                    String content = request(context, ID_NOTICE, null);
                    JSONObject root = new JSONObject(content);
                    if ("200".equals(root.optString("code"))) {
                        JSONObject json = new JSONObject(root.optString("msg"));
                        notice = json.optString("app_gg");
                    }
                } catch (Throwable ignored) {
                }
                final String fNotice = notice;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDone(fNotice);
                    }
                });
            }
        }, "notice").start();
    }

    public static void login(final Context context, final String kami, final LoginCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                String message = "";
                long vip = 0L;
                try {
                    String androidId = Settings.System.getString(
                            context.getContentResolver(), Settings.System.ANDROID_ID);
                    if (androidId == null) androidId = "";
                    long time = System.currentTimeMillis();
                    double value = 1 + Math.random() * 10 + time;
                    String sign = md5("kami=" + kami + "&markcode=" + androidId + "&t=" + time + "&" + SIGN_SALT);
                    String extra = "&kami=" + kami + "&markcode=" + androidId + "&t=" + time
                            + "&sign=" + sign + "&value=" + value;
                    String content = request(context, ID_LOGIN, extra);
                    JSONObject root = new JSONObject(content);
                    String code = root.optString("code");
                    String msg = root.optString("msg");
                    if ("200".equals(code)) {
                        JSONObject json = new JSONObject(msg);
                        String check = json.optString("check");
                        String serverTime = root.optString("time");
                        String vipStr = json.optString("vip");
                        String kmtype = json.optString("kmtype");
                        if (check.equals(md5(serverTime + SIGN_SALT + value))) {
                            success = true;
                            vip = parseLong(vipStr);
                            message = "类型：" + kmtype;
                        } else {
                            message = "校验失败";
                        }
                    } else {
                        message = msg;
                    }
                } catch (Throwable ignored) {
                    message = "配置有误";
                }
                final boolean fSuccess = success;
                final String fMessage = message;
                final long fVip = vip;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDone(fSuccess, fMessage, fVip);
                    }
                });
            }
        }, "login").start();
    }

    public static void unbind(final Context context, final String kami, final UnbindCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                String message = "";
                try {
                    String androidId = Settings.System.getString(
                            context.getContentResolver(), Settings.System.ANDROID_ID);
                    if (androidId == null) androidId = "";
                    long time = System.currentTimeMillis();
                    double value = 1 + Math.random() * 10 + time;
                    String sign = md5("kami=" + kami + "&markcode=" + androidId + "&t=" + time + "&" + SIGN_SALT);
                    String extra = "&kami=" + kami + "&markcode=" + androidId + "&t=" + time
                            + "&sign=" + sign + "&value=" + value;
                    String content = request(context, ID_UNBIND, extra);
                    JSONObject root = new JSONObject(content);
                    String code = root.optString("code");
                    String msg = root.optString("msg");
                    if ("200".equals(code)) {
                        JSONObject json = new JSONObject(msg);
                        success = true;
                        message = "解绑成功\n剩余解绑次数：" + json.optString("num");
                    } else {
                        message = msg;
                    }
                } catch (Throwable ignored) {
                    message = "配置有误";
                }
                final boolean fSuccess = success;
                final String fMessage = message;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDone(fSuccess, fMessage);
                    }
                });
            }
        }, "unbind").start();
    }

    private static String request(Context context, String id, String extra) {
        try {
            String data = "id=" + id + (extra == null ? "" : extra);
            String enc = Rc4Util.encryRC4String(data, RC4_KEY, "UTF-8");
            String url = BASE + TOKEN + "/" + enc;
            String resp = NetClient.get(context, url);
            if (resp == null) return null;
            return Rc4Util.decryRC4(resp, RC4_KEY, "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static String md5(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xFF) < 0x10) hex.append('0');
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
