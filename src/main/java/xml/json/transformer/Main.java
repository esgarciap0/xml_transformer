package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.application.JsonBuilderService;
import xml.json.transformer.application.XmlAdapterService;
import xml.json.transformer.domain.InvoiceData;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class Main {

        private static List<Image> loadAppIcons() {
                String[] sizes = {"16","32","48","128","256"};
                List<Image> imgs = new ArrayList<>();
                for (String sz : sizes) {
                        String path = "/app-" + sz + ".png";
                        try (InputStream in = Main.class.getResourceAsStream(path)) {
                                if (in != null) imgs.add(ImageIO.read(in));
                        } catch (IOException ignored) {}
                }
                return imgs;
        }

        public static void main(String[] args) {
                // Look & Feel nativo (opcional)
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}

                // ===== Ventana raíz (para barra de tareas + parenting de diálogos) =====
                JFrame app = new JFrame("XML Transformer");
                app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                app.setSize(460, 280);
                app.setLocationByPlatform(true);
                List<Image> icons = loadAppIcons();
                if (!icons.isEmpty()) {
                        app.setIconImages(icons);
                        if (Taskbar.isTaskbarSupported()) {
                                try { Taskbar.getTaskbar().setIconImage(icons.get(Math.min(1, icons.size()-1))); } catch (Exception ignore) {}
                        }
                }

                // Una portada mínima (opcional)
                JPanel panel = new JPanel(new BorderLayout());
                JLabel title = new JLabel("XML Transformer", SwingConstants.CENTER);
                title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
                panel.add(title, BorderLayout.NORTH);
                JLabel hint = new JLabel("Haz clic en \"Iniciar\" para seleccionar un XML.", SwingConstants.CENTER);
                panel.add(hint, BorderLayout.CENTER);
                JButton startBtn = new JButton("Iniciar");
                JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
                south.add(startBtn);
                panel.add(south, BorderLayout.SOUTH);
                app.setContentPane(panel);
                app.setVisible(true); // ← hace que Windows muestre el botón en la barra

                // Acción principal (también ejecutamos de una si quieres)
                startBtn.addActionListener(e -> runFlow(app));
                // Ejecutar automáticamente al abrir:
                SwingUtilities.invokeLater(() -> startBtn.doClick());
        }

        private static void runFlow(JFrame app) {
                try {
                        Preferences prefs = Preferences.userNodeForPackage(Main.class);
                        String lastDir = prefs.get("lastDir", System.getProperty("user.home"));

                        // 1) Select input XML (anclado a app)
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Seleccione el archivo XML de entrada");
                        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos XML (*.xml)", "xml"));
                        fileChooser.setCurrentDirectory(new File(lastDir));

                        if (fileChooser.showOpenDialog(app) != JFileChooser.APPROVE_OPTION) {
                                JOptionPane.showMessageDialog(app, "❌ No se seleccionó ningún archivo. Proceso cancelado.");
                                return;
                        }
                        File inputFile = fileChooser.getSelectedFile();
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
                                JOptionPane.showMessageDialog(app, "❌ No se encontró <cbc:IssueDate>.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }
                        LocalDate issueDate = LocalDate.parse(issueDateStr.trim());

                        String factura = (String) xp.evaluate("string(//*[local-name()='ParentDocumentID'][1])",
                                originalDoc, XPathConstants.STRING);
                        if (factura == null || factura.isBlank()) {
                                JOptionPane.showMessageDialog(app, "❌ No se encontró <cbc:ParentDocumentID>.", "Error", JOptionPane.ERROR_MESSAGE);
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

                        // 8) Build JSON via questionnaire (validaciones con loop)
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

                        // 10) Apply embedded XML transformations
                        System.out.println("🛠 Aplicando transformaciones al XML embebido...");
                        xmlService.applyManualTransformations(modifiedDoc, fechaSuministro);

                        // 11) Save outputs
                        xmlService.writeJson(data, outJson);
                        xmlService.writeXml(modifiedDoc, outXml);

                        JOptionPane.showMessageDialog(app,
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
                        JOptionPane.showMessageDialog(app, "❌ Error: " + e.getMessage(), "Fallo", JOptionPane.ERROR_MESSAGE);
                }
        }
}
