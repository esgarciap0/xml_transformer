package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.application.JsonBuilderService;
import xml.json.transformer.application.XmlAdapterService;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.licensing.ActivationGate;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class Main {

        private static List<Image> loadAppIcons() {
                String[] sizes = {"16", "32", "48", "128", "256"};
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
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}

                JFrame app = new JFrame("XML Transformer");
                app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                app.setSize(520, 300);
                app.setLocationByPlatform(true);

                List<Image> icons = loadAppIcons();
                if (!icons.isEmpty()) {
                        app.setIconImages(icons);
                        if (Taskbar.isTaskbarSupported()) {
                                try { Taskbar.getTaskbar().setIconImage(icons.get(Math.min(1, icons.size()-1))); } catch (Exception ignore) {}
                        }
                }

                JLabel title = new JLabel("XML Transformer", SwingConstants.CENTER);
                title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
                JLabel hint = new JLabel("Activa tu licencia y luego haz clic en \"Iniciar\".", SwingConstants.CENTER);
                JButton startBtn = new JButton("Iniciar");
                startBtn.setEnabled(false); // ← bloqueado hasta activar

                JPanel root = new JPanel(new BorderLayout());
                root.add(title, BorderLayout.NORTH);
                root.add(hint, BorderLayout.CENTER);
                JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
                south.add(startBtn);
                root.add(south, BorderLayout.SOUTH);
                app.setContentPane(root);
                app.setVisible(true);

                // 1) Gate de activación SIN permitir continuar si falla
                boolean activated = ActivationGate.ensureActivated(app);
                startBtn.setEnabled(activated);
                if (!activated) {
                        JOptionPane.showMessageDialog(app,
                                "La aplicación requiere una licencia válida para continuar.",
                                "Licencia requerida", JOptionPane.INFORMATION_MESSAGE);
                }

                // 2) Aun así, re-chequea antes de correr
                startBtn.addActionListener(e -> {
                        if (!ActivationGate.ensureActivated(app)) {
                                JOptionPane.showMessageDialog(app,
                                        "Licencia inválida o ausente. Importe una licencia válida.",
                                        "Licencia", JOptionPane.WARNING_MESSAGE);
                                return;
                        }
                        runFlow(app);
                });
        }

        private static void runFlow(JFrame app) {
                try {
                        Preferences prefs = Preferences.userNodeForPackage(Main.class);
                        String lastDir = prefs.get("lastDir", System.getProperty("user.home"));

                        JFileChooser fc = new JFileChooser();
                        fc.setDialogTitle("Seleccione el archivo XML de entrada");
                        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos XML (*.xml)", "xml"));
                        fc.setCurrentDirectory(new File(lastDir));
                        if (fc.showOpenDialog(app) != JFileChooser.APPROVE_OPTION) {
                                JOptionPane.showMessageDialog(app, "❌ No se seleccionó ningún archivo. Proceso cancelado.");
                                return;
                        }
                        File inputFile = fc.getSelectedFile();
                        prefs.put("lastDir", inputFile.getParent());
                        System.out.println("📂 XML seleccionado: " + inputFile.getAbsolutePath());

                        XmlAdapterService xmlService = new XmlAdapterService();
                        Document originalDoc = xmlService.readXml(inputFile.getAbsolutePath());

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

                        Path xmlDir = inputFile.getParentFile().toPath();
                        Path outDir = xmlDir.resolve(factura);
                        Files.createDirectories(outDir);
                        String outXml = outDir.resolve(factura + ".xml").toString();
                        String outJson = outDir.resolve(factura + ".json").toString();
                        System.out.println("📦 Carpeta destino: " + outDir);

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

                        Document modifiedDoc = xmlService.readXml(inputFile.getAbsolutePath());

                        System.out.println("📄 Generando JSON (cuestionario)...");
                        Document embeddedXml = xmlService.extractEmbeddedXml(modifiedDoc);
                        JsonBuilderService jsonService = new JsonBuilderService(issueDate);
                        InvoiceData data = jsonService.buildInvoiceData(originalDoc, embeddedXml, codPrestador);
                        if (data == null) {
                                System.out.println("⛔ Operación cancelada por el usuario.");
                                return;
                        }

                        String fechaSuministro = jsonService.getFechaSuministro();

                        System.out.println("🛠 Aplicando transformaciones al XML embebido...");
                        xmlService.applyManualTransformations(modifiedDoc, fechaSuministro);

                        xmlService.writeJson(data, outJson);
                        xmlService.writeXml(modifiedDoc, outXml);

                        JOptionPane.showMessageDialog(app,
                                "✅ Proceso completado exitosamente.\n\n" +
                                        "📘 XML modificado: " + outXml + "\n" +
                                        "📗 JSON generado: " + outJson,
                                "Proceso finalizado", JOptionPane.INFORMATION_MESSAGE);

                        try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(outDir.toFile()); } catch (Exception ignore) {}
                        prefs.put("lastDir", outDir.toString());

                        System.out.println("🏁 Listo.");
                } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(app, "❌ Error: " + e.getMessage(), "Fallo", JOptionPane.ERROR_MESSAGE);
                }
        }
}
