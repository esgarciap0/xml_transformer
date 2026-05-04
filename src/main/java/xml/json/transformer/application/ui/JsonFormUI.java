package xml.json.transformer.application.ui;

import com.toedter.calendar.JDateChooser;
import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.ui.AppIcon;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

public class JsonFormUI {

    private final FormInputMapper formInputMapper = new FormInputMapper();
    private final FormPreviewBuilder formPreviewBuilder = new FormPreviewBuilder();

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

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        int r = 0;

        JTextField tfNit = ro(nit);
        JTextField tfFactura = ro(factura);
        JTextField tfTipoNota = new JTextField();
        JTextField tfNumNota = new JTextField();
        JComboBox<String> cbUserTipoDoc = new JComboBox<>(new String[]{"CC", "CE", "TI", "PA", "RC", "NIT", "DNI", "PS"});
        cbUserTipoDoc.setSelectedItem("CC");
        JTextField tfUserNumDoc = new JTextField();
        JTextField tfTipoUsuario = new JTextField("10");

        JDateChooser dcNacimiento = new JDateChooser();
        dcNacimiento.setDateFormatString("yyyy-MM-dd");
        Calendar limNac = Calendar.getInstance();
        limNac.set(Calendar.HOUR_OF_DAY, 0);
        limNac.set(Calendar.MINUTE, 0);
        limNac.set(Calendar.SECOND, 0);
        limNac.set(Calendar.MILLISECOND, 0);
        dcNacimiento.setMaxSelectableDate(new Date(limNac.getTimeInMillis() - 1));

        JComboBox<String> cbSexo = new JComboBox<>(new String[]{"M", "F"});
        cbSexo.setSelectedItem("M");

        JTextField tfPaisRes = new JTextField("170");
        JTextField tfMun = new JTextField("23001");
        JTextField tfZona = new JTextField("02");

        JComboBox<String> cbIncap = new JComboBox<>(new String[]{"NO", "SI"});
        cbIncap.setSelectedItem("NO");

        JTextField tfPaisOrigen = new JTextField("170");
        JSpinner spConsecUser = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        JTextField tfCodPrestador = ro(nvl(codPrestador, ""));
        JTextField tfNumAut = ro(nvl(numAutorizacion, ""));
        JTextField tfMIPRES = new JTextField();

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

        JComboBox<String> cbTipoOS = new JComboBox<>(new String[]{"01", "02", "03", "04", "05"});
        cbTipoOS.setSelectedItem("02");

        JTextField tfCodTec = ro(nvl(defCodTec, ""));
        JTextField tfNomTec = new JTextField(nvl(defNomTec, ""));
        JSpinner spCant = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        JComboBox<String> cbServTipoDoc = new JComboBox<>(new String[]{"CC", "CE", "TI", "PA", "RC", "NIT", "DNI", "PS"});
        cbServTipoDoc.setSelectedItem("CC");
        JTextField tfServNumDoc = new JTextField(nvl(defDocServicio, ""));
        JTextField tfVrUnit = ro(String.valueOf(defVr));
        JComboBox<String> cbConcepto = new JComboBox<>(new String[]{"01", "02", "03", "04", "05"});
        cbConcepto.setSelectedItem("05");
        JSpinner spValorPM = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        JTextField tfNumFEV = new JTextField();
        JSpinner spConsecServ = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        formAdd(form, c, r++, "NIT:", tfNit);
        formAdd(form, c, r++, "Numero de la factura:", tfFactura);
        formAdd(form, c, r++, "Tipo de nota (opcional):", tfTipoNota);
        formAdd(form, c, r++, "Numero de la nota (opcional):", tfNumNota);
        formAdd(form, c, r++, "Tipo de documento (usuario):", cbUserTipoDoc);
        formAdd(form, c, r++, "Numero de documento (usuario):", tfUserNumDoc);
        formAdd(form, c, r++, "Tipo de usuario:", tfTipoUsuario);
        formAdd(form, c, r++, "Fecha de nacimiento:", dcNacimiento);
        formAdd(form, c, r++, "Codigo del sexo:", cbSexo);
        formAdd(form, c, r++, "Codigo pais de residencia:", tfPaisRes);
        formAdd(form, c, r++, "Codigo municipio de residencia:", tfMun);
        formAdd(form, c, r++, "Codigo zona territorial:", tfZona);
        formAdd(form, c, r++, "Incapacidad:", cbIncap);
        formAdd(form, c, r++, "Codigo pais de origen:", tfPaisOrigen);
        formAdd(form, c, r++, "Consecutivo usuario:", spConsecUser);
        formAdd(form, c, r++, "Codigo prestador:", tfCodPrestador);
        formAdd(form, c, r++, "Numero de autorizacion:", tfNumAut);
        formAdd(form, c, r++, "ID MIPRES (opcional):", tfMIPRES);
        formAdd(form, c, r++, "Fecha suministro:", pnlFechaSum);
        formAdd(form, c, r++, "Tipo OS:", cbTipoOS);
        formAdd(form, c, r++, "Codigo tecnologia de salud:", tfCodTec);
        formAdd(form, c, r++, "Nombre tecnologia de salud:", tfNomTec);
        formAdd(form, c, r++, "Cantidad OS:", spCant);
        formAdd(form, c, r++, "Tipo documento (servicio):", cbServTipoDoc);
        formAdd(form, c, r++, "Numero documento (servicio):", tfServNumDoc);
        formAdd(form, c, r++, "Valor unitario OS:", tfVrUnit);
        formAdd(form, c, r++, "Concepto de recaudo:", cbConcepto);
        formAdd(form, c, r++, "Valor pago moderador:", spValorPM);
        formAdd(form, c, r++, "Numero FEV pago moderador (opcional):", tfNumFEV);
        formAdd(form, c, r++, "Consecutivo del servicio:", spConsecServ);

