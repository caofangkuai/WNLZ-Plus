package com.wunelezi.plus.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.OutputStream;
import java.util.Random;

/**
 * Writes the decrypted payload into the host application content provider and
 * scatters random decoy blobs of similar size to frustrate extraction.
 */
public final class InjectionManager {

    private static final String ROOT =
            "content://com.netease.x19.widget_file_provider/widget_files/zeus_p/";
    private static final String COMMON =
            ROOT + "gbsdk.plugin.common/version-24400/apk/base-";
    private static final String SHARE =
            ROOT + "gbsdk.plugin.share/version-24400/apk/base-";
    private static final String DEBUG =
            ROOT + "gbsdk.plugin.debug/base-";

    public interface Callback {
        void onResult(boolean success, String message);
    }

    private InjectionManager() {
    }

    public static void inject(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                String error;
                try {
                    error = doInject(app);
                } catch (Throwable t) {
                    error = messageOf(t);
                }
                final boolean ok = error == null;
                final String detail = error;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(ok, detail);
                    }
                });
            }
        }, "inj").start();
    }

    private static String doInject(Context context) {
        byte[] real;
        try {
            real = Payload.base();
        } catch (Throwable t) {
            return messageOf(t);
        }
        if (real == null || real.length == 0) {
            return "无数据";
        }

        try {
            writeOrThrow(context, COMMON + "1.apk", real);
        } catch (Throwable t) {
            return messageOf(t);
        }

        Random random = new Random();
        for (int i = 2; i <= 10; i++) {
            writeDecoy(context, COMMON + i + ".apk", real.length, random);
        }
        for (int i = 2; i <= 10; i++) {
            writeDecoy(context, SHARE + i + ".apk", real.length, random);
        }
        for (int i = 1; i <= 10; i++) {
            writeDecoy(context, DEBUG + i + ".apk", real.length, random);
        }
        return null;
    }

    private static void writeDecoy(Context context, String url, int baseSize, Random random) {
        int delta = random.nextInt(8193) - 4096;
        int size = baseSize + delta;
        if (size < 1024) size = 1024 + random.nextInt(1024);
        byte[] data = new byte[size];
        random.nextBytes(data);
        try {
            writeOrThrow(context, url, data);
        } catch (Throwable ignored) {
        }
    }

    private static void writeOrThrow(Context context, String url, byte[] data) throws Exception {
        OutputStream os = ContentUriHelper.openOutputStreamOrThrow(context, url);
        try {
            os.write(data);
            os.flush();
        } finally {
            try {
                os.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String messageOf(Throwable t) {
        return "注入出错";
    }
}
