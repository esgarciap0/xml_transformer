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

                // 🗂 Seleccionar carpeta de salida
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

                // 🧩 Crear servicios
                XmlAdapterService xmlService = new XmlAdapterService();
                JsonBuilderService jsonService = new JsonBuilderService();

                // 📖 Leer XML original
                Document originalDoc = xmlService.readXml(inXml);

                // 🧠 Extraer codPrestador del XML original antes de modificar
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xp = xpf.newXPath();
                // Extraer XML embebido primero
                Document embeddedXmlForPrestador = xmlService.extractEmbeddedXml(originalDoc);

                // Buscar codPrestador dentro del XML embebido
                String codPrestador = xp.evaluate(
                        "string(//*[local-name()='AdditionalInformation']/*[local-name()='Name' and normalize-space(text())='CODIGO PRESTADOR']/following-sibling::*[local-name()='Value'][1])",
                        embeddedXmlForPrestador
                ).trim();

                if (codPrestador == null || codPrestador.isBlank()) {
                        System.err.println("⚠️ No se encontró codPrestador en el XML original.");
                } else {
                        System.out.println("💾 codPrestador original capturado correctamente: " + codPrestador);
                }

                // 🧩 Crear copia del XML para modificaciones
                Document modifiedDoc = xmlService.readXml(inXml);

                // ✅ Generar JSON
                System.out.println("📄 Generando JSON...");
                Document embeddedXml = xmlService.extractEmbeddedXml(modifiedDoc);
                InvoiceData data = jsonService.buildInvoiceData(originalDoc, embeddedXml, codPrestador);

                // ✅ Obtener fecha del JSON (ingresada por usuario)
                String fechaSuministro = jsonService.getFechaSuministro();

                // ✅ Aplicar transformaciones usando esa fecha
                System.out.println("🛠 Aplicando transformaciones...");
                xmlService.applyManualTransformations(modifiedDoc, fechaSuministro);

                // ✅ Guardar resultados
                xmlService.writeJson(data, outJson);
                xmlService.writeXml(modifiedDoc, outXml);

                // ✅ Confirmación visual
                JOptionPane.showMessageDialog(null,
                        "✅ Proceso completado exitosamente.\n\n" +
                                "📘 XML modificado: " + outXml + "\n" +
                                "📗 JSON generado: " + outJson,
                        "Proceso finalizado",
                        JOptionPane.INFORMATION_MESSAGE);

                System.out.println("🏁 Proceso completado exitosamente.");
        }
}
