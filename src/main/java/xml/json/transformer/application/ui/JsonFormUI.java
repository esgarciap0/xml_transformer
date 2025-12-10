package xml.json.transformer.application.ui;

import com.toedter.calendar.JDateChooser;
import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.ui.AppIcon;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

public class JsonFormUI {

    public FormInput show(String nit,
                          String factura,
                          String codPrestador,
                          String numAutorizacion,
                          String defCodTec,
                          String defNomTec,
                          int defVr,
                          String defDocServicio,
                          String noteHeader,
                          LocalDate issueDate) {

        // ================== LEFT PANEL: FORM ==================
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        int r = 0;

        // Defaults
        String dTipoUsuario = "10";
        String dCodPais = "170";
        String dMun = "23001";
        String dZona = "02";
        String dIncap = "NO";
        String dPaisOrigen = "170";
        int dConsecUser = 1;
        String dTipoOS = "02";
        String dConcepto = "05";
        int dValorPagoMod = 0;
        int dConsecServ = 1;

        // --- Header fields ---
        JTextField tfNit = ro(nit);
        JTextField tfFactura = ro(factura);

        JTextField tfTipoNota = new JTextField();
        JTextField tfNumNota = new JTextField();

        JComboBox<String> cbUserTipoDoc =
                new JComboBox<>(new String[]{"CC","CE","TI","PA","RC","NIT","DNI","PS"});
        cbUserTipoDoc.setSelectedItem("CC");

        JTextField tfUserNumDoc = new JTextField();

        JTextField tfTipoUsuario = new JTextField(dTipoUsuario);

        JDateChooser dcNacimiento = new JDateChooser();
        dcNacimiento.setDateFormatString("yyyy-MM-dd");
        // prevent future dates
        Calendar limNac = Calendar.getInstance();
        limNac.set(Calendar.HOUR_OF_DAY, 0);
        limNac.set(Calendar.MINUTE, 0);
        limNac.set(Calendar.SECOND, 0);
        limNac.set(Calendar.MILLISECOND, 0);
        dcNacimiento.setMaxSelectableDate(new Date(limNac.getTimeInMillis() - 1));

        JComboBox<String> cbSexo = new JComboBox<>(new String[]{"M","F"});
        cbSexo.setSelectedItem("M");

        JTextField tfPaisRes = new JTextField(dCodPais);
        JTextField tfMun = new JTextField(dMun);
        JTextField tfZona = new JTextField(dZona);

        JComboBox<String> cbIncap =
                new JComboBox<>(new String[]{"NO","SI"});
        cbIncap.setSelectedItem(dIncap);

        JTextField tfPaisOrigen = new JTextField(dPaisOrigen);
        JSpinner spConsecUser = new JSpinner(
                new SpinnerNumberModel(dConsecUser, 1, 9999, 1));

        JTextField tfCodPrestador = ro(nvl(codPrestador, ""));
        JTextField tfNumAut = ro(nvl(numAutorizacion, ""));

        JTextField tfMIPRES = new JTextField();

        // Fecha suministro + hora
        JDateChooser dcSumFecha = new JDateChooser();
        dcSumFecha.setDateFormatString("yyyy-MM-dd");
        JSpinner spHora = new JSpinner(new SpinnerNumberModel(15, 0, 23, 1));
        JSpinner spMin = new JSpinner(new SpinnerNumberModel(30, 0, 59, 1));

        JPanel pnlFechaSum = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlFechaSum.add(dcSumFecha);
        pnlFechaSum.add(new JLabel("Hora:"));
        pnlFechaSum.add(spHora);
        pnlFechaSum.add(new JLabel(":"));
        pnlFechaSum.add(spMin);

        JComboBox<String> cbTipoOS =
                new JComboBox<>(new String[]{"01","02","03","04","05"});
        cbTipoOS.setSelectedItem(dTipoOS);

        JTextField tfCodTec = ro(nvl(defCodTec, ""));
        JTextField tfNomTec = new JTextField(nvl(defNomTec, ""));

        JSpinner spCant = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        JComboBox<String> cbServTipoDoc =
                new JComboBox<>(new String[]{"CC","CE","TI","PA","RC","NIT","DNI","PS"});
        cbServTipoDoc.setSelectedItem("CC");

        JTextField tfServNumDoc = new JTextField(nvl(defDocServicio, ""));

        JTextField tfVrUnit = ro(String.valueOf(defVr));

        JComboBox<String> cbConcepto =
                new JComboBox<>(new String[]{"01","02","03","04","05"});
        cbConcepto.setSelectedItem(dConcepto);

        JSpinner spValorPM = new JSpinner(
                new SpinnerNumberModel(dValorPagoMod, 0, Integer.MAX_VALUE, 1));

        JTextField tfNumFEV = new JTextField();

        JSpinner spConsecServ = new JSpinner(
                new SpinnerNumberModel(dConsecServ, 1, 9999, 1));

        // ---- add rows ----
        formAdd(form, c, r++, "NIT:", tfNit);
        formAdd(form, c, r++, "Número de la factura:", tfFactura);
        formAdd(form, c, r++, "Tipo de nota (opcional):", tfTipoNota);
        formAdd(form, c, r++, "Número de la nota (opcional):", tfNumNota);

        formAdd(form, c, r++, "Tipo de documento (usuario):", cbUserTipoDoc);
        formAdd(form, c, r++, "Número de documento (usuario):", tfUserNumDoc);
        formAdd(form, c, r++, "Tipo de usuario:", tfTipoUsuario);

        formAdd(form, c, r++, "Fecha de nacimiento:", dcNacimiento);
        formAdd(form, c, r++, "Código del sexo:", cbSexo);
        formAdd(form, c, r++, "Código país de residencia:", tfPaisRes);
        formAdd(form, c, r++, "Código municipio de residencia:", tfMun);
        formAdd(form, c, r++, "Código zona territorial:", tfZona);

        formAdd(form, c, r++, "Incapacidad:", cbIncap);
        formAdd(form, c, r++, "Código país de origen:", tfPaisOrigen);
        formAdd(form, c, r++, "Consecutivo usuario:", spConsecUser);

        formAdd(form, c, r++, "Código prestador:", tfCodPrestador);
        formAdd(form, c, r++, "Número de autorización:", tfNumAut);
        formAdd(form, c, r++, "ID MIPRES (opcional):", tfMIPRES);

        formAdd(form, c, r++, "Fecha suministro:", pnlFechaSum);

        formAdd(form, c, r++, "Tipo OS:", cbTipoOS);
        formAdd(form, c, r++, "Código tecnología de salud:", tfCodTec);
        formAdd(form, c, r++, "Nombre tecnología de salud:", tfNomTec);

        formAdd(form, c, r++, "Cantidad OS:", spCant);
        formAdd(form, c, r++, "Tipo documento (servicio):", cbServTipoDoc);
        formAdd(form, c, r++, "Número documento (servicio):", tfServNumDoc);

        formAdd(form, c, r++, "Valor unitario OS:", tfVrUnit);

        formAdd(form, c, r++, "Concepto de recaudo:", cbConcepto);
        formAdd(form, c, r++, "Valor pago moderador:", spValorPM);
        formAdd(form, c, r++, "Número FEV pago moderador:", tfNumFEV);
        formAdd(form, c, r++, "Consecutivo del servicio:", spConsecServ);

        JScrollPane leftScroll = new JScrollPane(form);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setPreferredSize(new Dimension(720, 520));

        // ================== RIGHT PANEL: PREVIEW ==================
        JTextArea taPreview = new JTextArea();
        taPreview.setEditable(false);
        taPreview.setLineWrap(true);
        taPreview.setWrapStyleWord(true);
        taPreview.setBackground(new Color(250, 250, 250));
        taPreview.setBorder(BorderFactory.createTitledBorder("Mensaje / Vista previa"));

        Runnable refreshPreview = () -> {
            StringBuilder sb = new StringBuilder();

            if (noteHeader != null && !noteHeader.isBlank()) {
                sb.append(noteHeader.trim());
            } else {
                sb.append("(Sin nota)");
            }
            sb.append("\n\n");

            sb.append("📅 Fecha de factura: ")
                    .append(issueDate != null ? issueDate : "(no disponible)");

            Date d = dcSumFecha.getDate();
            Integer hh = (Integer) spHora.getValue();
            Integer mm = (Integer) spMin.getValue();

            if (d != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                cal.set(Calendar.HOUR_OF_DAY, hh);
                cal.set(Calendar.MINUTE, mm);
                cal.set(Calendar.SECOND, 0);

                Calendar calStart = (Calendar) cal.clone();
                calStart.add(Calendar.DATE, -1);

                String startDate = new SimpleDateFormat("yyyy-MM-dd").format(calStart.getTime());
                String endDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

                sb.append("\n🧾 Periodo de facturación (Paso F): ")
                        .append(startDate).append(" a ").append(endDate)
                        .append("\n   (StartTime 00:00:00-05:00, EndTime 00:00:00-05:00)");
            } else {
                sb.append("\n🧾 Periodo de facturación: seleccione la fecha de suministro para previsualizar.");
            }

            taPreview.setText(sb.toString());
            taPreview.setCaretPosition(0);
        };

        refreshPreview.run();

        dcSumFecha.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) { refreshPreview.run(); }
        });

        ChangeListener timeListener = new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { refreshPreview.run(); }
        };
        spHora.addChangeListener(timeListener);
        spMin.addChangeListener(timeListener);

        JScrollPane rightScroll = new JScrollPane(
                taPreview,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        rightScroll.setPreferredSize(new Dimension(520, 520));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setResizeWeight(0.70);
        split.setBorder(null);

        // ================== DIALOGO PROPIO CON ICONO ==================
        final FormInput[] result = {null};

        JDialog dialog = new JDialog((Frame) null, "Datos para generar JSON", true);
        AppIcon.applyTo(dialog);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(split, BorderLayout.CENTER);

        JPanel southButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Generar JSON");
        JButton btnCancel = new JButton("Cancelar");
        southButtons.add(btnOk);
        southButtons.add(btnCancel);
        dialog.add(southButtons, BorderLayout.SOUTH);

        btnOk.addActionListener(e -> {
            try {
                FormInput in = buildInput(
                        tfTipoNota, tfNumNota,
                        cbUserTipoDoc, tfUserNumDoc, tfTipoUsuario,
                        dcNacimiento, cbSexo, tfPaisRes, tfMun, tfZona,
                        cbIncap, tfPaisOrigen, spConsecUser,
                        cbTipoOS, defCodTec, tfNomTec, spCant,
                        cbServTipoDoc, tfServNumDoc, tfVrUnit,
                        cbConcepto, spValorPM, tfNumFEV, spConsecServ,
                        tfMIPRES, dcSumFecha, spHora, spMin
                );
                result[0] = in;
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        dialog,
                        ex.getMessage(),
                        "Validación",
                        JOptionPane.WARNING_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        btnCancel.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        dialog.pack();
        dialog.setSize(1200, 650);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return result[0];
    }

    // ================== BUILD FormInput ==================
    private FormInput buildInput(
            JTextField tfTipoNota,
            JTextField tfNumNota,
            JComboBox<String> cbUserTipoDoc,
            JTextField tfUserNumDoc,
            JTextField tfTipoUsuario,
            JDateChooser dcNacimiento,
            JComboBox<String> cbSexo,
            JTextField tfPaisRes,
            JTextField tfMun,
            JTextField tfZona,
            JComboBox<String> cbIncap,
            JTextField tfPaisOrigen,
            JSpinner spConsecUser,
            JComboBox<String> cbTipoOS,
            String defCodTec,
            JTextField tfNomTec,
            JSpinner spCant,
            JComboBox<String> cbServTipoDoc,
            JTextField tfServNumDoc,
            JTextField tfVrUnit,
            JComboBox<String> cbConcepto,
            JSpinner spValorPM,
            JTextField tfNumFEV,
            JSpinner spConsecServ,
            JTextField tfMIPRES,
            JDateChooser dcSumFecha,
            JSpinner spHora,
            JSpinner spMin
    ) {

        FormInput in = new FormInput();

        in.tipoNota = tfTipoNota.getText();
        in.numNota = tfNumNota.getText();

        in.user_tipoDoc = String.valueOf(cbUserTipoDoc.getSelectedItem());
        in.user_numDoc = must(tfUserNumDoc.getText(), "Número de documento (usuario)");
        in.user_tipoUsuario = must(tfTipoUsuario.getText(), "Tipo de usuario");

        in.user_fechaNac = dcNacimiento.getDate();
        in.user_fechaNacStr = dateOrNull(dcNacimiento.getDate(), "yyyy-MM-dd");

        in.user_codSexo = String.valueOf(cbSexo.getSelectedItem());
        in.user_codPaisRes = must(tfPaisRes.getText(), "Código país de residencia");
        in.user_codMunRes = must(tfMun.getText(), "Código municipio de residencia");
        in.user_codZona = must(tfZona.getText(), "Código zona territorial");
        in.user_incapacidad = String.valueOf(cbIncap.getSelectedItem());
        in.user_codPaisOrigen = must(tfPaisOrigen.getText(), "Código país de origen");
        in.user_consecutivo = ((Number) spConsecUser.getValue()).intValue();

        in.serv_tipoOS = String.valueOf(cbTipoOS.getSelectedItem());
        in.serv_codTec = nvl(defCodTec, "");
        in.serv_nomTec = must(tfNomTec.getText(), "Nombre tecnología de salud");
        in.serv_cant = ((Number) spCant.getValue()).intValue();
        in.serv_tipoDoc = String.valueOf(cbServTipoDoc.getSelectedItem());
        in.serv_numDoc = must(tfServNumDoc.getText(), "Número documento (servicio)");
        in.serv_vr = parseIntSafe(tfVrUnit.getText(), 0);
        in.serv_concepto = String.valueOf(cbConcepto.getSelectedItem());
        in.serv_valorPagoMod = ((Number) spValorPM.getValue()).intValue();
        in.serv_numFEV = tfNumFEV.getText();
        in.serv_consecutivo = ((Number) spConsecServ.getValue()).intValue();
        in.serv_idMIPRES = tfMIPRES.getText();

        // Fecha de suministro
        Date d = dcSumFecha.getDate();
        if (d == null) {
            throw new IllegalArgumentException("Debe seleccionar fecha de suministro.");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, (Integer) spHora.getValue());
        cal.set(Calendar.MINUTE, (Integer) spMin.getValue());
        cal.set(Calendar.SECOND, 0);

        in.fechaSum = cal.getTime();
        in.fechaSumStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(in.fechaSum);

        return in;
    }

    // ================== helpers ==================
    private static void formAdd(JPanel p, GridBagConstraints c, int row,
                                String label, JComponent comp) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx= 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, c);
    }
    private static JTextField ro(String text) {
        JTextField t = new JTextField(text != null ? text : "");
        t.setEditable(false);
        t.setBackground(new Color(245, 245, 245));
        return t;
    }

    private static String must(String v, String name) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + name + "' es obligatorio.");
        }
        return v.trim();
    }

    private static String dateOrNull(Date d, String pattern) {
        if (d == null) return null;
        return new SimpleDateFormat(pattern).format(d);
    }

    private static int parseIntSafe(String s, int def) {
        if (s == null) return def;
        try {
            String cleaned = s.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? def : Integer.parseInt(cleaned);
        } catch (Exception e) {
            return def;
        }
    }

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}

