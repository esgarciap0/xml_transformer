package xml.json.transformer.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class XmlFileSelector {

    public File select(String lastDirectory) {
        CountDownLatch latch = new CountDownLatch(1);
        File[] selected = {null};

        SwingUtilities.invokeLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(lastDirectory));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos XML", "xml"));
            fileChooser.setDialogTitle("Seleccione el XML de entrada");
            fileChooser.setApproveButtonText("Seleccionar");
            fileChooser.setControlButtonsAreShown(true);

            JFrame window = new JFrame("Seleccione el XML de entrada");
            AppIcon.applyTo(window);
            window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            window.setLayout(new BorderLayout());
            window.add(fileChooser, BorderLayout.CENTER);
            window.setSize(900, 600);
            window.setLocationRelativeTo(null);
            window.setResizable(true);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    latch.countDown();
                }
            });
            window.setVisible(true);

            fileChooser.addActionListener(e -> {
                if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                    selected[0] = fileChooser.getSelectedFile();
                } else if (JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand())) {
                    selected[0] = null;
                }
                window.dispose();
                latch.countDown();
            });
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return selected[0];
    }
}
