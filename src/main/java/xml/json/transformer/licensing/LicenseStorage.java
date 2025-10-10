package xml.json.transformer.licensing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static xml.json.transformer.licensing.LicenseModels.LicenseFile;

public class LicenseStorage {
    private static final ObjectMapper OM = new ObjectMapper();

    public static Path licenseDir() {
        String root = System.getenv("ProgramData");
        if (root == null || root.isBlank()) root = System.getProperty("user.home");
        return Path.of(root, "TuEmpresa", "XMLTransformer");
    }

    public static Path licenseFile() {
        return licenseDir().resolve("license.bin");
    }

    public static void saveEncrypted(LicenseFile lf) throws Exception {
        Files.createDirectories(licenseDir());
        byte[] json = OM.writerWithDefaultPrettyPrinter().writeValueAsBytes(lf);
        byte[] enc;
        try {
            enc = WinDpapi.protect(json);    // <-- usa DPAPI
        } catch (Throwable t) {
            // Fallback (no debería suceder en Windows): guarda en claro
            enc = json;
        }
        Files.write(licenseFile(), enc);
    }

    public static LicenseFile loadEncryptedOrNull() {
        try {
            Path p = licenseFile();
            if (!Files.exists(p)) return null;
            byte[] enc = Files.readAllBytes(p);
            byte[] json;
            try {
                json = WinDpapi.unprotect(enc);   // <-- intenta DPAPI
            } catch (Throwable t) {
                // Si no era DPAPI o estás en otro SO, intenta leer directo
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
}
