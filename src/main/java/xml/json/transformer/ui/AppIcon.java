package xml.json.transformer.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AppIcon {

    // Carga todas las resoluciones disponibles
    private static final List<Image> ICONS = loadIcons();

    private static List<Image> loadIcons() {
        List<Image> list = new ArrayList<>();
        String[] sizes = {"16", "32", "48", "128", "256"};

        for (String s : sizes) {
            try {
                Image img = Toolkit.getDefaultToolkit().getImage(
                        AppIcon.class.getResource("/app-" + s + ".png")
                );
                list.add(img);
            } catch (Exception ignored) {}
        }
        return list;
    }

    /** Aplica los iconos a cualquier ventana Swing */
    public static void applyTo(Window w) {
        if (w != null && ICONS != null && !ICONS.isEmpty()) {
            w.setIconImages(ICONS);
        }
    }

    /** Fuerza a todos los JOptionPane y diálogos internos a usar icono */
    public static void installAsDefaultIcon() {
        if (ICONS.isEmpty()) return;

        ImageIcon icon16 = new ImageIcon(ICONS.get(0));
        UIManager.put("OptionPane.informationIcon", icon16);
        UIManager.put("OptionPane.errorIcon", icon16);
        UIManager.put("OptionPane.warningIcon", icon16);
        UIManager.put("OptionPane.questionIcon", icon16);
    }
}
