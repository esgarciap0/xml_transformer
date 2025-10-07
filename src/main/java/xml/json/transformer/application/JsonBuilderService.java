package xml.json.transformer.application;

import com.toedter.calendar.JDateChooser;
import org.w3c.dom.Document;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;
import xml.json.transformer.ui.DateTimePicker;
import xml.json.transformer.ui.UiDialogs;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.time.*;
import java.util.*;

/**
 * Builds InvoiceData using ONE questionnaire, keeping field order exactly as JSON structure.
 * Validates fechaSuministroTecnologia <= IssueDate, and separates user vs service document fields.
 */
public class JsonBuilderService {

    private static final Map<String, String> NS = Map.of(
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "sts", "dian:gov:co:facturaelectronica:Structures-2-1",
            "xades", "http://uri.etsi.org/01903/v1.3.2#"
    );

    private final XPath xp;
    private final LocalDate issueDate;
    /** yyyy-MM-dd HH:mm kept for XmlAdapterService */
    private String fechaSuministro;

    public JsonBuilderService(LocalDate issueDate) {
        this.issueDate = Objects.requireNonNull(issueDate, "issueDate");
        this.xp = newXPath();
    }

    private XPath newXPath() {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
            @Override public String getNamespaceURI(String prefix) { return NS.getOrDefault(prefix, XMLConstants.NULL_NS_URI); }
            @Override public String getPrefix(String uri) { return null; }
            @Override public Iterator<String> getPrefixes(String uri) { return null; }
        });
        return xp;
    }

    public InvoiceData buildInvoiceData(Document mainXml, Document embeddedXml, String codPrestador) throws Exception {
        // ---- Values from XML (to display read-only) ----
        String nitObligado = eval(mainXml, "//cbc:CompanyID[@schemeID='8']");
        String parentDocID = eval(mainXml, "//cbc:ParentDocumentID");
        String numAutorizacion = eval(embeddedXml, "//sts:InvoiceAuthorization");
        String codTecnologia = eval(embeddedXml, "//cac:StandardItemIdentification/cbc:ID");
        String nomTecnologia = eval(embeddedXml, "//cac:Item/cbc:Description");
        String docIdentObligadoScheme2 = eval(mainXml, "//cbc:CompanyID[@schemeID='2']");
        String lineAmountRaw = eval(embeddedXml, "//cbc:LineExtensionAmount").replaceAll("[^0-9.]", "");

        // ---- Questionnaire in exact JSON order ----
        Map<String, Object> ans;
        LocalDate fechaNacimiento;
        LocalDateTime fechaSumLdt;

        while (true) {
            LinkedHashMap<String, JComponent> fields = new LinkedHashMap<>();

            // 1) Root level, same order:
            fields.put("numDocumentoIdObligado (XML schemeID=8)", UiDialogs.ro(nitObligado));
            fields.put("numFactura (ParentDocumentID XML)", UiDialogs.ro(parentDocID));
            fields.put("tipoNota (opcional)", UiDialogs.tx(null));
            fields.put("numNota (opcional)", UiDialogs.tx(null));

            // 2) usuarios[0] object (same order as JSON provided):
            //    tipoDocumentoIdentificacion, numDocumentoIdentificacion, tipoUsuario, fechaNacimiento, codSexo, codPaisResidencia,
            //    codMunicipioResidencia, codZonaTerritorialResidencia, incapacidad, codPaisOrigen, consecutivo
            fields.put("tipoDocumentoIdentificacion (usuario)", UiDialogs.cb(Defaults.TIPO_DOC, "CC"));
            fields.put("numDocumentoIdentificacion (usuario)", UiDialogs.tx(null));

            fields.put("tipoUsuario", UiDialogs.tx(Defaults.TIPO_USUARIO));

            JDateChooser fechaNacChooser = new JDateChooser();
            fechaNacChooser.setDateFormatString("yyyy-MM-dd");
            fields.put("fechaNacimiento (calendario)", fechaNacChooser);

            fields.put("codSexo", UiDialogs.cb(Defaults.SEXO, "M"));
            fields.put("codPaisResidencia", UiDialogs.tx(Defaults.COD_PAIS_RESIDENCIA));
            fields.put("codMunicipioResidencia", UiDialogs.tx(Defaults.COD_MPIO_RESIDENCIA));
            fields.put("codZonaTerritorialResidencia", UiDialogs.tx(Defaults.COD_ZONA_TERRITORIAL));
            fields.put("incapacidad", UiDialogs.cb(Defaults.INCAPACIDAD, "NO"));
            fields.put("codPaisOrigen", UiDialogs.tx(Defaults.COD_PAIS_ORIGEN));
            fields.put("consecutivo", UiDialogs.intSpinner(1, 9999, 1, Defaults.CONSECUTIVO));

            // 3) usuarios[0].servicios.otrosServicios[0] (same order):
            //    codPrestador, numAutorizacion, idMIPRES, fechaSuministroTecnologia, tipoOS,
            //    codTecnologiaSalud, nomTecnologiaSalud, cantidadOS,
            //    tipoDocumentoIdentificacion, numDocumentoIdentificacion,
            //    vrUnitOS, vrServicio, conceptoRecaudo, valorPagoModerador,
            //    numFEVPagoModerador, consecutivo
            fields.put("codPrestador (XML/extraído)", UiDialogs.ro(codPrestador));
            fields.put("numAutorizacion (XML)", UiDialogs.ro(numAutorizacion));
            fields.put("idMIPRES (opcional)", UiDialogs.tx(null));

            DateTimePicker fechaSumPicker = new DateTimePicker();
            fechaSumPicker.set(LocalDateTime.now());
            fields.put("fechaSuministroTecnologia (fecha+hora)", fechaSumPicker);

            fields.put("tipoOS", UiDialogs.tx(Defaults.TIPO_OS));
            fields.put("codTecnologiaSalud (XML)", UiDialogs.ro(codTecnologia));
            fields.put("nomTecnologiaSalud (XML)", UiDialogs.ro(nomTecnologia));
            fields.put("cantidadOS", UiDialogs.cb(Defaults.CANTIDAD, 1));

            fields.put("tipoDocumentoIdentificacion (servicio)", UiDialogs.cb(Defaults.TIPO_DOC, "CC"));
            fields.put("numDocumentoIdentificacion (servicio)", UiDialogs.tx(docIdentObligadoScheme2));

            // vrUnitOS / vrServicio shown read-only if comes from XML; both equal
            int amount = 0;
            if (!lineAmountRaw.isBlank()) {
                try { amount = (int) Math.floor(Double.parseDouble(lineAmountRaw)); } catch (NumberFormatException ignore) {}
            }
            fields.put("vrUnitOS (auto)", UiDialogs.ro(String.valueOf(amount)));
            fields.put("vrServicio (auto)", UiDialogs.ro(String.valueOf(amount)));

            fields.put("conceptoRecaudo", UiDialogs.tx(Defaults.CONCEPTO_RECAUDO));
            fields.put("valorPagoModerador", UiDialogs.intSpinner(0, 10_000_000, 1, Defaults.VALOR_PAGO_MODERADOR));
            fields.put("numFEVPagoModerador (opcional)", UiDialogs.tx(null));
            fields.put("consecutivoServicio", UiDialogs.intSpinner(1, 9999, 1, Defaults.CONSECUTIVO));

            ans = UiDialogs.questionnaire("Datos para generar JSON", fields);
            if (ans == null) throw new RuntimeException("Operation cancelled.");

            // Pull dates
            var dNac = (java.util.Date) ans.get("fechaNacimiento (calendario)");
            fechaSumLdt = (java.time.LocalDateTime) ans.get("fechaSuministroTecnologia (fecha+hora)");
            if (dNac == null || fechaSumLdt == null) {
                JOptionPane.showMessageDialog(null, "Seleccione ambas fechas.", "Faltan datos", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            fechaNacimiento = dNac.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            // Validate fecha suministro
            if (fechaSumLdt.toLocalDate().isAfter(issueDate)) {
                JOptionPane.showMessageDialog(null,
                        "La fecha de suministro no puede ser superior a " + issueDate,
                        "Fecha inválida", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            break; // all good
        }

        // ---- Build result respecting JSON structure ----
        InvoiceData invoice = new InvoiceData();
        invoice.numDocumentoIdObligado = eval(mainXml, "//cbc:CompanyID[@schemeID='8']"); // same as shown
        invoice.numFactura = eval(mainXml, "//cbc:ParentDocumentID");
        invoice.tipoNota = opt(ans.get("tipoNota (opcional)"));
        invoice.numNota = opt(ans.get("numNota (opcional)"));

        UserData u = new UserData();
        u.tipoDocumentoIdentificacion = reqStr(ans.get("tipoDocumentoIdentificacion (usuario)"), "tipoDocumentoIdentificacion (usuario)");
        u.numDocumentoIdentificacion  = reqStr(ans.get("numDocumentoIdentificacion (usuario)"),  "numDocumentoIdentificacion (usuario)");

        u.tipoUsuario = reqStr(ans.get("tipoUsuario"), "tipoUsuario");
        u.fechaNacimiento = fechaNacimiento.toString();
        u.codSexo = reqStr(ans.get("codSexo"), "codSexo");
        u.codPaisResidencia = reqStr(ans.get("codPaisResidencia"), "codPaisResidencia");
        u.codMunicipioResidencia = reqStr(ans.get("codMunicipioResidencia"), "codMunicipioResidencia");
        u.codZonaTerritorialResidencia = reqStr(ans.get("codZonaTerritorialResidencia"), "codZonaTerritorialResidencia");
        u.incapacidad = reqStr(ans.get("incapacidad"), "incapacidad");
        u.codPaisOrigen = reqStr(ans.get("codPaisOrigen"), "codPaisOrigen");
        u.consecutivo = (Integer) ans.get("consecutivo");

        UserData.OtrosServicios os = new UserData.OtrosServicios();
        os.codPrestador = codPrestador;
        os.numAutorizacion = eval(embeddedXml, "//sts:InvoiceAuthorization");
        os.idMIPRES = opt(ans.get("idMIPRES (opcional)"));

        this.fechaSuministro = fechaSumLdt.toString().replace('T', ' ').substring(0, 16);
        os.fechaSuministroTecnologia = this.fechaSuministro;

        os.tipoOS = reqStr(ans.get("tipoOS"), "tipoOS");
        os.codTecnologiaSalud = eval(embeddedXml, "//cac:StandardItemIdentification/cbc:ID");
        os.nomTecnologiaSalud = eval(embeddedXml, "//cac:Item/cbc:Description");
        os.cantidadOS = (Integer) Objects.requireNonNull(ans.get("cantidadOS"), "cantidadOS");

        os.tipoDocumentoIdentificacion = reqStr(ans.get("tipoDocumentoIdentificacion (servicio)"), "tipoDocumentoIdentificacion (servicio)");
        os.numDocumentoIdentificacion  = reqStr(ans.get("numDocumentoIdentificacion (servicio)"),  "numDocumentoIdentificacion (servicio)");

        int amount = 0;
        if (!lineAmountRaw.isBlank()) {
            try { amount = (int) Math.floor(Double.parseDouble(lineAmountRaw)); } catch (NumberFormatException ignore) {}
        }
        os.vrUnitOS = amount;
        os.vrServicio = amount;

        os.conceptoRecaudo = reqStr(ans.get("conceptoRecaudo"), "conceptoRecaudo");
        os.valorPagoModerador = (Integer) ans.get("valorPagoModerador");
        os.numFEVPagoModerador = opt(ans.get("numFEVPagoModerador (opcional)"));
        os.consecutivo = (Integer) ans.get("consecutivoServicio");

        u.servicios.otrosServicios.add(os);
        invoice.usuarios.add(u);
        return invoice;
    }

    public String getFechaSuministro() { return fechaSuministro; }

    // ---- helpers ----
    private String eval(Document doc, String xpath) throws XPathExpressionException {
        String v = xp.evaluate("string(" + xpath + ")", doc);
        return v == null ? "" : v.trim();
    }
    private static String opt(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
    private static String reqStr(Object o, String label) {
        if (o == null) throw new IllegalArgumentException("Missing required: " + label);
        String s = o.toString().trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Missing required: " + label);
        return s;
    }
}
