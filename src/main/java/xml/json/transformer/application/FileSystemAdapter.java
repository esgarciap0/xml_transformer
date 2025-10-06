package xml.json.transformer.application;

import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;
import org.w3c.dom.*;
import xml.json.transformer.infrastructure.JsonAdapter;
import xml.json.transformer.infrastructure.XmlAdapter;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileSystemAdapter implements XmlAdapter, JsonAdapter {

    private static String originalCodPrestador = "";

    // Namespaces
    private static final Map<String, String> NS = Map.of(
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "sts", "dian:gov:co:facturaelectronica:Structures-2-1"
    );

    // ------------------------------------------------------------------
    // XML Básico
    // ------------------------------------------------------------------
    @Override
    public Document readXml(String path) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new File(path));
    }

    @Override
    public void writeXml(Document doc, String path) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        t.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)));

        System.out.println("✅ Archivo XML modificado guardado correctamente: " + path);
    }

    @Override
    public void writeJson(Object data, String path) throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), data);
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

    // ------------------------------------------------------------------
    // Transformaciones según el manual
    // ------------------------------------------------------------------
    @Override
    public void applyManualTransformations(Document outerDoc) throws Exception {
        XPath xp = newXPath();

        // 💾 Guardar codPrestador antes de modificar el XML
        originalCodPrestador = xp.evaluate("string(//*[local-name()='Value'][../*[local-name()='Name']='CODIGO PRESTADOR'])", outerDoc);
        System.out.println("💾 codPrestador original capturado: " + originalCodPrestador);

        // Buscar todos los textos dentro de <cbc:Description>
        NodeList descTexts = (NodeList) xp.evaluate("//cbc:Description/text()", outerDoc, XPathConstants.NODESET);

        int processed = 0;
        for (int i = 0; i < descTexts.getLength(); i++) {
            Node textNode = descTexts.item(i);
            String content = textNode.getNodeValue();
            if (content == null) continue;

            String trimmed = content.trim();
            if (!trimmed.startsWith("<") || !trimmed.contains("<Invoice")) continue;

            Document innerDoc = parseInnerXml(trimmed);

            // A-H pasos del manual
            replaceGroupSchemeName(innerDoc);
            removeUnnamespacedElements(innerDoc, "Id");
            renameCodigoPrestador(innerDoc);
            removeUnnamespacedElements(innerDoc, "TotalesCop");
            replaceCustomizationId(innerDoc);
            insertInvoicePeriod(innerDoc);
            adjustValueElements(innerDoc);
            truncateCodigoPrestador(innerDoc);
            removeByQualifiedName(innerDoc, "cac:PrepaidPayment");

            String newContent = serializeXml(innerDoc);
            newContent = newContent.replaceAll("\\n\\s*\\n", "\n").trim();

            textNode.setNodeValue("\n" + newContent + "\n");
            processed++;
        }

        System.out.println("✅ applyManualTransformations: XMLs internos procesados: " + processed);
    }

    private void replaceGroupSchemeName(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList nodes = (NodeList) xp.evaluate("//*[local-name()='Group']", doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element group = (Element) nodes.item(i);
            if (!group.hasAttribute("schemeName"))
                group.setAttribute("schemeName", "Sector Salud");
        }
    }

    private void removeUnnamespacedElements(Document doc, String localName) throws Exception {
        XPath xp = newXPath();
        NodeList nodes = (NodeList) xp.evaluate("//*[local-name()='" + localName + "']", doc, XPathConstants.NODESET);
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) toRemove.add(nodes.item(i));
        for (Node n : toRemove) n.getParentNode().removeChild(n);
    }

    private void renameCodigoPrestador(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList names = (NodeList) xp.evaluate("//*[local-name()='Interoperabilidad']//*[local-name()='Name']", doc, XPathConstants.NODESET);
        for (int i = 0; i < names.getLength(); i++) {
            Node n = names.item(i);
            String original = n.getTextContent().trim();
            n.setTextContent(original.replaceAll("\\s+", "_").toUpperCase());
        }
    }

    private void replaceCustomizationId(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList ids = (NodeList) xp.evaluate("//cbc:CustomizationID", doc, XPathConstants.NODESET);
        for (int i = 0; i < ids.getLength(); i++) {
            Node n = ids.item(i);
            if ("10".equals(n.getTextContent().trim()))
                n.setTextContent("SS-SinAporte");
        }
    }

    private void insertInvoicePeriod(Document doc) throws Exception {
        XPath xp = newXPath();
        Node node = (Node) xp.evaluate("(//cbc:UBLVersionID)[1]", doc, XPathConstants.NODE);
        if (node == null) return;

        Element parent = (Element) node.getParentNode();
        Element invoicePeriod = doc.createElementNS(NS.get("cac"), "cac:InvoicePeriod");

        String[][] items = {
                {"cbc:StartDate", "2025-07-01"},
                {"cbc:StartTime", "00:00:00-05:00"},
                {"cbc:EndDate", "2025-07-31"},
                {"cbc:EndTime", "00:00:00-05:00"}
        };

        for (String[] i : items) {
            String prefix = i[0].split(":")[0];
            String local = i[0].split(":")[1];
            Element e = doc.createElementNS(NS.get(prefix), i[0]);
            e.setTextContent(i[1]);
            invoicePeriod.appendChild(e);
        }

        Node next = node.getNextSibling();
        while (next != null && next.getNodeType() == Node.TEXT_NODE && next.getTextContent().trim().isEmpty()) {
            next = next.getNextSibling();
        }

        if (next != null) parent.insertBefore(invoicePeriod, next);
        else parent.appendChild(invoicePeriod);
    }

    private void adjustValueElements(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList values = (NodeList) xp.evaluate("//*[local-name()='Value']", doc, XPathConstants.NODESET);
        for (int i = 0; i < values.getLength(); i++) {
            Element v = (Element) values.item(i);
            String text = v.getTextContent().trim();
            if (text.equalsIgnoreCase("Cobertura Póliza SOAT")) {
                v.setAttribute("schemeID", "10");
                v.setAttribute("schemeName", "salud_cobertuta.gc");
            } else if (text.equalsIgnoreCase("Pago por evento")) {
                v.setAttribute("schemeID", "04");
                v.setAttribute("schemeName", "salud_modalidad_pago.gc");
            }
        }
    }

    private void truncateCodigoPrestador(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList values = (NodeList) xp.evaluate("//*[local-name()='Value']", doc, XPathConstants.NODESET);
        for (int i = 0; i < values.getLength(); i++) {
            Element v = (Element) values.item(i);
            if (v.getTextContent().matches("\\d{12,}")) {
                String digits = v.getTextContent().substring(0, 10);
                v.setTextContent(digits);
            }
        }
    }

    private void removeByQualifiedName(Document doc, String qName) throws Exception {
        String[] parts = qName.split(":");
        if (parts.length != 2) return;
        String ns = NS.get(parts[0]);
        String local = parts[1];
        NodeList nodes = doc.getElementsByTagNameNS(ns, local);
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++)
            toRemove.add(nodes.item(i));
        for (Node n : toRemove)
            n.getParentNode().removeChild(n);
    }

    private Document parseInnerXml(String xmlContent) throws Exception {
        String cleaned = xmlContent.replaceFirst("<\\?xml.*?\\?>", "").trim();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(cleaned.getBytes(StandardCharsets.UTF_8)));
    }

    private String serializeXml(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    // ------------------------------------------------------------------
    // JSON Builder (con valores por defecto)
    // ------------------------------------------------------------------
    @Override
    public InvoiceData buildInvoiceData(Document doc) throws Exception {
        XPath xp = newXPath();
        InvoiceData invoice = new InvoiceData();

        invoice.numDocumentoIdObligado = xp.evaluate("string(//cbc:CompanyID)", doc);
        if (invoice.numDocumentoIdObligado.isBlank()) invoice.numDocumentoIdObligado = "901829771";

        invoice.numFactura = xp.evaluate("string(//cbc:ParentDocumentID)", doc);
        if (invoice.numFactura.isBlank()) invoice.numFactura = "SER63";

        UserData user = new UserData();
        user.tipoDocumentoIdentificacion = "CC";
        user.numDocumentoIdentificacion = "15648042";
        user.tipoUsuario = "10";
        user.fechaNacimiento = "1985-07-08";
        user.codSexo = "M";
        user.codPaisResidencia = "170";
        user.codMunicipioResidencia = "23001";
        user.codZonaTerritorialResidencia = "02";
        user.incapacidad = "NO";
        user.codPaisOrigen = "170";
        user.consecutivo = 1;

        UserData.OtrosServicios os = new UserData.OtrosServicios();
        os.codPrestador = originalCodPrestador.isBlank() ? "230010254701" : originalCodPrestador;
        os.numAutorizacion = xp.evaluate("string(//sts:InvoiceAuthorization)", doc);
        if (os.numAutorizacion.isBlank()) os.numAutorizacion = "18764093822061";
        os.idMIPRES = null;
        os.fechaSuministroTecnologia = "2025-07-25 14:19";
        os.tipoOS = "02";
        os.codTecnologiaSalud = "601T01";
        os.nomTecnologiaSalud = "TRASLADO PRIMARIO DE ACCIDENTES DE TRANSITO 202";
        os.cantidadOS = 1;
        os.tipoDocumentoIdentificacion = "CC";
        os.numDocumentoIdentificacion = "860002400";
        os.vrUnitOS = 436737;
        os.vrServicio = 436737;
        os.conceptoRecaudo = "03";
        os.valorPagoModerador = 0;
        os.numFEVPagoModerador = null;
        os.consecutivo = 1;

        user.servicios.otrosServicios.add(os);
        invoice.usuarios.add(user);

        return invoice;
    }
}