        JScrollPane leftScroll = new JScrollPane(form);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setPreferredSize(new Dimension(720, 520));

        JTextArea taPreview = new JTextArea();
        taPreview.setEditable(false);
        taPreview.setLineWrap(true);
        taPreview.setWrapStyleWord(true);
        taPreview.setBackground(new Color(250, 250, 250));
        taPreview.setBorder(BorderFactory.createTitledBorder("Mensaje / Vista previa"));

        Runnable refreshPreview = () -> taPreview.setText(formPreviewBuilder.build(
                noteHeader,
                issueDate,
                dcSumFecha.getDate(),
                (Integer) spHora.getValue(),
                (Integer) spMin.getValue()
        ));

        refreshPreview.run();
        dcSumFecha.addPropertyChangeListener("date", evt -> refreshPreview.run());
        ChangeListener timeListener = e -> refreshPreview.run();
        spHora.addChangeListener(timeListener);
        spMin.addChangeListener(timeListener);

        JScrollPane rightScroll = new JScrollPane(taPreview);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        rightScroll.setPreferredSize(new Dimension(520, 520));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setResizeWeight(0.70);
        split.setBorder(null);

        JFrame frame = new JFrame("Datos para generar JSON");
        AppIcon.applyTo(frame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(split, BorderLayout.CENTER);

        JPanel southButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnBack = new JButton("Atras");
        JButton btnOk = new JButton("Generar JSON");
        JButton btnCancel = new JButton("Cancelar");
        southButtons.add(btnBack);
        southButtons.add(btnOk);
        southButtons.add(btnCancel);
        frame.add(southButtons, BorderLayout.SOUTH);

        FormInput[] result = {null};

        btnOk.addActionListener(e -> {
            try {
                FormState state = new FormState(
                        tfTipoNota.getText(),
                        tfNumNota.getText(),
                        String.valueOf(cbUserTipoDoc.getSelectedItem()),
                        tfUserNumDoc.getText(),
                        tfTipoUsuario.getText(),
                        dcNacimiento.getDate(),
                        String.valueOf(cbSexo.getSelectedItem()),
                        tfPaisRes.getText(),
                        tfMun.getText(),
                        tfZona.getText(),
                        String.valueOf(cbIncap.getSelectedItem()),
                        tfPaisOrigen.getText(),
                        ((Number) spConsecUser.getValue()).intValue(),
                        String.valueOf(cbTipoOS.getSelectedItem()),
                        defCodTec,
                        tfNomTec.getText(),
                        ((Number) spCant.getValue()).intValue(),
                        String.valueOf(cbServTipoDoc.getSelectedItem()),
                        tfServNumDoc.getText(),
                        tfVrUnit.getText(),
                        String.valueOf(cbConcepto.getSelectedItem()),
                        ((Number) spValorPM.getValue()).intValue(),
                        tfNumFEV.getText(),
                        ((Number) spConsecServ.getValue()).intValue(),
                        tfMIPRES.getText(),
                        dcSumFecha.getDate(),
                        (Integer) spHora.getValue(),
                        (Integer) spMin.getValue(),
                        issueDate
                );
                result[0] = formInputMapper.map(state);
                frame.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> {
            result[0] = null;
            frame.dispose();
        });
        btnBack.addActionListener(e -> {
            result[0] = FormInput.BACK;
            frame.dispose();
        });

        frame.setSize(1200, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        while (frame.isShowing()) {
            try {
                Thread.sleep(35);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return result[0];
    }

    private static void formAdd(JPanel p, GridBagConstraints c, int row, String label, JComponent comp) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, c);
    }

    private static JTextField ro(String text) {
        JTextField t = new JTextField(text != null ? text : "");
        t.setEditable(false);
        t.setBackground(new Color(245, 245, 245));
        return t;
    }

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
