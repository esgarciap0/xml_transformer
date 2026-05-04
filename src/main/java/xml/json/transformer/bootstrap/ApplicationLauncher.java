package xml.json.transformer.bootstrap;

import xml.json.transformer.application.model.ProcessAttachmentResult;
import xml.json.transformer.application.port.out.JsonOutputPort;
import xml.json.transformer.application.port.out.XmlDocumentPort;
import xml.json.transformer.application.usecase.ProcessAttachmentUseCase;
import xml.json.transformer.infrastructure.adapter.JsonFileAdapter;
import xml.json.transformer.infrastructure.adapter.SwingFormInputAdapter;
import xml.json.transformer.infrastructure.adapter.XmlFileAdapter;
import xml.json.transformer.licensing.ActivationGate;
import xml.json.transformer.ui.AppIcon;
import xml.json.transformer.ui.ProcessingCompletionDialog;
import xml.json.transformer.ui.XmlFileSelector;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

public class ApplicationLauncher {

    private final Preferences preferences = Preferences.userNodeForPackage(ApplicationLauncher.class);
    private final XmlFileSelector xmlFileSelector = new XmlFileSelector();
    private final ProcessingCompletionDialog completionDialog = new ProcessingCompletionDialog();

    public void start() {
        installLookAndFeel();
        AppIcon.installAsDefaultIcon();

        if (ActivationGate.isAlreadyActivated()) {
            startProcessingFlow();
            return;
        }

        SwingUtilities.invokeLater(this::showInitialWindow);
    }

    private void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
        }
    }

    private void showInitialWindow() {
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
            boolean activated = ActivationGate.ensureActivated(app);
            if (activated) {
                app.dispose();
                startProcessingFlow();
            }
        });
    }

    private void startProcessingFlow() {
        new Thread(this::runProcessingFlow, "main-flow").start();
    }

    private void runProcessingFlow() {
        try {
            String lastDir = preferences.get("lastDir", System.getProperty("user.home"));
            File inputFile = xmlFileSelector.select(lastDir);
            if (inputFile == null) {
                return;
            }

            preferences.put("lastDir", inputFile.getParent());

            ProcessAttachmentResult result = buildUseCase().process(inputFile);
            if (result.status() == ProcessAttachmentResult.Status.CANCELLED) {
                return;
            }

            if (result.status() == ProcessAttachmentResult.Status.BACK) {
                startProcessingFlow();
                return;
            }

            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Proceso completo.\nXML: " + result.xmlOutputPath() + "\nJSON: " + result.jsonOutputPath()
            ));

            tryOpenOutputFolder(result.outputDirectory());
            if (completionDialog.showAndAskToProcessAgain()) {
                startProcessingFlow();
            } else {
                System.exit(0);
            }

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage()));
        }
    }

    private ProcessAttachmentUseCase buildUseCase() {
        XmlDocumentPort xmlPort = new XmlFileAdapter();
        JsonOutputPort jsonPort = new JsonFileAdapter();
        return new ProcessAttachmentUseCase(xmlPort, jsonPort, new SwingFormInputAdapter());
    }

    private void tryOpenOutputFolder(java.nio.file.Path outDir) {
        try {
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            Desktop.getDesktop().open(outDir.toFile());
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Los archivos se generaron correctamente, pero no se pudo abrir la carpeta:\n" + outDir,
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
            ));
        }
    }
}
