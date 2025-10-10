package xml.json.transformer.licensing;

import com.sun.jna.platform.win32.Crypt32Util;

/**
 * Envoltura mínima de DPAPI (Windows) con JNA.
 * - User-scope (por usuario). Suficiente para nuestro caso.
 */
public final class WinDpapi {

    private WinDpapi() {}

    /** Cifra bytes con DPAPI (álcance usuario). */
    public static byte[] protect(byte[] data) {
        return Crypt32Util.cryptProtectData(data);
    }

    /** Descifra bytes producidos por protect(...). */
    public static byte[] unprotect(byte[] blob) {
        return Crypt32Util.cryptUnprotectData(blob);
    }
}
