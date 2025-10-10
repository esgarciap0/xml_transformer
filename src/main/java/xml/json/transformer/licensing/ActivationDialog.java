package xml.json.transformer.licensing;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;

import static xml.json.transformer.licensing.FingerprintServiceWin.*;
import static xml.json.transformer.licensing.LicenseModels.LicenseFile;

public class ActivationDialog extends JDialog {
    private String deviceId;
    private Components comps;
    private boolean activated = false;

    public ActivationDialog(Frame owner) {
        super(owner, "Activación de licencia", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(owner);

        // recoger huella
        this.comps = FingerprintServiceWin.collect();
        this.deviceId = FingerprintServiceWin.computeDeviceId(comps);

        JTextArea ta = new JTextArea(
                "Huella del equipo (deviceId):\n" + deviceId +
                        "\n\nComponentes:\n" +
                        "UUID: " + comps.uuid + "\n" +
                        "BaseBoard: " + comps.baseBoard + "\n" +
                        "BIOS: " + comps.bios + "\n" +
                        "Disk: " + comps.disk + "\n\n" +
                        "Envíe este deviceId al proveedor para recibir su licencia.\n"
        );
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);

        JButton btnCopy = new JButton("Copiar huella");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(deviceId), null);
            JOptionPane.showMessageDialog(this, "Huella copiada al portapapeles.");
        });

        JButton btnImport = new JButton("Importar licencia…");
        btnImport.addActionListener(e -> onImport());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnCopy);
        south.add(btnImport);

        setLayout(new BorderLayout(8,8));
        add(new JScrollPane(ta), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void onImport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccione el archivo de licencia (license.dat)");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();

        LicenseFile lf = LicenseStorage.loadFromFile(f);
        if (lf == null) {
            JOptionPane.showMessageDialog(this, "Archivo de licencia inválido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String localDeviceId = this.deviceId;
        var verifier = new LicenseVerifier();
        String err = verifier.verify(lf, localDeviceId);
        if (err != null) {
            JOptionPane.showMessageDialog(this, "Licencia no válida: " + err, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            LicenseStorage.saveEncrypted(lf);
            activated = true;
            JOptionPane.showMessageDialog(this, "Licencia activada correctamente.");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la licencia: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isActivated() { return activated; }
}
