package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.infrastructure.FileSystemAdapter;
import xml.json.transformer.domain.InvoiceData;

public class Main {
    public static void main(String[] args) throws Exception {
        String inXml = "C:\\XMLTest\\AttachmentDocument.xml";
        String outXml = "C:\\XMLTest\\Modified.xml";
        String outJson = "C:\\XMLTest\\Output.json";

        FileSystemAdapter adapter = new FileSystemAdapter();

        // 🧩 Leer XML original
        Document doc = adapter.readXml(inXml);

        // ✅ 1. Aplicar transformaciones del manual
        System.out.println("Aplicando transformaciones del manual...");
        adapter.applyManualTransformations(doc);

        // ✅ 2. Generar JSON (opcional)
        System.out.println("Construyendo JSON...");
        InvoiceData data = adapter.buildInvoiceData(doc);
        adapter.writeJson(data, outJson);

        // ✅ 3. Guardar XML formateado
        System.out.println("Guardando XML modificado...");
        adapter.writeXml(doc, outXml);

        System.out.println("🏁 Proceso completo.");
    }
}