package xml.json.transformer.application;

import org.w3c.dom.Document;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

import com.toedter.calendar.JDateChooser;

public class JsonBuilderService {

    // ===== Namespaces =====
    private static final Map<String, String> NS = Map.of(
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "sts", "dian:gov:co:facturaelectronica:Structures-2-1",
            "xades", "http://uri.etsi.org/01903/v1.3.2#"
    );

    private final XPath xp;
    private final LocalDate issueDate; // fecha de IssueDate (XML original) para validar fechaSuministroTecnologia

    private String fechaSuministro; // yyyy-MM-dd HH:mm

    public JsonBuilderService(LocalDate issueDate) {
        this.issueDate = issueDate;
        this.xp = newXPath();
    }

    // ======================= PUBLIC API =======================

    /**
     * Construye InvoiceData usando un solo cuestionario (Swing) con calendario/combos y validaciones.
     */
    public InvoiceData buildInvoiceData(Document mainXml, Document embeddedXml, String codPrestador) throws Exception {
        // -------- Valores por defecto extraídos de XML (solo lectura en el UI donde aplique) --------
        String nitObligado = eval(mainXml, "//cbc:CompanyID[@schemeID='8']");
        String parentDocID = eval(mainXml, "//cbc:ParentDocumentID");

        // Nota descriptiva (encabezado superior de ayuda)
        String noteHeaderRaw = eval(embeddedXml, "//cbc:Note");
        String noteHeader = noteHeaderRaw == null ? "" : noteHeaderRaw.replaceAll("(?i)^\\s*linea\\s+de\\s+negocio\\s*:\\s*", "").trim();

        // Autorización (XML embebido)
        String numAutorizacion = eval(embeddedXml, "//sts:InvoiceAuthorization");

        // Códigos/nombre tecnología desde embebido
        String codTec = eval(embeddedXml, "//cac:StandardItemIdentification/cbc:ID");
        String nomTec = eval(embeddedXml, "//cac:Item/cbc:Description");

        // Monto (LineExtensionAmount) -> enteros
        int vr = 0;
        String valor = eval(embeddedXml, "//cbc:LineExtensionAmount");
        if (valor != null && !valor.isBlank()) {
            valor = valor.replaceAll("[^0-9.]", "");
            try {
                double m = Double.parseDouble(valor);
                vr = (int) Math.floor(m);
            } catch (NumberFormatException ignore) { /* queda 0 */ }
        }

        // **numDocumentoIdentificacion (servicio)** del XML ORIGINAL, ReceiverParty (cualquier schemeID)
        String docIdentServicio = firstNonBlank(
                eval(mainXml, "//cac:ReceiverParty//cac:PartyTaxScheme//cbc:CompanyID"),
                eval(mainXml, "//cac:AccountingCustomerParty//cac:PartyTaxScheme//cbc:CompanyID")
        );

        // --------- Construir UI (un solo cuestionario) ---------
        FormData ans = showQuestionnaire(
                nitObligado, parentDocID, noteHeader,
                codPrestador, numAutorizacion,
                codTec, nomTec,
                vr,
                docIdentServicio
        );
        if (ans == null) return null; // cancelado

        // Validar fecha suministro <= IssueDate
        if (issueDate != null && ans.fechaSum != null) {
            LocalDate fSum = toLocalDate(ans.fechaSum);
            if (fSum.isAfter(issueDate)) {
                JOptionPane.showMessageDialog(null,
                        "La fecha de suministro no puede ser posterior a la IssueDate del XML (" + issueDate + ").",
                        "Validación de fecha", JOptionPane.WARNING_MESSAGE);
                throw new IllegalArgumentException("fechaSuministroTecnologia > IssueDate");
            }
        }

        // Guardar cadena final para el XML transformador
        this.fechaSuministro = ans.fechaSumStr; // yyyy-MM-dd HH:mm

        // ------- Construir objetos de dominio -------
        InvoiceData invoice = new InvoiceData();
        invoice.numDocumentoIdObligado = nitObligado;
        invoice.numFactura = parentDocID;
        invoice.tipoNota = nullIfEmpty(ans.tipoNota);
        invoice.numNota = nullIfEmpty(ans.numNota);

        UserData user = new UserData();
        user.tipoDocumentoIdentificacion = ans.user_tipoDoc;
        user.numDocumentoIdentificacion = ans.user_numDoc;
        user.tipoUsuario = ans.user_tipoUsuario;
        user.fechaNacimiento = ans.user_fechaNacStr; // yyyy-MM-dd
        user.codSexo = ans.user_codSexo;
        user.codPaisResidencia = ans.user_codPaisRes;
        user.codMunicipioResidencia = ans.user_codMunRes;
        user.codZonaTerritorialResidencia = ans.user_codZona;
        user.incapacidad = ans.user_incapacidad;
        user.codPaisOrigen = ans.user_codPaisOrigen;
        user.consecutivo = ans.user_consecutivo;

        UserData.OtrosServicios os = new UserData.OtrosServicios();
        os.codPrestador = codPrestador;
        os.numAutorizacion = numAutorizacion;
        os.idMIPRES = nullIfEmpty(ans.serv_idMIPRES);

        os.fechaSuministroTecnologia = ans.fechaSumStr; // yyyy-MM-dd HH:mm
        os.tipoOS = ans.serv_tipoOS;
        os.codTecnologiaSalud = ans.serv_codTec; // de XML (read-only)
        os.nomTecnologiaSalud = ans.serv_nomTec; // EDITABLE por el usuario
        os.cantidadOS = ans.serv_cant;
        os.tipoDocumentoIdentificacion = ans.serv_tipoDoc;
        os.numDocumentoIdentificacion = ans.serv_numDoc; // ReceiverParty CompanyID
        os.vrUnitOS = ans.serv_vr;     // RO
        os.vrServicio = ans.serv_vr;   // auto = vrUnitOS
        os.conceptoRecaudo = ans.serv_concepto;
        os.valorPagoModerador = ans.serv_valorPagoMod;
        os.numFEVPagoModerador = nullIfEmpty(ans.serv_numFEV);
        os.consecutivo = ans.serv_consecutivo;

        user.servicios.otrosServicios.add(os);
        invoice.usuarios.add(user);

        return invoice;
    }

