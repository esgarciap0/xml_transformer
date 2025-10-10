package xml.json.transformer.licensing;

import javax.swing.*;
import static xml.json.transformer.licensing.FingerprintServiceWin.*;

public class ActivationGate {

    /** Devuelve true si la app puede continuar. */
    public static boolean ensureActivated(JFrame appOwner) {
        try {
            var saved = LicenseStorage.loadEncryptedOrNull();
            var comps = FingerprintServiceWin.collect();
            var localDeviceId = FingerprintServiceWin.computeDeviceId(comps);
            var verifier = new LicenseVerifier();

            if (saved != null) {
                String err = verifier.verify(saved, localDeviceId);
                if (err == null) return true;
                JOptionPane.showMessageDialog(appOwner,
                        "La licencia guardada no es válida: " + err + "\n" +
                                "Por favor, importe una licencia válida.", "Licencia", JOptionPane.WARNING_MESSAGE);
            }

            ActivationDialog dlg = new ActivationDialog(appOwner);
            dlg.setVisible(true);
            return dlg.isActivated();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(appOwner,
                    "No se pudo validar la licencia: " + ex.getMessage(),
                    "Licencia", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

}
