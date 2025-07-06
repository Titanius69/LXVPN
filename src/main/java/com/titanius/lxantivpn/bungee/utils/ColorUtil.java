// Source code is decompiled from a .class file using FernFlower decompiler.
package com.titanius.lxantivpn.bungee.utils;

import net.md_5.bungee.api.ChatColor;

public class ColorUtil {
    public ColorUtil() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
