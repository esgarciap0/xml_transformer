package xml.json.transformer.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AppIcon {

    private static final List<Image> ICONS = loadIcons();

    private static List<Image> loadIcons() {
        List<Image> list = new ArrayList<>();
        String[] sizes = {"16", "32", "48", "128", "256"};

        for (String s : sizes) {
            String path = "/app-" + s + ".png";
            try (InputStream is = AppIcon.class.getResourceAsStream(path)) {
                if (is != null) {
                    Image img = ImageIO.read(is);  // ← Carga REAL
                    list.add(img);
                } else {
                    System.out.println("No encontrado: " + path);
                }
            } catch (IOException e) {
                System.out.println("Error cargando icono: " + path + " - " + e.getMessage());
            }
        }
        return list;
    }

    /** Aplica iconos a ventanas Swing */
    public static void applyTo(Window w) {
        if (w != null && !ICONS.isEmpty()) {
            w.setIconImages(ICONS);
        }
    }

    /** Icono por defecto para JOptionPane */
    public static void installAsDefaultIcon() {
        if (ICONS.isEmpty()) return;

        ImageIcon icon16 = new ImageIcon(ICONS.get(0));
        UIManager.put("OptionPane.informationIcon", icon16);
        UIManager.put("OptionPane.errorIcon", icon16);
        UIManager.put("OptionPane.warningIcon", icon16);
        UIManager.put("OptionPane.questionIcon", icon16);
    }
}
