package dev;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import xml.json.transformer.licensing.LicenseModels.LicensePayload;

import java.nio.file.Files;
import java.nio.file.Path;

public class CanonTool {
    public static void main(String[] args) throws Exception {
        // Rellena con tus datos reales
        var p = new LicensePayload();
        p.owner = "Servimedical Salud S.A.S";
        p.deviceId = "068c185c465fc90a94396bfbc6aa7236e1b138bb8dc154b720025d65982eba0d";
        p.exp = "2026-12-31";
        p.features = java.util.List.of("core");

        var CANON = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        byte[] canon = CANON.writeValueAsBytes(p);
        Files.write(Path.of("C:/Users/nicol/OneDrive/Escritorio/LicenciasApp/payload_canon.json"), canon);
        System.out.println("OK canon -> payload_canon.json");
    }
}
