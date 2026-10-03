//
// Decompiled by Jadx - 847ms
//
package com.wunelezi.injector;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.wunelezi.injector.Inject;
import dalvik.system.DexClassLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

public final class Inject {
    private static final String API_TOKEN = "3bef2dacee0deb19e5df09fa477f1115";
    private static final int BUFFER_SIZE = 4096;
    private static final boolean CLEAN_DEX_DIR_BEFORE_INJECT = true;
    private static final String DEX_DIR_NAME = "wnlz_dex";
    private static final String DEX_SUFFIX = ".dex";
    private static final String INFO_ENTRY_NAME = "info.json";
    private static final String INFO_KEY_INIT = "init";
    private static final String INFO_KEY_MERGE_DEX = "mergeDex";
    private static final int MAX_INFO_JSON_CHARS = -1;
    private static final String OVERLAY_TAG = "WNLZ_KAMI_OVERLAY";
    private static final String PLUGIN_LIST_FILE = "plugins.json";
    private static final String RC4_KEY = "FYs0EFgl0gs85070";
    private static final String SIGN_SALT = "6c2LPtDCx8c8U6LY";
    private static final String SP_KEY_KAMI = "saved_kami";
    private static final String SP_KEY_VALID = "kami_valid";
    private static final String SP_NAME = "wnlz_kami_config";
    private static final String TAG = "WNLZ-Inject";
    private static final String WEIURL = "https://wy.llua.cn/v2/";
    private static View sFloatBall;
    private static View sOverlayView;
    private static final AtomicBoolean sCleanedOnce = new AtomicBoolean(false);
    private static final Set<String> sInjectedPlugins = new HashSet();
    private static final Set<String> sExtractedDexPaths = new HashSet();
    private static final Set<String> sRegisteredDexPaths = new HashSet();
    private static final AtomicInteger sDexSeq = new AtomicInteger(0);
    private static final AtomicBoolean sKamiCheckDone = new AtomicBoolean(false);
    private static volatile boolean sLoadAllowed = false;

    public interface KamiCallback {
        void onResult(boolean z, String str);
    }

    public interface LoadCallback {
        void onComplete(boolean z);
    }

    public static final class PluginInfo {
        final String initClassName;
        final boolean mergeDex;

        PluginInfo(String str, boolean z) {
            this.initClassName = str;
            this.mergeDex = z;
        }
    }

