package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.application.JsonBuilderService;
import xml.json.transformer.application.XmlAdapterService;
import xml.json.transformer.domain.InvoiceData;

import javax.swing.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.prefs.Preferences;

public class Main {
        public static void main(String[] args) {
                try {
                        // Preferencias para recordar la última carpeta usada
                        Preferences prefs = Preferences.userNodeForPackage(Main.class);
                        String lastDir = prefs.get("lastDir", System.getProperty("user.home"));

                        // 1) Select input XML
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Seleccione el archivo XML de entrada");
                        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos XML (*.xml)", "xml"));
                        fileChooser.setCurrentDirectory(new File(lastDir)); // ← última carpeta
                        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                                JOptionPane.showMessageDialog(null, "❌ No se seleccionó ningún archivo. Proceso cancelado.");
                                return;
                        }
                        File inputFile = fileChooser.getSelectedFile();
                        // Guardar carpeta para próximas ejecuciones
                        prefs.put("lastDir", inputFile.getParent());
                        System.out.println("📂 XML seleccionado: " + inputFile.getAbsolutePath());

                        // 2) Services
                        XmlAdapterService xmlService = new XmlAdapterService();

                        // 3) Read original XML (outer)
                        Document originalDoc = xmlService.readXml(inputFile.getAbsolutePath());

                        // 4) Extract IssueDate and ParentDocumentID from outer XML
                        XPathFactory xpf = XPathFactory.newInstance();
                        XPath xp = xpf.newXPath();

                        String issueDateStr = (String) xp.evaluate("string(//*[local-name()='IssueDate'][1])",
                                originalDoc, XPathConstants.STRING);
                        if (issueDateStr == null || issueDateStr.isBlank()) {
                                JOptionPane.showMessageDialog(null, "❌ No se encontró <cbc:IssueDate>.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }
                        LocalDate issueDate = LocalDate.parse(issueDateStr.trim());

                        String factura = (String) xp.evaluate("string(//*[local-name()='ParentDocumentID'][1])",
                                originalDoc, XPathConstants.STRING);
                        if (factura == null || factura.isBlank()) {
                                JOptionPane.showMessageDialog(null, "❌ No se encontró <cbc:ParentDocumentID>.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }
                        factura = factura.trim();

                        // 5) Compute output dir and filenames: <xml_dir>/<ParentDocumentID>/{Factura}.xml/.json
                        Path xmlDir = inputFile.getParentFile().toPath();
                        Path outDir = xmlDir.resolve(factura);
                        Files.createDirectories(outDir);
                        String outXml = outDir.resolve(factura + ".xml").toString();
                        String outJson = outDir.resolve(factura + ".json").toString();
                        System.out.println("📦 Carpeta destino: " + outDir);

                        // 6) Extract codPrestador from embedded XML (in Description)
                        Document embeddedXmlForPrestador = xmlService.extractEmbeddedXml(originalDoc);
                        String codPrestador = xp.evaluate(
                                "string(//*[local-name()='AdditionalInformation']/*[local-name()='Name' and " +
                                        "(normalize-space(text())='CODIGO PRESTADOR' or normalize-space(text())='CODIGO_PRESTADOR')]" +
                                        "/following-sibling::*[local-name()='Value'][1])",
                                embeddedXmlForPrestador
                        ).trim();
                        if (codPrestador == null || codPrestador.isBlank()) {
                                System.err.println("⚠️ No se encontró codPrestador en el XML embebido.");
                        } else {
                                System.out.println("💾 codPrestador: " + codPrestador);
                        }

                        // 7) Clone/re-read for modifications
                        Document modifiedDoc = xmlService.readXml(inputFile.getAbsolutePath());

                        // 8) Build JSON via single questionnaire (validaciones internas con loop)
                        System.out.println("📄 Generando JSON (cuestionario)...");
                        Document embeddedXml = xmlService.extractEmbeddedXml(modifiedDoc);
                        JsonBuilderService jsonService = new JsonBuilderService(issueDate);
                        InvoiceData data = jsonService.buildInvoiceData(originalDoc, embeddedXml, codPrestador);
                        if (data == null) {
                                System.out.println("⛔ Operación cancelada por el usuario.");
                                return;
                        }

                        // 9) Fecha suministro (for inner XML transform)
                        String fechaSuministro = jsonService.getFechaSuministro();

                        // 10) Apply embedded XML transformations (keeps your flow)
                        System.out.println("🛠 Aplicando transformaciones al XML embebido...");
                        xmlService.applyManualTransformations(modifiedDoc, fechaSuministro);

                        // 11) Save outputs
                        xmlService.writeJson(data, outJson);
                        xmlService.writeXml(modifiedDoc, outXml);

                        JOptionPane.showMessageDialog(null,
                                "✅ Proceso completado exitosamente.\n\n" +
                                        "📘 XML modificado: " + outXml + "\n" +
                                        "📗 JSON generado: " + outJson,
                                "Proceso finalizado", JOptionPane.INFORMATION_MESSAGE);

                        // 12) Abrir la carpeta de salida y recordar como lastDir
                        try {
                                if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(outDir.toFile());
                                }
                        } catch (Exception ignore) {}
                        prefs.put("lastDir", outDir.toString());

                        System.out.println("🏁 Listo.");
                } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "❌ Error: " + e.getMessage(), "Fallo", JOptionPane.ERROR_MESSAGE);
                }
        }
}
