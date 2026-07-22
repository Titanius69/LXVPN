package com.titanius.lxvpn.core.util;

/**
 * Translates {@code &}-prefixed colour codes into the section-sign form both proxies understand.
 *
 * <p>The core deals in plain strings. Velocity's Adventure components and BungeeCord's
 * {@code TextComponent} are both built in the platform layer from the string this produces, which
 * keeps message handling identical on both and means messages.yml is one file, not two.
 */
public final class Colors {

    private static final char ALT = '&';
    private static final char SECTION = '\u00a7';
    private static final String CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

    private Colors() {
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == ALT && CODES.indexOf(chars[i + 1]) > -1) {
                chars[i] = SECTION;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /** Removes colour codes, for log lines where escape sequences are just noise. */
    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\u00a7&][0-9a-fk-orA-FK-OR]", "");
    }
}
