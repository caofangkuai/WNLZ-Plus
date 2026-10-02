package com.wunelezi.plus.core;

/**
 * A single plugin / XP module entry.
 */
public final class ModuleInfo {

    public String zipName;
    public String name;
    public String author;
    public boolean enabled = true;

    public ModuleInfo() {
    }

    public ModuleInfo(String zipName, String name, String author, boolean enabled) {
        this.zipName = zipName;
        this.name = name;
        this.author = author;
        this.enabled = enabled;
    }

    public String displayName() {
        if (name != null && !name.isEmpty()) return name;
        if (zipName != null) {
            int dot = zipName.lastIndexOf('.');
            return dot > 0 ? zipName.substring(0, dot) : zipName;
        }
        return "";
    }
}
