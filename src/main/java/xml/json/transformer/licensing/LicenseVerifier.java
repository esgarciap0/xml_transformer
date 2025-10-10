package xml.json.transformer.licensing;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static xml.json.transformer.licensing.LicenseModels.*;

public class LicenseVerifier {

    // Reemplaza por tu clave pública real (PEM X.509 SubjectPublicKeyInfo)
    private static final String PUBLIC_KEY_PEM = """
        -----BEGIN PUBLIC KEY-----
                    MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv6owNh5Y239sxIvlZknv
                    yj/ZYSuqZNxBbIwlwsRJXhghI95o+sGCYXbLkw9yzltDhkgp3qa5GmwKPnnF0YK6
                    psqcH/ZPiJL6Z0iecWFAe2yoCr/0FJr8+iAoLRLwYjALWRCQCuuXp5If6cBUvvIi
                    vbkNAVirKkcgs7US2l8zxmWgByHd1CPKLHVlJRGTvur8dFm8tmtySk4Ixi0sr2Ap
                    gx1gBVx9E3YW0SHxEDiKPeznRkJ6M0ROlOKsh8/3UIFkhG24D0GI4ZWA5hYzPtCc
                    BtoNtr6mMPo1OsoS/ZOdrrHbcXVmV7IHoBLEefoWdUUEwf9SeEaOh09tdWqtKwAx
                    VwIDAQAB
                    -----END PUBLIC KEY-----""";

    private final ObjectMapper om = new ObjectMapper();
    private final PublicKey publicKey;

    public LicenseVerifier() {
        this.publicKey = loadPublicKey(PUBLIC_KEY_PEM);
        String fp = java.util.Base64.getEncoder().encodeToString(this.publicKey.getEncoded())
                .substring(0, 24);
        System.out.println("[DEBUG] PublicKey FP: " + fp);
    }

    public static PublicKey loadPublicKey(String pem) {
        try {
            String base64 = pem.replaceAll("-----\\w+ PUBLIC KEY-----", "").replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Clave pública inválida", e);
        }
    }

    /** Devuelve null si ok; de lo contrario, el motivo del rechazo. */
    public String verify(LicenseFile file, String localDeviceId) {
        try {
            if (file == null || file.payload == null || file.signature == null || file.signature.isBlank())
                return "Licencia incompleta.";

            // Expiración (opcional)
            if (LicenseModels.isExpired(file.payload.exp)) return "Licencia expirada.";

            // DeviceId
            if (!file.payload.deviceId.equalsIgnoreCase(localDeviceId))
                return "La licencia no corresponde a este equipo.";

            // Firma
            byte[] signed = canonicalPayloadBytes(file.payload);
            byte[] sig = Base64.getDecoder().decode(file.signature);
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(publicKey);
            s.update(signed);
            boolean ok = s.verify(sig);
            return ok ? null : "Firma digital inválida.";

        } catch (Exception e) {
            return "Error verificando la licencia: " + e.getMessage();
        }
    }

    /** Serializa el payload de forma estable para firmarlo/verificarlo. */
    private static final ObjectMapper CANON = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

    private byte[] canonicalPayloadBytes(LicensePayload p) throws Exception {
        // JSON minificado y con claves ordenadas => forma estable
        return CANON.writeValueAsBytes(p);
    }
}
