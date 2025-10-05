package xml.json.transformer.infrastructure;

import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;
import org.w3c.dom.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileSystemAdapter implements XmlAdapter, JsonAdapter {

    private static final Map<String, String> NS = Map.of(
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "sts", "dian:gov:co:facturaelectronica:Structures-2-1"
    );

    // ------------------------------------------------------------------
    // XML básico
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
    // Transformaciones según el manual (A–I)
    // ------------------------------------------------------------------
    @Override
    public void applyManualTransformations(Document outerDoc) throws Exception {
        XPath xp = newXPath();
        NodeList descTexts = (NodeList) xp.evaluate("//cbc:Description/text()", outerDoc, XPathConstants.NODESET);

        int processed = 0;
        for (int i = 0; i < descTexts.getLength(); i++) {
            Node textNode = descTexts.item(i);
            String content = textNode.getNodeValue();
            if (content == null) continue;
            String trimmed = content.trim();

            if (!trimmed.startsWith("<") || !trimmed.contains("<Invoice")) continue;

            Document innerDoc = parseInnerXml(trimmed);

            replaceGroupSchemeName(innerDoc); // A
            removeUnnamespacedElements(innerDoc, "Id"); // B
            renameCodigoPrestador(innerDoc); // C
            removeUnnamespacedElements(innerDoc, "TotalesCop"); // D
            replaceCustomizationId(innerDoc); // E
            insertInvoicePeriod(innerDoc); // F
            adjustValueElements(innerDoc); // G
            truncateCodigoPrestador(innerDoc); // H
            removeByQualifiedName(innerDoc, "cac:PrepaidPayment"); // I

            String newContent = serializeXml(innerDoc).replaceAll("\\n\\s*\\n", "\n").trim();
            textNode.setNodeValue("\n" + newContent + "\n");
            processed++;
        }

        System.out.println("✅ applyManualTransformations: XMLs internos procesados: " + processed);
    }

    // A) Reemplazar <Group> por <Group schemeName="Sector Salud">
    private void replaceGroupSchemeName(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList nodes = (NodeList) xp.evaluate("//*[local-name()='Group']", doc, XPathConstants.NODESET);
        int changed = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element group = (Element) nodes.item(i);
            if (!group.hasAttribute("schemeName")) {
                group.setAttribute("schemeName", "Sector Salud");
                changed++;
            }
        }
        System.out.println("🟢 replaceGroupSchemeName: " + changed + " <Group> actualizados");
    }

    // B, D) Quitar <Id> y <TotalesCop>
    private void removeUnnamespacedElements(Document doc, String localName) throws Exception {
        XPath xp = newXPath();
        NodeList nodes = (NodeList) xp.evaluate("//*[local-name()='" + localName + "']", doc, XPathConstants.NODESET);
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            toRemove.add(nodes.item(i));
        }
        for (Node n : toRemove) {
            Node parent = n.getParentNode();
            if (parent != null) parent.removeChild(n);
        }
        System.out.println("🗑 Eliminados " + toRemove.size() + " nodos <" + localName + ">");
    }

    // C) Renombrar <Name>... ...</Name> → <Name>..._...</Name> dentro de <Interoperabilidad>
    private void renameCodigoPrestador(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList names = (NodeList) xp.evaluate("//*[local-name()='Interoperabilidad']//*[local-name()='Name']", doc, XPathConstants.NODESET);
        int changes = 0;

        for (int i = 0; i < names.getLength(); i++) {
            Node n = names.item(i);
            String original = n.getTextContent().trim();
            String modified = original.replaceAll("\\s+", "_").toUpperCase();
            if (!modified.equals(original)) {
                n.setTextContent(modified);
                changes++;
            }
        }
        System.out.println("🟢 renameCodigoPrestador: " + changes + " etiquetas <Name> modificadas");
    }

    // E) Reemplazar <cbc:CustomizationID>10</cbc:CustomizationID> → SS-SinAporte
    private void replaceCustomizationId(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList ids = (NodeList) xp.evaluate("//cbc:CustomizationID", doc, XPathConstants.NODESET);
        int count = 0;
        for (int i = 0; i < ids.getLength(); i++) {
            Node n = ids.item(i);
            if (n.getTextContent().trim().equals("10")) {
                n.setTextContent("SS-SinAporte");
                count++;
            }
        }
        System.out.println("🟢 replaceCustomizationId: " + count + " reemplazos realizados");
    }

    // F) Insertar bloque <cac:InvoicePeriod> después de <cbc:UBLVersionID>
    private void insertInvoicePeriod(Document doc) throws Exception {
        XPath xp = newXPath();
        Node node = (Node) xp.evaluate("(//cbc:UBLVersionID)[1]", doc, XPathConstants.NODE);
        if (node == null) {
            System.out.println("⚠️ No se encontró <cbc:UBLVersionID>");
            return;
        }

        Element parent = (Element) node.getParentNode();
        Element invoicePeriod = doc.createElementNS(NS.get("cac"), "cac:InvoicePeriod");

        Element startDate = doc.createElementNS(NS.get("cbc"), "cbc:StartDate");
        startDate.setTextContent("2025-07-01");
        Element startTime = doc.createElementNS(NS.get("cbc"), "cbc:StartTime");
        startTime.setTextContent("00:00:00-05:00");
        Element endDate = doc.createElementNS(NS.get("cbc"), "cbc:EndDate");
        endDate.setTextContent("2025-07-31");
        Element endTime = doc.createElementNS(NS.get("cbc"), "cbc:EndTime");
        endTime.setTextContent("00:00:00-05:00");

        invoicePeriod.appendChild(startDate);
        invoicePeriod.appendChild(startTime);
        invoicePeriod.appendChild(endDate);
        invoicePeriod.appendChild(endTime);

        Node ref = node.getNextSibling();
        while (ref != null && ref.getNodeType() == Node.TEXT_NODE && ref.getTextContent().trim().isEmpty()) {
            ref = ref.getNextSibling();
        }
        if (ref != null) parent.insertBefore(invoicePeriod, ref);
        else parent.appendChild(invoicePeriod);

        System.out.println("🟢 insertInvoicePeriod: bloque agregado correctamente");
    }

    // G) Ajustar atributos de Value
    private void adjustValueElements(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList values = (NodeList) xp.evaluate("//*[local-name()='Value']", doc, XPathConstants.NODESET);
        for (int i = 0; i < values.getLength(); i++) {
            Element v = (Element) values.item(i);
            String text = v.getTextContent().trim();
            if (text.equalsIgnoreCase("Cobertura Póliza SOAT")) {
                v.setAttribute("schemeID", "10");
                v.setAttribute("schemeName", "salud_cobertuta.gc");
                v.setTextContent("Cobertura Póliza SOAT ");
            } else if (text.equalsIgnoreCase("Pago por evento")) {
                v.setAttribute("schemeID", "04");
                v.setAttribute("schemeName", "salud_modalidad_pago.gc");
            }
        }
    }

    // H) Truncar códigos numéricos largos
    private void truncateCodigoPrestador(Document doc) throws Exception {
        XPath xp = newXPath();
        NodeList values = (NodeList) xp.evaluate("//*[local-name()='Value']", doc, XPathConstants.NODESET);
        for (int i = 0; i < values.getLength(); i++) {
            Element v = (Element) values.item(i);
            if (v.getTextContent().matches("\\d{12,}")) {
                v.setTextContent(v.getTextContent().substring(0, 10));
            }
        }
    }

    // I) Eliminar nodos por nombre calificado (ej: cac:PrepaidPayment)
    private void removeByQualifiedName(Document doc, String qName) throws Exception {
        String[] parts = qName.split(":");
        if (parts.length != 2) return;
        String ns = NS.get(parts[0]);
        String local = parts[1];
        NodeList nodes = doc.getElementsByTagNameNS(ns, local);
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) toRemove.add(nodes.item(i));
        for (Node n : toRemove) n.getParentNode().removeChild(n);
        System.out.println("🗑 removeByQualifiedName: eliminados " + toRemove.size() + " <" + qName + ">");
    }
    @Override
    public InvoiceData buildInvoiceData(Document doc) throws Exception {
        XPath xp = newXPath();
        InvoiceData invoice = new InvoiceData();

        invoice.numDocumentoIdObligado = xp.evaluate("string(//cac:AccountingSupplierParty//cbc:CompanyID)", doc);
        invoice.numFactura = xp.evaluate("string(//cbc:ID)", doc);

        UserData user = new UserData();
        String note = xp.evaluate("string(//cbc:Note)", doc);

        // Extraer CC del paciente
        Matcher mDoc = Pattern.compile("CC\\.\\s*(\\d{6,})").matcher(note);
        if (mDoc.find()) {
            user.tipoDocumentoIdentificacion = "CC";
            user.numDocumentoIdentificacion = mDoc.group(1);
        }

        // Extraer nombre del paciente
        Matcher mName = Pattern.compile("PACIENTE\\s+(.+?)\\s+CC\\.", Pattern.CASE_INSENSITIVE).matcher(note);
        if (mName.find()) {
            System.out.println("Paciente: " + mName.group(1));
        }

        user.tipoUsuario = "10";
        user.codPaisResidencia = "170";
        user.codPaisOrigen = "170";
        user.incapacidad = "NO";
        user.consecutivo = 1;

        NodeList lines = (NodeList) xp.evaluate("//cac:InvoiceLine", doc, XPathConstants.NODESET);
        for (int i = 0; i < lines.getLength(); i++) {
            Element line = (Element) lines.item(i);
            UserData.OtrosServicios os = new UserData.OtrosServicios();

            os.codPrestador = xp.evaluate("string(//cbc:Value[../cbc:Name='CODIGO_PRESTADOR'])", doc);
            os.numAutorizacion = xp.evaluate("string(//sts:InvoiceAuthorization)", doc);
            os.codTecnologiaSalud = xp.evaluate("string(cac:Item/cac:StandardItemIdentification/cbc:ID)", line);
            os.nomTecnologiaSalud = xp.evaluate("string(cac:Item/cbc:Description)", line);
            os.cantidadOS = parseInt(xp.evaluate("string(cbc:InvoicedQuantity)", line));
            os.vrUnitOS = parseInt(xp.evaluate("string(cac:Price/cbc:PriceAmount)", line));
            os.vrServicio = parseInt(xp.evaluate("string(cbc:LineExtensionAmount)", line));

            user.servicios.otrosServicios.add(os);
        }

        invoice.usuarios.add(user);
        return invoice;
    }

    private Integer parseInt(String s) {
        try {
            String clean = s.replaceAll("[^0-9]", "");
            return clean.isEmpty() ? null : Integer.parseInt(clean);
        } catch (Exception e) {
            return null;
        }
    }


    // ------------------------------------------------------------------
    // Helpers XML
    // ------------------------------------------------------------------
    private Document parseInnerXml(String xmlContent) throws Exception {
        String cleaned = xmlContent.replaceFirst("<\\?xml.*?\\?>", "").trim();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(cleaned.getBytes(StandardCharsets.UTF_8)));
    }

    private String serializeXml(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString().replaceAll("\\n\\s*\\n", "\n").trim();
    }
}
