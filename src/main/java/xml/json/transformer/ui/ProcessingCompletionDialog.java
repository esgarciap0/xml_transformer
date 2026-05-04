package xml.json.transformer.ui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;

public class ProcessingCompletionDialog {

    public boolean showAndAskToProcessAgain() {
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] processAgain = {false};

        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("XML Transformer");
            AppIcon.applyTo(window);
            window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            window.setSize(420, 200);
            window.setLocationRelativeTo(null);
            window.setLayout(new BorderLayout());

            JLabel message = new JLabel(
                    "<html><center>Proceso finalizado correctamente.<br><br>" +
                            "Desea procesar otro XML?</center></html>",
                    SwingConstants.CENTER
            );

            JButton againButton = new JButton("Cambiar otro XML");
            JButton exitButton = new JButton("Finalizar");

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            buttons.add(againButton);
            buttons.add(exitButton);

            window.add(message, BorderLayout.CENTER);
            window.add(buttons, BorderLayout.SOUTH);

            againButton.addActionListener(e -> {
                processAgain[0] = true;
                window.dispose();
                latch.countDown();
            });
            exitButton.addActionListener(e -> {
                processAgain[0] = false;
                window.dispose();
                latch.countDown();
            });
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    latch.countDown();
                }
            });

            window.setVisible(true);
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return processAgain[0];
    }
}
