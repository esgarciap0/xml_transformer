package xml.json.transformer.licensing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class FingerprintService {

    private static final String SALT = "XMLT-2A7F9C6B-const-salt";

    public String getLocalDeviceId() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return FingerprintServiceWin.computeDeviceId(FingerprintServiceWin.collect());
        }
        if (os.contains("mac")) {
            String serial = run("system_profiler", "SPHardwareDataType");
            serial = firstMatchAfterLabel(serial, "Serial Number");

            String uuid = run("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
            uuid = firstQuotedValue(uuid, "IOPlatformUUID");

            String model = run("sysctl", "-n", "hw.model").trim();

            return sha256Hex(SALT + "|" + normalize(uuid) + "|" + normalize(serial) + "|" + normalize(model));
        }
        return "";
    }

    private static String run(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder out = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
                process.waitFor();
                return out.toString().trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String firstMatchAfterLabel(String text, String label) {
        for (String line : text.split("\\R")) {
            int idx = line.indexOf(label + ":");
            if (idx >= 0) {
                return line.substring(idx + label.length() + 1).trim();
            }
        }
        return "";
    }

    private static String firstQuotedValue(String text, String key) {
        for (String line : text.split("\\R")) {
            if (!line.contains(key)) continue;
            int first = line.indexOf('"', line.indexOf('='));
            int last = line.lastIndexOf('"');
            if (first >= 0 && last > first) {
                return line.substring(first + 1, last).trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
