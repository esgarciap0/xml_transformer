package xml.json.transformer;

import org.w3c.dom.Document;
import xml.json.transformer.application.JsonBuilderService;
import xml.json.transformer.application.XmlAdapterService;
import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.application.ui.JsonFormUI;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.licensing.ActivationGate;
import xml.json.transformer.ui.AppIcon;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.prefs.Preferences;

public class Main {

        public static void main(String[] args) {

                try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignore) {}

                // iconos en JOptionPane
                AppIcon.installAsDefaultIcon();

                // 1️⃣ validar licencia
                if (ActivationGate.isAlreadyActivated()) {
                        runFlow();
                        return;
                }

                // 2️⃣ mostrar ventana de bienvenida
                showInitialWindow();
        }

        private static void showInitialWindow() {

                JFrame app = new JFrame("XML Transformer");
                AppIcon.applyTo(app);
                app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                app.setSize(520, 300);
                app.setLocationRelativeTo(null);

                JLabel title = new JLabel("XML Transformer", SwingConstants.CENTER);
                title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

                JLabel hint = new JLabel("Activa tu licencia para usar el programa", SwingConstants.CENTER);

                JButton startBtn = new JButton("Activar Licencia");

                JPanel root = new JPanel(new BorderLayout());
                root.add(title, BorderLayout.NORTH);
                root.add(hint, BorderLayout.CENTER);

                JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
                south.add(startBtn);
                root.add(south, BorderLayout.SOUTH);

                app.setContentPane(root);
                app.setVisible(true);

                startBtn.addActionListener(e -> {
                        boolean ok = ActivationGate.ensureActivated(app);
                        if (ok) {
                                app.dispose();
                                runFlow();
                        }
                });
        }

        // ======================================================================
        // PROCESO PRINCIPAL
        // ======================================================================
        private static void runFlow() {
                try {

                        Preferences prefs = Preferences.userNodeForPackage(Main.class);
                        String lastDir = prefs.get("lastDir", System.getProperty("user.home"));

                        File inputFile = showXmlChooser(lastDir);
                        if (inputFile == null) return;

                        prefs.put("lastDir", inputFile.getParent());

                        XmlAdapterService xmlService = new XmlAdapterService();
                        Document originalDoc = xmlService.readXml(inputFile.getAbsolutePath());

                        XPath xp = XPathFactory.newInstance().newXPath();

                        String issueDateStr = xp.evaluate("string(//*[local-name()='IssueDate'][1])", originalDoc).trim();
                        LocalDate issueDate = LocalDate.parse(issueDateStr);

                        String factura = xp.evaluate("string(//*[local-name()='ParentDocumentID'][1])", originalDoc).trim();

                        Path outDir = inputFile.getParentFile().toPath().resolve(factura);
                        Files.createDirectories(outDir);

                        String outXml = outDir.resolve(factura + ".xml").toString();
                        String outJson = outDir.resolve(factura + ".json").toString();

                        Document embeddedXml = xmlService.extractEmbeddedXml(originalDoc);

                        String codPrestador = xp.evaluate(
                                "string(//*[local-name()='AdditionalInformation']/*[local-name()='Name' " +
                                        "and (normalize-space(text())='CODIGO PRESTADOR' or normalize-space(text())='CODIGO_PRESTADOR')]/" +
                                        "following-sibling::*[local-name()='Value'][1])",
                                embeddedXml).trim();

                        String numAutorizacion = xp.evaluate("string(//*[local-name()='InvoiceAuthorization'][1])", embeddedXml).trim();
                        String codTec = xp.evaluate("string(//*[local-name()='StandardItemIdentification']/*[local-name()='ID'][1])", embeddedXml).trim();
                        String nomTec = xp.evaluate("string(//*[local-name()='Item']/*[local-name()='Description'][1])", embeddedXml).trim();

                        int vr = 0;
                        try {
                                String v = xp.evaluate("string(//*[local-name()='LineExtensionAmount'][1])",
                                        embeddedXml).replaceAll("[^0-9.]", "");
                                vr = (int) Math.floor(Double.parseDouble(v));
                        } catch (Exception ignored) {}

                        String docIdentServicio = xp.evaluate(
                                "string(//*[local-name()='AccountingCustomerParty']//*[local-name()='CompanyID'][1])",
                                originalDoc).trim();

                        String noteHeader = xp.evaluate("string(//*[local-name()='Note'][1])", embeddedXml).trim();

                        String nitObligado = xp.evaluate(
                                "string(//*[local-name()='CompanyID'][@schemeID='8'][1])",
                                originalDoc).trim();

                        JsonFormUI ui = new JsonFormUI();
                        FormInput input = ui.show(
                                nitObligado, factura, codPrestador, numAutorizacion,
                                codTec, nomTec, vr, docIdentServicio, noteHeader, issueDate
                        );

                        if (input == null) return;

                        JsonBuilderService builder = new JsonBuilderService(issueDate);
                        InvoiceData data = builder.buildInvoiceData(
                                input, nitObligado, factura, codPrestador, numAutorizacion
                        );

                        Document modified = xmlService.readXml(inputFile.getAbsolutePath());
                        xmlService.applyManualTransformations(modified, builder.getFechaSuministro());

                        xmlService.writeJson(data, outJson);
                        xmlService.writeXml(modified, outXml);

                        JOptionPane.showMessageDialog(null,
                                "Proceso completo.\nXML: " + outXml + "\nJSON: " + outJson);

                        Desktop.getDesktop().open(outDir.toFile());

                } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
                }
        }

        // ======================================================================
        // SELECTOR XML — FIX PARA QUE NO SE VEA EN BLANCO EN .EXE
        // ======================================================================
        private static File showXmlChooser(String lastDir) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(lastDir));
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos XML", "xml"));

                AtomicReference<File> selected = new AtomicReference<>(null);
                CountDownLatch latch = new CountDownLatch(1);

                SwingUtilities.invokeLater(() -> {
                        JFrame frame = new JFrame("Seleccione el XML de entrada");
                        AppIcon.applyTo(frame);

                        // Ensure it behaves like a normal window (taskbar + min/max)
                        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        frame.setLayout(new BorderLayout());
                        frame.setMinimumSize(new Dimension(900, 600));
                        frame.setResizable(true);

                        // Optional: make the chooser feel more direct
                        fc.setApproveButtonText("Seleccionar");

                        frame.add(fc, BorderLayout.CENTER);

                        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        JButton ok = new JButton("Seleccionar");
                        JButton cancel = new JButton("Cancelar");
                        south.add(ok);
                        south.add(cancel);
                        frame.add(south, BorderLayout.SOUTH);

                        ok.addActionListener(e -> {
                                selected.set(fc.getSelectedFile());
                                frame.dispose();
                        });

                        cancel.addActionListener(e -> frame.dispose());

                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                                        latch.countDown();
                                }
                        });

                        frame.pack();
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                });

                try {
                        latch.await(); // Wait until the window closes (not on EDT)
                } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                }

                return selected.get();
        }

}