    public String getFechaSuministro() {
        return fechaSuministro;
    }

    // ======================= UI =======================

    // Datos intermedios para trasladar desde el formulario
    private static final class FormData {
        // cabecera
        String tipoNota;
        String numNota;

        // usuario
        String user_tipoDoc;
        String user_numDoc;
        String user_tipoUsuario;
        Date   user_fechaNac;
        String user_fechaNacStr;
        String user_codSexo;
        String user_codPaisRes;
        String user_codMunRes;
        String user_codZona;
        String user_incapacidad;
        String user_codPaisOrigen;
        Integer user_consecutivo;

        // servicio
        String serv_tipoOS;
        String serv_codTec;
        String serv_nomTec; // editable
        Integer serv_cant;
        String serv_tipoDoc;
        String serv_numDoc;
        Integer serv_vr; // ro
        String serv_concepto;
        Integer serv_valorPagoMod;
        String serv_numFEV;
        Integer serv_consecutivo;
        String serv_idMIPRES;

        // fecha suministro
        Date fechaSum;
        String fechaSumStr; // yyyy-MM-dd HH:mm
    }

    private FormData showQuestionnaire(
            String nitObligado,
            String parentDocID,
            String noteHeader,
            String codPrestador,
            String numAutorizacion,
            String def_codTec,
            String def_nomTec,
            int def_vr,
            String def_numDocServicio
    ) {
        // defaults
        String d_tipoUsuario = "10";
        String d_codPais = "170";
        String d_mun = "23001";
        String d_zona = "02";
        String d_incap = "NO";
        String d_paisOrigen = "170";
        Integer d_consec = 1;

        String d_tipoOS = "02";
        String d_concepto = "03";
        Integer d_valorPagoMod = 0;
        Integer d_consecServ = 1;

        // --- panel base con scroll
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int r = 0;

        // Encabezado (nota)
        JTextArea taHeader = new JTextArea(
                noteHeader == null || noteHeader.isBlank()
                        ? "(Sin nota)"
                        : noteHeader
        );
        taHeader.setEditable(false);
        taHeader.setLineWrap(true);
        taHeader.setWrapStyleWord(true);
        taHeader.setBackground(new Color(250, 250, 250));
        taHeader.setBorder(BorderFactory.createTitledBorder("Mensaje (XML embebido / cbc:Note)"));
        c.gridx = 0; c.gridy = r++; c.gridwidth = 2;
        form.add(taHeader, c);
        c.gridwidth = 1;

        // -------- INVOICE (orden JSON) --------
        // numDocumentoIdObligado (RO)
        r = row(form, c, r, "numDocumentoIdObligado (XML schemeID=8):", ro(nitObligado));
        // numFactura (RO)
        r = row(form, c, r, "numFactura (ParentDocumentID XML):", ro(parentDocID));

        // tipoNota / numNota (opcionales)
        JTextField tfTipoNota = txt(null);
        JTextField tfNumNota = txt(null);
        r = row(form, c, r, "tipoNota (opcional):", tfTipoNota);
        r = row(form, c, r, "numNota (opcional):", tfNumNota);

        // -------- usuario --------
        JComboBox<String> cbUserTipoDoc = new JComboBox<>(new String[]{"CC", "CE", "TI", "PA", "RC", "NIT", "DNI", "PS"});
        cbUserTipoDoc.setSelectedItem("CC");
        r = row(form, c, r, "tipoDocumentoIdentificacion (usuario):", cbUserTipoDoc);

        JTextField tfUserNumDoc = txt(""); // editable por el usuario
        r = row(form, c, r, "numDocumentoIdentificacion (usuario):", tfUserNumDoc);

        JTextField tfTipoUsuario = txt(d_tipoUsuario);
        r = row(form, c, r, "tipoUsuario:", tfTipoUsuario);

        // fechaNacimiento: calendario solo fecha
        JDateChooser dcNacimiento = new JDateChooser();
        dcNacimiento.setDateFormatString("yyyy-MM-dd");
        r = row(form, c, r, "fechaNacimiento (calendario):", dcNacimiento);

        JComboBox<String> cbSexo = new JComboBox<>(new String[]{"M", "F"});
        cbSexo.setSelectedItem("M");
        r = row(form, c, r, "codSexo:", cbSexo);

        JTextField tfCodPais = txt(d_codPais);
        JTextField tfMun = txt(d_mun);
        JTextField tfZona = txt(d_zona);
        r = row(form, c, r, "codPaisResidencia:", tfCodPais);
        r = row(form, c, r, "codMunicipioResidencia:", tfMun);
        r = row(form, c, r, "codZonaTerritorialResidencia:", tfZona);

        JComboBox<String> cbIncap = new JComboBox<>(new String[]{"NO", "SI"});
        cbIncap.setSelectedItem(d_incap);
        r = row(form, c, r, "incapacidad:", cbIncap);

        JTextField tfPaisOrigen = txt(d_paisOrigen);
        r = row(form, c, r, "codPaisOrigen:", tfPaisOrigen);

        JSpinner spConsecUser = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(d_consec), Integer.valueOf(1), Integer.valueOf(9999), Integer.valueOf(1))
        );
        r = row(form, c, r, "consecutivo:", spConsecUser);

        // -------- servicio --------
        // codPrestador + numAutorizacion (RO)
        r = row(form, c, r, "codPrestador (XML/extraído):", ro(nvl(codPrestador, "")));
        r = row(form, c, r, "numAutorizacion (XML):", ro(nvl(numAutorizacion, "")));

        JTextField tfMIPRES = txt(null);
        r = row(form, c, r, "idMIPRES (opcional):", tfMIPRES);

        // fechaSuministro: calendario + hora/minuto
        JPanel pnlFechaSum = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JDateChooser dcSumFecha = new JDateChooser();
        dcSumFecha.setDateFormatString("yyyy-MM-dd");
        JSpinner spHora = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(15), Integer.valueOf(0), Integer.valueOf(23), Integer.valueOf(1))
        );
        JSpinner spMin = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(30), Integer.valueOf(0), Integer.valueOf(59), Integer.valueOf(1))
        );
        pnlFechaSum.add(dcSumFecha);
        pnlFechaSum.add(new JLabel("Hora:"));
        pnlFechaSum.add(spHora);
        pnlFechaSum.add(new JLabel(":"));
        pnlFechaSum.add(spMin);
        r = row(form, c, r, "fechaSuministroTecnologia (fecha+hora):", pnlFechaSum);

        JComboBox<String> cbTipoOS = new JComboBox<>(new String[]{"01", "02", "03", "04", "05"});
        cbTipoOS.setSelectedItem(d_tipoOS);
        r = row(form, c, r, "tipoOS:", cbTipoOS);

        // codTecnologia (RO) y nomTecnologia (EDITABLE)
        r = row(form, c, r, "codTecnologiaSalud (XML):", ro(nvl(def_codTec, "")));

        JTextField tfNomTec = txt(nvl(def_nomTec, ""));
        r = row(form, c, r, "nomTecnologiaSalud (editable):", tfNomTec);

        // cantidad
        JSpinner spCant = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), Integer.valueOf(9999), Integer.valueOf(1))
        );
        r = row(form, c, r, "cantidadOS:", spCant);

        // tipo/num doc servicio
        JComboBox<String> cbServTipoDoc = new JComboBox<>(new String[]{"CC", "CE", "TI", "PA", "RC", "NIT", "DNI", "PS"});
        cbServTipoDoc.setSelectedItem("CC");
        r = row(form, c, r, "tipoDocumentoIdentificacion (servicio):", cbServTipoDoc);

        JTextField tfServNumDoc = txt(nvl(def_numDocServicio, ""));
        r = row(form, c, r, "numDocumentoIdentificacion (servicio):", tfServNumDoc);

        // vrUnitOS RO y vrServicio auto
        JTextField roVrUnit = ro(String.valueOf(def_vr));
        r = row(form, c, r, "vrUnitOS (auto):", roVrUnit);

        JTextField roVrServ = ro(roVrUnit.getText());
        r = row(form, c, r, "vrServicio (auto):", roVrServ);

        JComboBox<String> cbConcepto = new JComboBox<>(new String[]{"01", "02", "03", "04", "05"});
        cbConcepto.setSelectedItem(d_concepto);
        r = row(form, c, r, "conceptoRecaudo:", cbConcepto);

        JSpinner spValorPM = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(d_valorPagoMod), Integer.valueOf(0), Integer.valueOf(Integer.MAX_VALUE), Integer.valueOf(1))
        );
        r = row(form, c, r, "valorPagoModerador:", spValorPM);

        JTextField tfNumFEV = txt(null);
        r = row(form, c, r, "numFEVPagoModerador (opcional):", tfNumFEV);

        JSpinner spConsecServ = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(d_consecServ), Integer.valueOf(1), Integer.valueOf(9999), Integer.valueOf(1))
        );
        r = row(form, c, r, "consecutivoServicio:", spConsecServ);

        // --- contenedor scroll
        JScrollPane scroll = new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(980, 640));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        int ok = JOptionPane.showConfirmDialog(null, scroll, "Datos para generar JSON",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;

        // --- recoger valores
        FormData out = new FormData();

        out.tipoNota = tfTipoNota.getText();
        out.numNota = tfNumNota.getText();

        out.user_tipoDoc = String.valueOf(cbUserTipoDoc.getSelectedItem());
        out.user_numDoc = must(tfUserNumDoc.getText(), "numDocumentoIdentificacion (usuario)");
        out.user_tipoUsuario = must(tfTipoUsuario.getText(), "tipoUsuario");
        out.user_fechaNac = dcNacimiento.getDate();
        out.user_fechaNacStr = dateOrNull(dcNacimiento.getDate(), "yyyy-MM-dd");
        out.user_codSexo = String.valueOf(cbSexo.getSelectedItem());
        out.user_codPaisRes = must(tfCodPais.getText(), "codPaisResidencia");
        out.user_codMunRes = must(tfMun.getText(), "codMunicipioResidencia");
        out.user_codZona = must(tfZona.getText(), "codZonaTerritorialResidencia");
        out.user_incapacidad = String.valueOf(cbIncap.getSelectedItem());
        out.user_codPaisOrigen = must(tfPaisOrigen.getText(), "codPaisOrigen");
        out.user_consecutivo = ((Number) spConsecUser.getValue()).intValue();

        out.serv_tipoOS = String.valueOf(cbTipoOS.getSelectedItem());
        out.serv_codTec = nvl(def_codTec, "");
        out.serv_nomTec = must(tfNomTec.getText(), "nomTecnologiaSalud");
        out.serv_cant = ((Number) spCant.getValue()).intValue();
        out.serv_tipoDoc = String.valueOf(cbServTipoDoc.getSelectedItem());
        out.serv_numDoc = must(tfServNumDoc.getText(), "numDocumentoIdentificacion (servicio)");
        out.serv_vr = parseIntSafe(roVrUnit.getText(), 0);
        out.serv_concepto = String.valueOf(cbConcepto.getSelectedItem());
        out.serv_valorPagoMod = ((Number) spValorPM.getValue()).intValue();
        out.serv_numFEV = tfNumFEV.getText();
        out.serv_consecutivo = ((Number) spConsecServ.getValue()).intValue();
        out.serv_idMIPRES = tfMIPRES.getText();

        // fecha suministro (yyyy-MM-dd HH:mm)
        Date d = dcSumFecha.getDate();
        Integer hh = (Integer) spHora.getValue();
        Integer mm = (Integer) spMin.getValue();
        if (d == null) throw new IllegalArgumentException("Debe seleccionar fecha de suministro.");
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, hh);
        cal.set(Calendar.MINUTE, mm);
        cal.set(Calendar.SECOND, 0);
        out.fechaSum = cal.getTime();
        out.fechaSumStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(out.fechaSum);

        return out;
    }

    // ======================= Helpers UI =======================

    private static int row(JPanel p, GridBagConstraints c, int r, String label, JComponent comp) {
        c.gridx = 0; c.gridy = r; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        JLabel lb = new JLabel(label);
        p.add(lb, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, c);
        return r + 1;
    }

    private static JTextField txt(String s) {
        JTextField t = new JTextField();
        if (s != null) t.setText(s);
        return t;
    }

    private static JTextField ro(String s) {
        JTextField t = new JTextField();
        if (s != null) t.setText(s);
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
        } catch (Exception e) { return def; }
    }

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static String nullIfEmpty(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static LocalDate toLocalDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    // ======================= XPath =======================

    private XPath newXPath() {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
            @Override public String getNamespaceURI(String prefix) {
                return NS.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
            }
            @Override public String getPrefix(String uri) { return null; }
            @Override public Iterator<String> getPrefixes(String uri) { return null; }
        });
        return xp;
    }

    private String eval(Document doc, String xpath) {
        if (doc == null) return null;
        try {
            String v = xp.evaluate("string(" + xpath + ")", doc);
            return v == null ? null : v.trim();
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
