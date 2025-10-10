package xml.json.transformer.licensing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/** Obtiene señales estables de hardware en Windows y calcula un deviceId. */
public class FingerprintServiceWin {

    // Cambia este salt por uno tuyo, fijo, no público
    private static final String SALT = "XMLT-2A7F9C6B-const-salt";

    public static final class Components {
        public final String uuid;        // Win32_ComputerSystemProduct.UUID
        public final String baseBoard;   // Win32_BaseBoard.SerialNumber
        public final String bios;        // Win32_BIOS.SerialNumber
        public final String disk;        // Win32_DiskDrive.SerialNumber (primer disco físico)
        public Components(String uuid, String baseBoard, String bios, String disk) {
            this.uuid = n(uuid); this.baseBoard = n(baseBoard); this.bios = n(bios); this.disk = n(disk);
        }
        private String n(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT); }
        public List<String> list(){ return List.of(uuid, baseBoard, bios, disk); }
    }

    /** Ejecuta un comando PowerShell y devuelve la primera línea limpia. */
    private static String ps(String cmd) {
        try {
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", cmd).redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String out = br.lines().collect(Collectors.joining("\n")).trim();
                p.waitFor();
                // nos quedamos con la primera línea no vacía
                for (String line : out.split("\\R")) {
                    String s = line.trim();
                    if (!s.isEmpty()) return s;
                }
                return "";
            }
        } catch (Exception e) { return ""; }
    }

    public static Components collect() {
        String uuid  = ps("(Get-CimInstance Win32_ComputerSystemProduct).UUID");
        String bb    = ps("(Get-CimInstance Win32_BaseBoard).SerialNumber");
        String bios  = ps("(Get-CimInstance Win32_BIOS).SerialNumber");
        // primer disco con serial
        String disk  = ps("(Get-CimInstance Win32_PhysicalMedia | Where-Object {$_.SerialNumber -ne $null} | Select-Object -First 1 -ExpandProperty SerialNumber)");
        if (disk.isEmpty()) {
            // fallback por si PhysicalMedia no trae serial
            disk = ps("(Get-CimInstance Win32_DiskDrive | Select-Object -First 1 -ExpandProperty SerialNumber)");
        }
        return new Components(uuid, bb, bios, disk);
    }

    /** DeviceId = SHA256( SALT + comp1|comp2|comp3|comp4 ) en HEX. */
    public static String computeDeviceId(Components c) {
        String payload = String.join("|", c.list());
        return sha256Hex(SALT + "|" + payload);
    }

    /** Coincidencia por tolerancia: cuántos componentes (de 4) coinciden exactamente. */
    public static int matchCount(Components a, Components b) {
        int m = 0;
        if (a.uuid.equals(b.uuid)) m++;
        if (a.baseBoard.equals(b.baseBoard)) m++;
        if (a.bios.equals(b.bios)) m++;
        if (a.disk.equals(b.disk)) m++;
        return m;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length*2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
