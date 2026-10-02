package com.wunelezi.plus.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ContentResolver helper for content:// targets exposed by the host application.
 * All methods fail silently and return null / false so callers only surface vague errors.
 */
public final class ContentUriHelper {

    private ContentUriHelper() {
    }

    public static String readUri(Context context, String url) {
        InputStream in = openInputStream(context, url);
        if (in == null) return null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            reader.close();
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static byte[] readBytes(Context context, String url) {
        InputStream in = openInputStream(context, url);
        if (in == null) return null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            in.close();
            return bos.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean writeUri(Context context, String url, String content) {
        return writeBytes(context, url, content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean writeBytes(Context context, String url, byte[] data) {
        OutputStream os = openOutputStream(context, url);
        if (os == null) return false;
        try {
            os.write(data);
            os.flush();
            os.close();
            return true;
        } catch (Throwable t) {
            try {
                os.close();
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    public static boolean delete(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            ContentResolver resolver = context.getContentResolver();
            return resolver.delete(uri, null, null) > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean exists(Context context, String url) {
        return openInputStream(context, url) != null;
    }

    public static InputStream openInputStream(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            ContentResolver resolver = context.getContentResolver();
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable ignored) {
            }
            return resolver.openInputStream(uri);
        } catch (Throwable t) {
            return null;
        }
    }

    public static OutputStream openOutputStream(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            ContentResolver resolver = context.getContentResolver();
            try {
                resolver.takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Throwable ignored) {
            }
            OutputStream os = null;
            try {
                os = resolver.openOutputStream(uri, "wt");
            } catch (Throwable ignored) {
            }
            if (os == null) {
                try {
                    os = resolver.openOutputStream(uri, "w");
                } catch (Throwable ignored) {
                }
            }
            if (os == null) {
                try {
                    os = resolver.openOutputStream(uri);
                } catch (Throwable ignored) {
                }
            }
            return os;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Opens an output stream for the given content URI and throws with the full
     * underlying cause when the provider rejects the request. Used so callers can
     * surface the real error instead of a vague failure message.
     */
    public static OutputStream openOutputStreamOrThrow(Context context, String url) throws Exception {
        Uri uri = Uri.parse(url);
        ContentResolver resolver = context.getContentResolver();
        try {
            resolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        OutputStream os = null;
        Throwable last = null;
        try {
            os = resolver.openOutputStream(uri, "wt");
        } catch (Throwable t) {
            last = t;
        }
        if (os == null) {
            try {
                os = resolver.openOutputStream(uri, "w");
            } catch (Throwable t) {
                last = t;
            }
        }
        if (os == null) {
            try {
                os = resolver.openOutputStream(uri);
            } catch (Throwable t) {
                last = t;
            }
        }
        if (os != null) return os;
        Exception e = new Exception("openOutputStream 返回 null: " + url);
        if (last != null) e.initCause(last);
        throw e;
    }

    public static void writeBytesOrThrow(Context context, String url, byte[] data) throws Exception {
        OutputStream os = openOutputStreamOrThrow(context, url);
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
}
