package xml.json.transformer.application;

import org.w3c.dom.Document;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonBuilderService {

    private static final Map<String, String> NS = Map.of(
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "sts", "dian:gov:co:facturaelectronica:Structures-2-1",
            "xades", "http://uri.etsi.org/01903/v1.3.2#"
    );

    private final XPath xp;
    private String fechaSuministro; // 🔹 Se guarda aquí para reutilizarla

    public JsonBuilderService() {
        xp = newXPath();
    }

    private XPath newXPath() {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return NS.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
            }
            @Override public String getPrefix(String uri) { return null; }
            @Override public Iterator<String> getPrefixes(String uri) { return null; }
        });
        return xp;
    }

    public InvoiceData buildInvoiceData(Document mainXml, Document embeddedXml, String codPrestador) throws Exception {
        InvoiceData invoice = new InvoiceData();

        invoice.numDocumentoIdObligado = getValue(mainXml, "//cbc:CompanyID[@schemeID='8']", "Ingrese el NIT del obligado (schemeID=8)");
        invoice.numFactura = getValue(mainXml, "//cbc:ParentDocumentID", "Ingrese el número de factura");
        invoice.tipoNota = askNullable("Ingrese tipoNota (o deje vacío):");
        invoice.numNota = askNullable("Ingrese numNota (o deje vacío):");

        // ---------- Usuario ----------
        UserData user = new UserData();
        user.tipoDocumentoIdentificacion = askRequired("Ingrese tipoDocumentoIdentificacion (ej: CC):");
        user.numDocumentoIdentificacion = askRequired("Ingrese numDocumentoIdentificacion (ej: 15648042):");
        user.tipoUsuario = askRequired("Ingrese tipoUsuario (ej: 10):");
        user.fechaNacimiento = askRequired("Ingrese fechaNacimiento (ej: 1985-07-08):");
        user.codSexo = askRequired("Ingrese codSexo (M/F):");
        user.codPaisResidencia = askRequired("Ingrese codPaisResidencia (ej: 170):");
        user.codMunicipioResidencia = askRequired("Ingrese codMunicipioResidencia (ej: 23001):");
        user.codZonaTerritorialResidencia = askRequired("Ingrese codZonaTerritorialResidencia (ej: 02):");
        user.incapacidad = askRequired("Ingrese incapacidad (ej: NO):");
        user.codPaisOrigen = askRequired("Ingrese codPaisOrigen (ej: 170):");
        user.consecutivo = Integer.parseInt(askRequired("Ingrese consecutivo (ej: 1):"));

        // ---------- Servicio ----------
        UserData.OtrosServicios os = new UserData.OtrosServicios();
        os.codPrestador = codPrestador;
        os.numAutorizacion = getValue(embeddedXml, "//sts:InvoiceAuthorization", "Ingrese numAutorizacion");

        // ✅ El usuario ingresa la fecha solo UNA VEZ
        fechaSuministro = askFechaSuministro();
        os.fechaSuministroTecnologia = fechaSuministro;

        os.codTecnologiaSalud = getValue(embeddedXml, "//cac:StandardItemIdentification/cbc:ID", "Ingrese codTecnologiaSalud");
        os.nomTecnologiaSalud = getValue(embeddedXml, "//cac:Item/cbc:Description", "Ingrese nomTecnologiaSalud");
        os.numDocumentoIdentificacion = getValue(mainXml, "//cbc:CompanyID[@schemeID='2']", "Ingrese numDocumentoIdentificacion (schemeID=2)");

        String valor = xp.evaluate("string(//cbc:LineExtensionAmount)", embeddedXml);
        if (!valor.isBlank()) {
            valor = valor.replaceAll("[^0-9.]", "");
            double monto = Double.parseDouble(valor);
            os.vrUnitOS = (int) Math.floor(monto);
            os.vrServicio = (int) Math.floor(monto);
        } else {
            os.vrUnitOS = Integer.parseInt(askRequired("Ingrese vrUnitOS (ej: 436737):"));
            os.vrServicio = os.vrUnitOS;
        }

        os.tipoOS = askRequired("Ingrese tipoOS (ej: 02):");
        os.cantidadOS = Integer.parseInt(askRequired("Ingrese cantidadOS (ej: 1):"));
        os.tipoDocumentoIdentificacion = askRequired("Ingrese tipoDocumentoIdentificacion del servicio (ej: CC):");
        os.conceptoRecaudo = askRequired("Ingrese conceptoRecaudo (ej: 03):");
        os.idMIPRES = askNullable("Ingrese idMIPRES (o deje vacío):");
        os.valorPagoModerador = Integer.parseInt(askRequired("Ingrese valorPagoModerador (ej: 0):"));
        os.numFEVPagoModerador = askNullable("Ingrese numFEVPagoModerador (o deje vacío):");
        os.consecutivo = Integer.parseInt(askRequired("Ingrese consecutivo (ej: 1):"));

        user.servicios.otrosServicios.add(os);
        invoice.usuarios.add(user);
        return invoice;
    }

    // 🔹 Devuelve la fecha ingresada (para usarla en el XmlAdapterService)
    public String getFechaSuministro() {
        return fechaSuministro;
    }

    // --------------------- Auxiliares ---------------------
    private String askFechaSuministro() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setLenient(false);
        String value;
        while (true) {
            value = JOptionPane.showInputDialog(null,
                    "Ingrese la fecha de suministro (yyyy-MM-dd HH:mm):\nEjemplo: 2025-09-23 14:19",
                    "Fecha de suministro", JOptionPane.QUESTION_MESSAGE);
            if (value == null || value.trim().isEmpty()) continue;
            try {
                sdf.parse(value.trim());
                return value.trim();
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(null,
                        "⚠️ Formato inválido. Use yyyy-MM-dd HH:mm (ej: 2025-09-23 14:19)",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String getValue(Document doc, String xpath, String prompt) throws XPathExpressionException {
        String value = xp.evaluate("string(" + xpath + ")", doc);
        return value.isBlank() ? askRequired(prompt) : value.trim();
    }

    private String askRequired(String message) {
        String value;
        do {
            value = JOptionPane.showInputDialog(null, message, "Campo obligatorio", JOptionPane.QUESTION_MESSAGE);
            if (value == null || value.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "⚠️ Este campo no puede estar vacío", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } while (value == null || value.trim().isEmpty());
        return value.trim();
    }

    private String askNullable(String message) {
        String value = JOptionPane.showInputDialog(null, message, "Campo opcional", JOptionPane.QUESTION_MESSAGE);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
