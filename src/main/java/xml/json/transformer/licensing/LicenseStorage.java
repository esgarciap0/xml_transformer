package xml.json.transformer.licensing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

import static xml.json.transformer.licensing.LicenseModels.LicenseFile;

public class LicenseStorage {
    private static final ObjectMapper OM = new ObjectMapper();
    private static final String KEYCHAIN_SERVICE = "XMLTransformer";
    private static final String KEYCHAIN_ACCOUNT = "license.dat";

    public static Path licenseDir() {
        String root = System.getenv("ProgramData");
        if (root == null || root.isBlank()) root = System.getProperty("user.home");
        return Path.of(root, "TuEmpresa", "XMLTransformer");
    }

    public static Path licenseFile() {
        return licenseDir().resolve("license.bin");
    }

    public static void saveEncrypted(LicenseFile lf) throws Exception {
        byte[] json = OM.writerWithDefaultPrettyPrinter().writeValueAsBytes(lf);
        if (isMac()) {
            saveToMacKeychain(json);
            return;
        }

        Files.createDirectories(licenseDir());
        byte[] enc;
        try {
            enc = WinDpapi.protect(json);
        } catch (Throwable t) {
            enc = json;
        }
        Files.write(licenseFile(), enc);
    }

    public static LicenseFile loadEncryptedOrNull() {
        try {
            if (isMac()) {
                byte[] json = loadFromMacKeychain();
                return json == null ? null : OM.readValue(json, LicenseFile.class);
            }

            Path p = licenseFile();
            if (!Files.exists(p)) return null;
            byte[] enc = Files.readAllBytes(p);
            byte[] json;
            try {
                json = WinDpapi.unprotect(enc);
            } catch (Throwable t) {
                json = enc;
            }
            return OM.readValue(json, LicenseFile.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static LicenseFile loadFromFile(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            return OM.readValue(in, LicenseFile.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static void saveToMacKeychain(byte[] json) throws Exception {
        String secret = Base64.getEncoder().encodeToString(json);
        Process process = new ProcessBuilder(
                "security", "add-generic-password",
                "-U",
                "-a", KEYCHAIN_ACCOUNT,
                "-s", KEYCHAIN_SERVICE,
                "-w", secret
        ).redirectErrorStream(true).start();

        String output = readProcessOutput(process);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("No se pudo guardar la licencia en Keychain: " + output);
        }
    }

    private static byte[] loadFromMacKeychain() throws Exception {
        Process process = new ProcessBuilder(
                "security", "find-generic-password",
                "-a", KEYCHAIN_ACCOUNT,
                "-s", KEYCHAIN_SERVICE,
                "-w"
        ).redirectErrorStream(true).start();

        String output = readProcessOutput(process).trim();
        int exit = process.waitFor();
        if (exit != 0 || output.isBlank()) {
            return null;
        }
        return Base64.getDecoder().decode(output);
    }

    private static String readProcessOutput(Process process) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append('\n');
            }
            return out.toString();
        }
    }
}
