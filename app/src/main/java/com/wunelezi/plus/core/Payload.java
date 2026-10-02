package com.wunelezi.plus.core;

/**
 * XOR protected payload holder. The encrypted blob lives in the generated
 * {@link PayloadData} class; the key lives here so the two must be combined.
 */
final class Payload {

    private static final byte[] K = {
            (byte) 0x5B, (byte) 0xA7, (byte) 0x31, (byte) 0xE9,
            (byte) 0x4C, (byte) 0x02, (byte) 0x8D, (byte) 0xF6
    };

    private static volatile byte[] cache;

    private Payload() {
    }

    static byte[] base() {
        byte[] local = cache;
        if (local == null) {
            synchronized (Payload.class) {
                local = cache;
                if (local == null) {
                    byte[] enc = PayloadData.all();
                    byte[] out = new byte[enc.length];
                    for (int i = 0; i < enc.length; i++) {
                        out[i] = (byte) (enc[i] ^ K[i % K.length]);
                    }
                    local = out;
                    cache = out;
                }
            }
        }
        return local;
    }
}
