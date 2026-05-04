package dev;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import xml.json.transformer.licensing.LicenseModels.LicensePayload;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper interno para generar el payload canónico antes de firmar una licencia.
 * Este archivo vive fuera de src/main para que no termine empaquetado dentro de la app.
 */
public class CanonTool {
    public static void main(String[] args) throws Exception {
        var p = new LicensePayload();
        p.owner = "REEMPLAZAR_OWNER";
        p.deviceId = "REEMPLAZAR_DEVICE_ID";
        p.exp = "2026-12-31";
        p.features = java.util.List.of("core");

        var canon = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .writeValueAsBytes(p);

        Path out = Path.of("payload_canon.json");
        Files.write(out, canon);
        System.out.println("OK canon -> " + out.toAbsolutePath());
    }
}
