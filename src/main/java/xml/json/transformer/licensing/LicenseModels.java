package xml.json.transformer.licensing;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

public class LicenseModels {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LicensePayload {
        public String owner;         // "Empresa X"
        public String deviceId;      // SHA-256 calculado
        public String exp;           // "2026-12-31" (opcional); ISO yyyy-MM-dd
        public List<String> features;// ["core"]
    }

    /** Archivo de licencia: payload + signature Base64 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LicenseFile {
        public LicensePayload payload;
        public String signature;     // Base64 de la firma RSA sobre el JSON canonical del payload
    }

    public static boolean isExpired(String exp) {
        if (exp == null || exp.isBlank()) return false;
        return LocalDate.now().isAfter(LocalDate.parse(exp.trim()));
    }
}
