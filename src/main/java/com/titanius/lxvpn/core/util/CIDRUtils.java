package com.titanius.lxvpn.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Matches an address against a CIDR range.
 *
 * <p>Bypass lists and blocklists are written in ranges, not individual addresses. Handles IPv4 and
 * IPv6, and never throws on malformed input - a typo in a config file should exclude one entry, not
 * stop the plugin from starting.
 */
public final class CIDRUtils {

    private CIDRUtils() {
    }

    /** True when the notation looks like {@code 10.0.0.0/8} rather than a bare address. */
    public static boolean isRange(String notation) {
        return notation != null && notation.indexOf('/') > 0;
    }

    /**
     * @param notation a CIDR range such as {@code 192.168.0.0/16}, or a plain address
     * @param address  the address to test
     * @return true when the address falls inside the range; false on any parse failure
     */
    public static boolean matches(String notation, String address) {
        if (notation == null || address == null) {
            return false;
        }
        int slash = notation.indexOf('/');
        if (slash <= 0) {
            return notation.equals(address);
        }
        try {
            InetAddress network = InetAddress.getByName(notation.substring(0, slash).trim());
            int prefix = Integer.parseInt(notation.substring(slash + 1).trim());
            InetAddress target = InetAddress.getByName(address);

            byte[] networkBytes = network.getAddress();
            byte[] targetBytes = target.getAddress();
            if (networkBytes.length != targetBytes.length) {
                return false; // mixing IPv4 and IPv6 never matches
            }
            if (prefix < 0 || prefix > networkBytes.length * 8) {
                return false;
            }

            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;

            if (fullBytes > 0 && !Arrays.equals(
                    Arrays.copyOf(networkBytes, fullBytes), Arrays.copyOf(targetBytes, fullBytes))) {
                return false;
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = (0xFF00 >> remainingBits) & 0xFF;
            return (networkBytes[fullBytes] & mask) == (targetBytes[fullBytes] & mask);
        } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            return false;
        }
    }

    /** True when the address is loopback, link-local or in a private range. */
    public static boolean isLocal(String address) {
        try {
            InetAddress inet = InetAddress.getByName(address);
            return inet.isLoopbackAddress() || inet.isLinkLocalAddress() || inet.isSiteLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
