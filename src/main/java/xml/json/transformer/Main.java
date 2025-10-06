package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.application.JsonBuilderService;
import xml.json.transformer.application.XmlAdapterService;
import xml.json.transformer.domain.InvoiceData;

import javax.swing.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        // 🧭 Seleccionar archivo XML de entrada
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccione el archivo XML de entrada");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos XML (*.xml)", "xml"));

        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(null, "❌ No se seleccionó ningún archivo. El proceso ha sido cancelado.");
            return;
        }

        File inputFile = fileChooser.getSelectedFile();
        String inXml = inputFile.getAbsolutePath();
        System.out.println("📂 Archivo XML seleccionado: " + inXml);

        // 🗂 Seleccionar carpeta de destino
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("Seleccione la carpeta donde guardar los archivos generados");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (dirChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(null, "❌ No se seleccionó una carpeta de destino. El proceso ha sido cancelado.");
            return;
        }

        File outputDir = dirChooser.getSelectedFile();
        String outXml = outputDir.getAbsolutePath() + File.separator + "Modified.xml";
        String outJson = outputDir.getAbsolutePath() + File.separator + "Output.json";

        // 🧱 Crear adaptadores
        XmlAdapterService xmlService = new XmlAdapterService();
        JsonBuilderService jsonService = new JsonBuilderService();

        // 🧩 Leer XML original
        Document originalDoc = xmlService.readXml(inXml);

        // ✅ Extraer el XML embebido del original (antes de aplicar transformaciones)
        Document embeddedFromOriginal = xmlService.extractEmbeddedXml(originalDoc);

        // ✅ Buscar codPrestador dentro del XML embebido (no del principal)
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        String codPrestador = xp.evaluate(
                "normalize-space(//*[local-name()='AdditionalInformation'][*[local-name()='Name' and normalize-space(text())='CODIGO PRESTADOR']]/*[local-name()='Value'])",
                embeddedFromOriginal
        ).trim();

        // Log de depuración
        if (codPrestador == null || codPrestador.isBlank()) {
            System.err.println("⚠️ No se encontró codPrestador en el XML embebido original.");
        } else {
            System.out.println("📦 codPrestador original capturado correctamente: " + codPrestador);
        }

        // 🧩 Crear copia del XML para aplicar transformaciones
        Document modifiedDoc = xmlService.readXml(inXml);

        // ✅ 1. Aplicar transformaciones
        System.out.println("🛠 Aplicando transformaciones...");
        xmlService.applyManualTransformations(modifiedDoc);

        // ✅ 2. Extraer XML embebido (para poblar JSON correctamente)
        Document embeddedXml = xmlService.extractEmbeddedXml(modifiedDoc);

        // ✅ 3. Construir JSON dinámico con codPrestador original
        System.out.println("📄 Generando JSON...");
        InvoiceData data = jsonService.buildInvoiceData(originalDoc, embeddedXml, codPrestador);

        // ✅ 4. Guardar resultados
        xmlService.writeJson(data, outJson);
        xmlService.writeXml(modifiedDoc, outXml);

        // ✅ 5. Confirmación visual
        JOptionPane.showMessageDialog(null,
                "✅ Proceso completado exitosamente.\n\n" +
                        "📘 XML modificado: " + outXml + "\n" +
                        "📗 JSON generado: " + outJson,
                "Proceso finalizado",
                JOptionPane.INFORMATION_MESSAGE);

        System.out.println("🏁 Proceso completado exitosamente.");
    }
}
