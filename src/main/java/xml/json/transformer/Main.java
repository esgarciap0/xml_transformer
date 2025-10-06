package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.application.FileSystemAdapter;
import xml.json.transformer.domain.InvoiceData;

import javax.swing.*;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        // 🧭 Seleccionar el archivo XML de entrada
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccione el archivo XML de entrada");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos XML (*.xml)", "xml"));

        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
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

        int dirResult = dirChooser.showSaveDialog(null);
        if (dirResult != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(null, "❌ No se seleccionó una carpeta de destino. El proceso ha sido cancelado.");
            return;
        }

        File outputDir = dirChooser.getSelectedFile();
        String outXml = outputDir.getAbsolutePath() + File.separator + "Modified.xml";
        String outJson = outputDir.getAbsolutePath() + File.separator + "Output.json";

        // 🧱 Adaptador del sistema de archivos
        FileSystemAdapter adapter = new FileSystemAdapter();

        // 🧩 Leer XML original
        Document doc = adapter.readXml(inXml);

        // ✅ 1. Aplicar transformaciones del manual
        System.out.println("🛠 Aplicando transformaciones...");
        adapter.applyManualTransformations(doc);

        // ✅ 2. Generar JSON
        System.out.println("📄 Generando JSON...");
        InvoiceData data = adapter.buildInvoiceData(doc);
        adapter.writeJson(data, outJson);

        // ✅ 3. Guardar XML modificado
        System.out.println("💾 Guardando XML modificado...");
        adapter.writeXml(doc, outXml);

        // ✅ 4. Confirmación visual
        JOptionPane.showMessageDialog(null,
                "✅ Proceso completado exitosamente.\n\n" +
                        "📘 XML modificado: " + outXml + "\n" +
                        "📗 JSON generado: " + outJson,
                "Proceso finalizado",
                JOptionPane.INFORMATION_MESSAGE);

        System.out.println("🏁 Proceso completado exitosamente.");
    }
}