    private static byte[] initRC4Key(String str) {
        byte[] bytes = str.getBytes();
        byte[] bArr = new byte[256];
        for (int i = 0; i < 256; i++) {
            bArr[i] = (byte) i;
        }
        if (bytes.length == 0) {
            return null;
        }
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < 256; i4++) {
            int i5 = bytes[i2] & 255;
            byte b = bArr[i4];
            i3 = (i5 + (b & 255) + i3) & 255;
            bArr[i4] = bArr[i3];
            bArr[i3] = b;
            i2 = (i2 + 1) % bytes.length;
        }
        return bArr;
    }

    private static byte[] rc4Base(byte[] bArr, String str) {
        byte[] initRC4Key = initRC4Key(str);
        byte[] bArr2 = new byte[bArr.length];
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < bArr.length; i3++) {
            i = (i + 1) & 255;
            byte b = initRC4Key[i];
            i2 = ((b & 255) + i2) & 255;
            initRC4Key[i] = initRC4Key[i2];
            initRC4Key[i2] = b;
            int i4 = ((initRC4Key[i] & 255) + (b & 255)) & 255;
            bArr2[i3] = (byte) (initRC4Key[i4] ^ bArr[i3]);
        }
        return bArr2;
    }

    private static String bytesToHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() < 2) {
                sb.append(0);
            }
            sb.append(hexString);
        }
        return sb.toString();
    }

    private static byte[] hexToByte(String str) {
        byte[] bArr;
        int length = str.length();
        if (length % 2 == 1) {
            length++;
            bArr = new byte[length / 2];
            str = "0" + str;
        } else {
            bArr = new byte[length / 2];
        }
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = i + 2;
            bArr[i2] = (byte) Integer.parseInt(str.substring(i, i3), 16);
            i2++;
            i = i3;
        }
        return bArr;
    }

    private static String encryRC4String(String str, String str2, String str3) throws UnsupportedEncodingException {
        if (str == null || str2 == null) {
            return null;
        }
        return bytesToHex(rc4Base((str3 == null || str3.isEmpty()) ? str.getBytes() : str.getBytes(str3), str2));
    }

    private static String decryRC4(String str, String str2, String str3) throws UnsupportedEncodingException {
        if (str == null || str2 == null) {
            return null;
        }
        return new String(rc4Base(hexToByte(str), str2), str3);
    }

    private static String urlGet(String str) {
        StringBuilder sb = new StringBuilder();
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
            httpURLConnection.setReadTimeout(9000);
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setRequestMethod("GET");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                }
                sb.append(readLine);
            }
            bufferedReader.close();
        } catch (IOException e) {
            Log.e(TAG, "HTTP请求失败", e);
        }
        return sb.toString();
    }

    private static String stringToMD5(String str) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                int i = b & 255;
                if (i < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toHexString(i));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static SharedPreferences getSP(Context context) {
        return context.getSharedPreferences(SP_NAME, 0);
    }

    private static void saveKami(Context context, String str, boolean z) {
        getSP(context).edit().putString(SP_KEY_KAMI, str).putBoolean(SP_KEY_VALID, z).apply();
    }

    private static String getSavedKami(Context context) {
        return getSP(context).getString(SP_KEY_KAMI, "");
    }

    private static boolean isKamiValid(Context context) {
        return getSP(context).getBoolean(SP_KEY_VALID, false);
    }

    private static Activity getCurrentActivity() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread", new Class[0]);
            declaredMethod.setAccessible(CLEAN_DEX_DIR_BEFORE_INJECT);
            Object invoke = declaredMethod.invoke(null, new Object[0]);
            Field declaredField = cls.getDeclaredField("mActivities");
            declaredField.setAccessible(CLEAN_DEX_DIR_BEFORE_INJECT);
            Object obj = declaredField.get(invoke);
            if (obj != null && (obj instanceof ArrayMap)) {
                for (Object obj2 : ((ArrayMap) obj).values()) {
                    if (obj2 != null) {
                        Field declaredField2 = obj2.getClass().getDeclaredField("activity");
                        declaredField2.setAccessible(CLEAN_DEX_DIR_BEFORE_INJECT);
                        Object obj3 = declaredField2.get(obj2);
                        if (obj3 instanceof Activity) {
                            return (Activity) obj3;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取当前Activity失败", e);
        }
        return null;
    }

    private static void verifyKamiAsync(final Application application, final String str, final KamiCallback kamiCallback) {
        new Thread(new Runnable() {
            @Override
            public final void run() {
                Inject.lambda$verifyKamiAsync$2(application, str, kamiCallback);
            }
        }).start();
    }

    public static void lambda$verifyKamiAsync$2(Application application, String str, final KamiCallback kamiCallback) {
        final String string;
        final boolean z;
        try {
            String string2 = Settings.System.getString(application.getContentResolver(), "android_id");
            long time = new Date().getTime();
            double random = (Math.random() * 10.0d) + 1.0d;
            double d = time;
            Double.isNaN(d);
            double d2 = random + d;
            JSONObject jSONObject = new JSONObject(decryRC4(urlGet("https://wy.llua.cn/v2/3bef2dacee0deb19e5df09fa477f1115/" + encryRC4String("id=C51F16E81E7&kami=" + str + "&markcode=" + string2 + "&t=" + time + "&sign=" + stringToMD5("kami=" + str + "&markcode=" + string2 + "&t=" + time + "&6c2LPtDCx8c8U6LY") + "&value=" + d2, RC4_KEY, "UTF-8")), RC4_KEY, "UTF-8"));
            if (!jSONObject.getString("code").equals("200")) {
                saveKami(application, "", false);
                string = jSONObject.getString("msg");
                z = false;
            } else {
                if (!new JSONObject(jSONObject.getString("msg")).getString("check").equals(stringToMD5(jSONObject.getString("time") + SIGN_SALT + d2))) {
                    saveKami(application, "", false);
                    string = "卡密校验失败";
                    z = false;
                } else {
                    z = CLEAN_DEX_DIR_BEFORE_INJECT;
                    saveKami(application, str, CLEAN_DEX_DIR_BEFORE_INJECT);
                    string = "卡密验证成功！";
                }
            }
            if (kamiCallback != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public final void run() {
                        Inject.KamiCallback.this.onResult(z, string);
                    }
                });
            }
        } catch (Exception e) {
            saveKami(application, "", false);
            if (kamiCallback != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public final void run() {
                        Inject.KamiCallback.this.onResult(false, "网络错误，请检查连接");
                    }
                });
            }
        }
    }

    private static int dp2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private static void removeOverlay() {
        View view = sOverlayView;
        if (view == null) {
            return;
        }
        try {
            ViewGroup viewGroup = (ViewGroup) view.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(sOverlayView);
            }
        } catch (Exception e) {
            Log.e(TAG, "移除悬浮窗失败", e);
        }
        sOverlayView = null;
    }

    private static void attachOverlayToActivity(final Activity activity, final LoadCallback loadCallback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public final void run() {
                Inject.lambda$attachOverlayToActivity$10(activity, loadCallback);
            }
        });
    }

    public static void lambda$attachOverlayToActivity$10(final Activity activity, final LoadCallback loadCallback) {
        try {
            ViewGroup viewGroup = (ViewGroup) activity.getWindow().getDecorView();
            if (viewGroup == null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        Inject.lambda$attachOverlayToActivity$3(activity, loadCallback);
                    }
                }, 300L);
                return;
            }
            View findViewWithTag = viewGroup.findViewWithTag(OVERLAY_TAG);
            if (findViewWithTag == null) {
                FrameLayout frameLayout = new FrameLayout(activity);
                frameLayout.setTag(OVERLAY_TAG);
                frameLayout.setClipChildren(false);
                frameLayout.setClipToPadding(false);
                final LinearLayout linearLayout = new LinearLayout(activity);
                linearLayout.setOrientation(1);
                linearLayout.setPadding(dp2px(activity, 16.0f), dp2px(activity, 12.0f), dp2px(activity, 16.0f), dp2px(activity, 12.0f));
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setColor(MAX_INFO_JSON_CHARS);
                gradientDrawable.setCornerRadius(dp2px(activity, 12.0f));
                gradientDrawable.setStroke(dp2px(activity, 1.0f), Color.parseColor("#2196F3"));
                linearLayout.setBackground(gradientDrawable);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dp2px(activity, 300.0f), -2);
                layoutParams.topMargin = dp2px(activity, 100.0f);
                layoutParams.gravity = 49;
                linearLayout.setVisibility(8);
                TextView textView = new TextView(activity);
                textView.setText("卡密验证");
                textView.setTextSize(16.0f);
                textView.setTextColor(Color.parseColor("#212121"));
                textView.setGravity(17);
                textView.setPadding(0, 0, 0, dp2px(activity, 8.0f));
                linearLayout.addView(textView);
                final EditText editText = new EditText(activity);
                editText.setHint("请输入卡密");
                editText.setTextColor(Color.parseColor("#212121"));
                editText.setHintTextColor(Color.parseColor("#9E9E9E"));
                editText.setBackground(null);
                editText.setPadding(dp2px(activity, 8.0f), dp2px(activity, 4.0f), dp2px(activity, 8.0f), dp2px(activity, 4.0f));
                String savedKami = getSavedKami(activity.getApplicationContext());
                if (!savedKami.isEmpty()) {
                    editText.setText(savedKami);
                    editText.setSelection(savedKami.length());
                }
                linearLayout.addView(editText);
                LinearLayout linearLayout2 = new LinearLayout(activity);
                linearLayout2.setOrientation(0);
                linearLayout2.setGravity(8388613);
                linearLayout2.setPadding(0, dp2px(activity, 8.0f), 0, 0);
                Button button = new Button(activity);
                button.setText("关闭");
                button.setTextColor(Color.parseColor("#757575"));
                button.setBackgroundColor(0);
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
                layoutParams2.rightMargin = dp2px(activity, 8.0f);
                button.setLayoutParams(layoutParams2);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        linearLayout.setVisibility(8);
                    }
                });
                final Button button2 = new Button(activity);
                button2.setText("登录");
                button2.setTextColor(MAX_INFO_JSON_CHARS);
                GradientDrawable gradientDrawable2 = new GradientDrawable();
                gradientDrawable2.setColor(Color.parseColor("#2196F3"));
                gradientDrawable2.setCornerRadius(dp2px(activity, 6.0f));
                button2.setBackground(gradientDrawable2);
                button2.setPadding(dp2px(activity, 16.0f), dp2px(activity, 4.0f), dp2px(activity, 16.0f), dp2px(activity, 4.0f));
                linearLayout2.addView(button);
                linearLayout2.addView(button2);
                linearLayout.addView(linearLayout2);
                frameLayout.addView(linearLayout, layoutParams);
                int dp2px = dp2px(activity, 48.0f);
                ImageView imageView = new ImageView(activity);
                GradientDrawable gradientDrawable3 = new GradientDrawable();
                gradientDrawable3.setShape(1);
                gradientDrawable3.setColor(Color.parseColor("#2196F3"));
                imageView.setBackground(gradientDrawable3);
                imageView.setImageResource(0x0108003e);
                FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(dp2px, dp2px);
                layoutParams3.gravity = 8388629;
                layoutParams3.rightMargin = dp2px(activity, 16.0f);
                frameLayout.addView(imageView, layoutParams3);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        linearLayout.setVisibility(0);
                    }
                });
                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        Inject.lambda$attachOverlayToActivity$7(editText, activity, button2, linearLayout, loadCallback, view);
                    }
                });
                viewGroup.addView(frameLayout, new FrameLayout.LayoutParams(MAX_INFO_JSON_CHARS, MAX_INFO_JSON_CHARS));
                frameLayout.bringToFront();
                sOverlayView = frameLayout;
                button2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        Inject.lambda$attachOverlayToActivity$9(editText, activity, button2, loadCallback, view);
                    }
                });
                return;
            }
            findViewWithTag.bringToFront();
        } catch (Exception e) {
            Log.e(TAG, "添加悬浮窗失败", e);
            if (loadCallback != null) {
                loadCallback.onComplete(false);
            }
        }
    }

    public static void lambda$attachOverlayToActivity$3(Activity activity, LoadCallback loadCallback) {
        attachOverlayToActivity(activity, loadCallback);
    }

    public static void lambda$attachOverlayToActivity$7(EditText editText, final Activity activity, final Button button, final LinearLayout linearLayout, final LoadCallback loadCallback, View view) {
        final String trim = editText.getText().toString().trim();
        if (trim.isEmpty()) {
            Toast.makeText(activity, "卡密不能为空", 0).show();
            return;
        }
        button.setEnabled(false);
        button.setText("验证中...");
        verifyKamiAsync((Application) activity.getApplicationContext(), trim, new KamiCallback() {
            @Override
            public final void onResult(boolean z, String str) {
                Inject.lambda$attachOverlayToActivity$6(activity, trim, linearLayout, loadCallback, button, z, str);
            }
        });
    }

    public static void lambda$attachOverlayToActivity$6(Activity activity, String str, LinearLayout linearLayout, LoadCallback loadCallback, Button button, boolean z, String str2) {
        Toast.makeText(activity, str2, 0).show();
        if (z) {
            saveKami(activity.getApplicationContext(), str, CLEAN_DEX_DIR_BEFORE_INJECT);
            linearLayout.setVisibility(8);
            if (loadCallback != null) {
                loadCallback.onComplete(CLEAN_DEX_DIR_BEFORE_INJECT);
                return;
            }
            return;
        }
        button.setEnabled(CLEAN_DEX_DIR_BEFORE_INJECT);
        button.setText("登录");
    }

    public static void lambda$attachOverlayToActivity$9(EditText editText, final Activity activity, final Button button, final LoadCallback loadCallback, View view) {
        String trim = editText.getText().toString().trim();
        if (trim.isEmpty()) {
            Toast.makeText(activity, "卡密不能为空", 0).show();
            return;
        }
        button.setEnabled(false);
        button.setText("验证中...");
        verifyKamiAsync((Application) activity.getApplicationContext(), trim, new KamiCallback() {
            @Override
            public final void onResult(boolean z, String str) {
                Inject.lambda$attachOverlayToActivity$8(activity, loadCallback, button, z, str);
            }
        });
    }

    public static void lambda$attachOverlayToActivity$8(Activity activity, LoadCallback loadCallback, Button button, boolean z, String str) {
        Toast.makeText(activity, str, 0).show();
        if (z) {
            removeOverlay();
            if (loadCallback != null) {
                loadCallback.onComplete(CLEAN_DEX_DIR_BEFORE_INJECT);
                return;
            }
            return;
        }
        button.setEnabled(CLEAN_DEX_DIR_BEFORE_INJECT);
        button.setText("验证");
    }

    private static void showKamiSuspendWin(Application application, final LoadCallback loadCallback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public final void run() {
                Inject.lambda$showKamiSuspendWin$11(Inject.LoadCallback.this);
            }
        });
    }

    public static void lambda$showKamiSuspendWin$11(LoadCallback loadCallback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            if (loadCallback != null) {
                loadCallback.onComplete(false);
                return;
            }
            return;
        }
        attachOverlayToActivity(currentActivity, loadCallback);
    }

    private static void checkKami(final Application application) {
        AtomicBoolean atomicBoolean = sKamiCheckDone;
        if (atomicBoolean.get()) {
            loadPlugins(application, sLoadAllowed);
            return;
        }
        if (isKamiValid(application)) {
            sLoadAllowed = CLEAN_DEX_DIR_BEFORE_INJECT;
            atomicBoolean.set(CLEAN_DEX_DIR_BEFORE_INJECT);
            loadPlugins(application, CLEAN_DEX_DIR_BEFORE_INJECT);
        } else {
            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle bundle) {
                }

                @Override
                public void onActivityStarted(Activity activity) {
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    Inject.attachOverlayToActivity(activity, null);
                }

                @Override
                public void onActivityPaused(Activity activity) {
                }

                @Override
                public void onActivityStopped(Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                }
            });
            showKamiSuspendWin(application, new LoadCallback() {
                @Override
                public final void onComplete(boolean z) {
                    Inject.lambda$checkKami$12(application, z);
                }
            });
        }
    }

    public static void lambda$checkKami$12(Application application, boolean z) {
        sLoadAllowed = z;
        sKamiCheckDone.set(CLEAN_DEX_DIR_BEFORE_INJECT);
        loadPlugins(application, z);
    }

    public static void inject(Application application) {
        if (application == null) {
            return;
        }
        checkKami(application);
    }

    private static void loadPlugins(Application application, boolean z) {
        if (!z) {
            log("卡密验证未通过，跳过插件加载");
            return;
        }
        File externalFilesDir = application.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return;
        }
        if (sCleanedOnce.compareAndSet(false, CLEAN_DEX_DIR_BEFORE_INJECT)) {
            cleanDexDir(application);
        }
        File file = new File(externalFilesDir, PLUGIN_LIST_FILE);
        if (!file.exists()) {
            log("plugins.json not found");
            return;
        }
        try {
            JSONArray jSONArray = new JSONArray(readFully(new FileReader(file), MAX_INFO_JSON_CHARS));
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (jSONObject.optBoolean("enable", false)) {
                    String optString = jSONObject.optString("zip", "");
                    if (!optString.isEmpty()) {
                        processPlugin(new File(externalFilesDir, optString), application);
                    }
                } else {
                    log("插件 " + jSONObject.optString("name", "?") + " 已禁用，跳过");
                }
            }
        } catch (Throwable th) {
            log("解析plugins.json失败");
            logThrowable(th);
        }
    }

    private static void cleanDexDir(Application application) {
        File dir = application.getDir(DEX_DIR_NAME, 0);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        int i = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(DEX_SUFFIX) && file.delete()) {
                i++;
            }
        }
        log("清理旧dex文件 " + i + " 个");
    }

    private static void processPlugin(File file, Application application) {
        ClassLoader buildClassLoader;
        if (file.exists()) {
            String absolutePath = file.getAbsolutePath();
            Set<String> set = sInjectedPlugins;
            synchronized (set) {
                if (set.contains(absolutePath)) {
                    return;
                }
                set.add(absolutePath);
                PluginInfo readPluginInfo = readPluginInfo(file);
                if (readPluginInfo == null || readPluginInfo.initClassName == null || readPluginInfo.initClassName.isEmpty()) {
                    return;
                }
                if (readPluginInfo.mergeDex) {
                    if (!mergeDex(file, application)) {
                        buildClassLoader = buildClassLoader(file, application);
                    } else {
                        buildClassLoader = Inject.class.getClassLoader();
                    }
                } else {
                    buildClassLoader = buildClassLoader(file, application);
                }
                if (buildClassLoader == null) {
                    return;
                }
                Object invokeInit = invokeInit(readPluginInfo.initClassName, file, application, buildClassLoader);
                if (invokeInit instanceof Application.ActivityLifecycleCallbacks) {
                    application.registerActivityLifecycleCallbacks((Application.ActivityLifecycleCallbacks) invokeInit);
                }
            }
        }
    }

    private static ClassLoader buildClassLoader(File file, Application application) {
        List<File> extractDex = extractDex(file, application, "a");
        if (extractDex.isEmpty()) {
            return null;
        }
        File dir = application.getDir(DEX_DIR_NAME, 0);
        StringBuilder sb = new StringBuilder();
        for (File file2 : extractDex) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(file2.getAbsolutePath());
        }
        return new DexClassLoader(sb.toString(), dir.getAbsolutePath(), null, Inject.class.getClassLoader());
    }

    private static boolean mergeDex(File file, Application application) {
        List<File> extractDex = extractDex(file, application, "b");
        if (extractDex.isEmpty()) {
            return false;
        }
        ArrayList<File> arrayList = new ArrayList();
        synchronized (sRegisteredDexPaths) {
            for (File file2 : extractDex) {
                if (!sRegisteredDexPaths.contains(file2.getAbsolutePath())) {
                    arrayList.add(file2);
                }
            }
        }
        if (arrayList.isEmpty()) {
            return CLEAN_DEX_DIR_BEFORE_INJECT;
        }
        File dir = application.getDir(DEX_DIR_NAME, 0);
        StringBuilder sb = new StringBuilder();
        for (File file3 : arrayList) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(file3.getAbsolutePath());
        }
        try {
            DexClassLoader dexClassLoader = new DexClassLoader(sb.toString(), dir.getAbsolutePath(), null, Inject.class.getClassLoader());
            Field declaredField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList");
            declaredField.setAccessible(CLEAN_DEX_DIR_BEFORE_INJECT);
            Object obj = declaredField.get(dexClassLoader);
            Object obj2 = declaredField.get(Inject.class.getClassLoader());
            Field declaredField2 = Class.forName("dalvik.system.DexPathList").getDeclaredField("dexElements");
            declaredField2.setAccessible(CLEAN_DEX_DIR_BEFORE_INJECT);
            Object[] objArr = (Object[]) declaredField2.get(obj);
            Object[] objArr2 = (Object[]) declaredField2.get(obj2);
            Object[] objArr3 = (Object[]) Array.newInstance(objArr2.getClass().getComponentType(), objArr2.length + objArr.length);
            System.arraycopy(objArr2, 0, objArr3, 0, objArr2.length);
            System.arraycopy(objArr, 0, objArr3, objArr2.length, objArr.length);
            declaredField2.set(obj2, objArr3);
            synchronized (sRegisteredDexPaths) {
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    sRegisteredDexPaths.add(((File) it.next()).getAbsolutePath());
                }
            }
            return CLEAN_DEX_DIR_BEFORE_INJECT;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<File> extractDex(File file, Application application, String str) {
        ArrayList arrayList = new ArrayList();
        File dir = application.getDir(DEX_DIR_NAME, 0);
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry nextElement = entries.nextElement();
                    if (!nextElement.isDirectory() && nextElement.getName().toLowerCase().endsWith(DEX_SUFFIX)) {
                        File file2 = new File(dir, "plugin_" + str + "_" + System.currentTimeMillis() + "_" + sDexSeq.incrementAndGet() + "_" + new File(nextElement.getName()).getName());
                        if (sExtractedDexPaths.contains(file2.getAbsolutePath())) {
                            arrayList.add(file2);
                        } else {
                            InputStream inputStream = zipFile.getInputStream(nextElement);
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                                try {
                                    byte[] bArr = new byte[BUFFER_SIZE];
                                    while (true) {
                                        int read = inputStream.read(bArr);
                                        if (read <= 0) {
                                            break;
                                        }
                                        fileOutputStream.write(bArr, 0, read);
                                    }
                                    arrayList.add(file2);
                                    sExtractedDexPaths.add(file2.getAbsolutePath());
                                    fileOutputStream.close();
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                } finally {
                                }
                            } finally {
                            }
                        }
                    }
                }
                zipFile.close();
            } finally {
            }
        } catch (IOException e) {
            log("解压dex失败: " + file.getName());
        }
        return arrayList;
    }

    private static PluginInfo readPluginInfo(File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                ZipEntry entry = zipFile.getEntry(INFO_ENTRY_NAME);
                if (entry != null) {
                    JSONObject jSONObject = new JSONObject(readFully(new InputStreamReader(zipFile.getInputStream(entry)), MAX_INFO_JSON_CHARS));
                    PluginInfo pluginInfo = new PluginInfo(jSONObject.optString(INFO_KEY_INIT), jSONObject.optBoolean(INFO_KEY_MERGE_DEX, false));
                    zipFile.close();
                    return pluginInfo;
                }
                zipFile.close();
                return null;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String readFully(Reader reader, int i) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] cArr = new char[BUFFER_SIZE];
        while (true) {
            int read = reader.read(cArr);
            if (read != MAX_INFO_JSON_CHARS) {
                if (i > 0 && sb.length() + read > i) {
                    return null;
                }
                sb.append(cArr, 0, read);
            } else {
                return sb.toString();
            }
        }
    }

    private static Object invokeInit(String str, File file, Application application, ClassLoader classLoader) {
        try {
            Class<?> cls = Class.forName(str, CLEAN_DEX_DIR_BEFORE_INJECT, classLoader);
            Method findMethod = findMethod(cls, new Class[]{File.class, Application.class});
            if (findMethod != null) {
                return findMethod.invoke(cls.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]), file, application);
            }
            Method findMethod2 = findMethod(cls, new Class[]{Application.class});
            if (findMethod2 != null) {
                return findMethod2.invoke(cls.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]), application);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> cls, Class[] clsArr) {
        try {
            Method declaredMethod = cls.getDeclaredMethod("onInject", clsArr);
            declaredMethod.setAccessible(CLEAN_DEX_DIR_BEFORE_INJECT);
            return declaredMethod;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }

    private static void logThrowable(Throwable th) {
        Log.e(TAG, "异常", th);
    }
}
