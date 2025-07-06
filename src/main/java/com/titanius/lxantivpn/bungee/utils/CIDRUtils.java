// Source code is decompiled from a .class file using FernFlower decompiler.
package com.titanius.lxantivpn.bungee.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class CIDRUtils {
    private final InetAddress base;
    private final int prefixLength;
    private final long mask;

    public CIDRUtils(String cidr) throws UnknownHostException {
        String[] parts = cidr.split("/");
        base = InetAddress.getByName(parts[0]);
        prefixLength = Integer.parseInt(parts[1]);
        byte[] baseAddr = base.getAddress();
        int maskLength = (prefixLength + 7) / 8;
        byte[] maskBytes = new byte[maskLength];
        Arrays.fill(maskBytes, (byte)-1);
        int remainingBits = prefixLength % 8;
        if (remainingBits != 0) {
            maskBytes[maskLength - 1] = (byte)(255 << 8 - remainingBits);
        }

        mask = byteArrayToLong(maskBytes);
    }

    public boolean isInRange(String ip) throws UnknownHostException {
        byte[] ipAddr = InetAddress.getByName(ip).getAddress();
        long ipLong = byteArrayToLong(ipAddr);
        long baseLong = byteArrayToLong(this.base.getAddress());
        return (ipLong & mask) == (baseLong & mask);
    }

    private long byteArrayToLong(byte[] bytes) {
        long result = 0L;
        byte[] var4 = bytes;
        int var5 = bytes.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            byte b = var4[var6];
            result = result << 8 | (long)(b & 255);
        }

        return result;
    }
}
