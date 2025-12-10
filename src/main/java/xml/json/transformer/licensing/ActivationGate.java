package xml.json.transformer.licensing;

import javax.swing.*;
import static xml.json.transformer.licensing.FingerprintServiceWin.*;

public class ActivationGate {

    public static boolean isAlreadyActivated() {
        try {
            // 1. Cargar licencia si existe en disco
            LicenseModels.LicenseFile lic = LicenseStorage.loadEncryptedOrNull();
            if (lic == null) return false;

            // 2. Obtener DeviceId local correcto
            FingerprintServiceWin.Components c = FingerprintServiceWin.collect();
            String deviceId = FingerprintServiceWin.computeDeviceId(c);

            if (deviceId == null || deviceId.isBlank()) return false;

            // 3. Validar la licencia
            LicenseVerifier verifier = new LicenseVerifier();
            String error = verifier.verify(lic, deviceId);

            // licencia válida si error == null
            return error == null;

        } catch (Exception e) {
            return false;
        }
    }

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
