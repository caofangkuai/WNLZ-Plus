package com.wunelezi.plus.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Pure content:// based module manager for the host application external files.
 * Metadata is stored in plugins.json / xp.json.
 */
public final class ModuleRepository {

    public enum Kind {
        PLUGIN("plugins.json", ".zip", "application/zip"),
        XP("xp.json", ".apk", "application/vnd.android.package-archive");

        final String jsonName;
        final String suffix;
        final String mimeType;

        Kind(String jsonName, String suffix, String mimeType) {
            this.jsonName = jsonName;
            this.suffix = suffix;
            this.mimeType = mimeType;
        }

        public String mimeType() {
            return mimeType;
        }
    }

    private static final String ROOT =
            "content://com.netease.x19.widget_file_provider/widget_external_files/";

    private ModuleRepository() {
    }

    public static List<ModuleInfo> load(Context context, Kind kind) {
        List<ModuleInfo> result = new ArrayList<>();
        String raw = ContentUriHelper.readUri(context, ROOT + kind.jsonName);
        if (raw == null || raw.trim().isEmpty()) return result;
        try {
            JSONArray arr = new JSONArray(raw.trim());
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                ModuleInfo info = parseEntry(item);
                if (info == null || info.zipName == null || info.zipName.isEmpty()) continue;
                if (kind == Kind.PLUGIN) enrichFromZip(context, info);
                result.add(info);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    public static String importZip(Context context, Kind kind, Uri source) {
        if (source == null) return "未选择文件";
        byte[] data;
        try {
            data = readAll(context, source);
        } catch (Throwable t) {
            return messageOf(t);
        }
        if (data == null || data.length == 0) return "读取文件失败";

        String zipName = UUID.randomUUID().toString().replace("-", "") + kind.suffix;
        String target = ROOT + zipName;
        try {
            ContentUriHelper.writeBytesOrThrow(context, target, data);
        } catch (Throwable t) {
            return messageOf(t);
        }

        ModuleInfo info = new ModuleInfo();
        info.zipName = zipName;
        info.enabled = true;
        if (kind == Kind.XP) {
            parseApkInfo(context, data, info);
        } else {
            parseInfoJson(data, info);
        }
        if (info.name == null || info.name.isEmpty()) info.name = zipName;

        List<ModuleInfo> list = load(context, kind);
        list.add(info);
        try {
            save(context, kind, list);
        } catch (Throwable t) {
            return messageOf(t);
        }
        return null;
    }

    public static boolean delete(Context context, Kind kind, ModuleInfo info) {
        if (info == null) return false;
        List<ModuleInfo> list = load(context, kind);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (info.zipName != null && info.zipName.equals(list.get(i).zipName)) {
                list.remove(i);
            }
        }
        try {
            save(context, kind, list);
        } catch (Throwable ignored) {
        }
        ContentUriHelper.delete(context, ROOT + info.zipName);
        return true;
    }

    public static boolean setEnabled(Context context, Kind kind, ModuleInfo info, boolean enabled) {
        if (info == null) return false;
        List<ModuleInfo> list = load(context, kind);
        for (ModuleInfo m : list) {
            if (info.zipName != null && info.zipName.equals(m.zipName)) {
                m.enabled = enabled;
            }
        }
        try {
            save(context, kind, list);
        } catch (Throwable ignored) {
        }
        return true;
    }

    public static boolean setAllEnabled(Context context, Kind kind, boolean enabled) {
        List<ModuleInfo> list = load(context, kind);
        for (ModuleInfo m : list) m.enabled = enabled;
        try {
            save(context, kind, list);
        } catch (Throwable ignored) {
        }
        return true;
    }

    public static void deleteAll(Context context, Kind kind, Collection<String> zipNames) {
        List<ModuleInfo> list = load(context, kind);
        List<ModuleInfo> keep = new ArrayList<>();
        for (ModuleInfo m : list) {
            if (m.zipName != null && zipNames.contains(m.zipName)) {
                ContentUriHelper.delete(context, ROOT + m.zipName);
            } else {
                keep.add(m);
            }
        }
        try {
            save(context, kind, keep);
        } catch (Throwable ignored) {
        }
    }

    private static void save(Context context, Kind kind, List<ModuleInfo> list) throws Exception {
        JSONArray arr = new JSONArray();
        for (ModuleInfo m : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("zip", m.zipName);
                o.put("name", m.name == null ? "" : m.name);
                if (kind == Kind.PLUGIN) {
                    o.put("author", m.author == null ? "" : m.author);
                }
                o.put("enable", m.enabled);
                arr.put(o);
            } catch (Throwable ignored) {
            }
        }
        ContentUriHelper.writeBytesOrThrow(context, ROOT + kind.jsonName,
                arr.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static ModuleInfo parseEntry(Object item) {
        ModuleInfo info = new ModuleInfo();
        if (item instanceof String) {
            info.zipName = (String) item;
            info.enabled = true;
            return info;
        }
        if (item instanceof JSONObject) {
            JSONObject o = (JSONObject) item;
            info.zipName = firstNonEmpty(o, "zip", "file", "fileName", "path");
            info.name = firstNonEmpty(o, "name", "modName", "moduleName", "title", "displayName");
            info.author = firstNonEmpty(o, "author", "authorName", "by");
            info.enabled = o.optBoolean("enable", o.optBoolean("enabled", true));
            return info;
        }
        return null;
    }

    private static String firstNonEmpty(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, "");
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }

    private static void enrichFromZip(Context context, ModuleInfo info) {
        InputStream in = ContentUriHelper.openInputStream(context, ROOT + info.zipName);
        if (in == null) return;
        try {
            ZipInputStream zis = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String n = entry.getName();
                if (n != null && n.toLowerCase().endsWith("info.json")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = zis.read(buf)) != -1) bos.write(buf, 0, r);
                    applyInfoJson(new String(bos.toByteArray(), StandardCharsets.UTF_8), info);
                    break;
                }
            }
            zis.close();
        } catch (Throwable ignored) {
        }
    }

    private static void parseInfoJson(byte[] zipData, ModuleInfo info) {
        try {
            ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String n = entry.getName();
                if (n != null && n.toLowerCase().endsWith("info.json")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = zis.read(buf)) != -1) bos.write(buf, 0, r);
                    applyInfoJson(new String(bos.toByteArray(), StandardCharsets.UTF_8), info);
                    break;
                }
            }
            zis.close();
        } catch (Throwable ignored) {
        }
    }

    private static void parseApkInfo(Context context, byte[] apkData, ModuleInfo info) {
        File tmp = null;
        try {
            tmp = File.createTempFile("wnlz_xp_", ".apk", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(apkData);
            fos.flush();
            fos.close();

            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(tmp.getAbsolutePath(), 0);
            if (pi != null) {
                ApplicationInfo ai = pi.applicationInfo;
                if (ai != null) {
                    ai.sourceDir = tmp.getAbsolutePath();
                    ai.publicSourceDir = tmp.getAbsolutePath();
                    CharSequence label = ai.loadLabel(pm);
                    if (label != null && label.length() > 0) info.name = label.toString();
                }
                if ((info.name == null || info.name.isEmpty()) && pi.packageName != null) {
                    info.name = pi.packageName;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (tmp != null) {
                try {
                    tmp.delete();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void applyInfoJson(String json, ModuleInfo info) {
        try {
            JSONObject o = new JSONObject(json.trim());
            String name = firstNonEmpty(o, "name", "moduleName", "modName", "title");
            String author = firstNonEmpty(o, "author", "authorName", "by");
            if (!name.isEmpty()) info.name = name;
            if (!author.isEmpty()) info.author = author;
        } catch (Throwable ignored) {
        }
    }

    private static byte[] readAll(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("无法打开所选文件");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String messageOf(Throwable t) {
        return "导入出错";
    }
}
