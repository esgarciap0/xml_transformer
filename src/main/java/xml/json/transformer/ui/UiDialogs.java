package xml.json.transformer.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small helpers to build one big form (keeps insertion order). */
public final class UiDialogs {
    private UiDialogs() {}

    public static Map<String, Object> questionnaire(String title, LinkedHashMap<String, JComponent> fields) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0; c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        // 🔧 campos con ancho consistente (evita crecer horizontal de más)
        fields.forEach((label, comp) -> {
            if (comp instanceof JTextField tf) tf.setColumns(24);
        });

        for (Map.Entry<String, JComponent> e : fields.entrySet()) {
            c.gridx = 0; form.add(new JLabel(e.getKey() + ":"), c);
            c.gridx = 1; form.add(e.getValue(), c);
            c.gridy++;
        }

        // ✅ ScrollPane (ajusta a pantallas pequeñas)
        JScrollPane sp = new JScrollPane(form,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(720, 520)); // <— ajusta si lo quieres más pequeño

        int ok = JOptionPane.showConfirmDialog(
                null, sp, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.OK_OPTION) return null;

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, JComponent> e : fields.entrySet()) {
            JComponent comp = e.getValue();
            Object v = null;
            if (comp instanceof JTextField tf) v = tf.getText();
            else if (comp instanceof JComboBox<?> cb) v = cb.getSelectedItem();
            else if (comp instanceof JSpinner spn) v = spn.getValue();
            else if (comp instanceof JLabel lbl) v = lbl.getText();
            else if (comp instanceof DateTimePicker dt) v = dt.get();
            else {
                try { // JDateChooser
                    if (Class.forName("com.toedter.calendar.JDateChooser").isInstance(comp)) {
                        var m = comp.getClass().getMethod("getDate");
                        v = m.invoke(comp);
                    }
                } catch (Exception ignored) {}
            }
            out.put(e.getKey(), v);
        }
        return out;
    }

    public static JTextField ro(String text) {
        JTextField tf = new JTextField(text != null ? text : "");
        tf.setEditable(false);
        tf.setBackground(new Color(245,245,245));
        return tf;
    }
    public static JTextField tx(String text) { return new JTextField(text != null ? text : ""); }
    public static <T> JComboBox<T> cb(T[] values, T selected) {
        JComboBox<T> box = new JComboBox<>(values);
        if (selected != null) box.setSelectedItem(selected);
        return box;
    }
    public static JSpinner intSpinner(int min, int max, int step, int value) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, step));
    }
}
