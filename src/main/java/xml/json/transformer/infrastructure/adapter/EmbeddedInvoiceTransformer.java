package xml.json.transformer.infrastructure.adapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

final class EmbeddedInvoiceTransformer {

    void transform(Document outerDoc, String fechaSuministro) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList descTexts = (NodeList) xp.evaluate("//cbc:Description/text()", outerDoc, XPathConstants.NODESET);

        int processed = 0;
        for (int i = 0; i < descTexts.getLength(); i++) {
            Node textNode = descTexts.item(i);
            String content = textNode.getNodeValue();
            if (content == null) {
                continue;
            }

            String trimmed = content.trim();
            if (!trimmed.startsWith("<") || !trimmed.contains("<Invoice")) {
                continue;
            }

            Document innerDoc = XmlSupport.parseInnerXml(trimmed);
            replaceGroupSchemeName(innerDoc);
            removeUnnamespacedElements(innerDoc, "Id");
            renameCodigoPrestador(innerDoc);
            removeUnnamespacedElements(innerDoc, "TotalesCop");
            replaceCustomizationId(innerDoc);
            adjustValueElements(innerDoc);
            truncateCodigoPrestador(innerDoc);
            removeByQualifiedName(innerDoc, "cac:PrepaidPayment");
            insertInvoicePeriod(innerDoc, fechaSuministro);

            String newContent = XmlSupport.serializeXml(innerDoc).replaceAll("\\n\\s*\\n", "\n").trim();
            textNode.setNodeValue("\n" + newContent + "\n");
            processed++;
        }

        System.out.println("applyManualTransformations: XMLs internos procesados: " + processed);
    }

    Document extractEmbeddedXml(Document doc) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList descTexts = (NodeList) xp.evaluate("//cbc:Description/text()", doc, XPathConstants.NODESET);
        for (int i = 0; i < descTexts.getLength(); i++) {
            String cdata = descTexts.item(i).getNodeValue().trim();
            if (cdata.startsWith("<")) {
                return XmlSupport.parseInnerXml(cdata);
            }
        }
        return null;
    }

    private void replaceGroupSchemeName(Document doc) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList nodes = (NodeList) xp.evaluate("//*[local-name()='Group']", doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element group = (Element) nodes.item(i);
            if (!group.hasAttribute("schemeName")) {
                group.setAttribute("schemeName", "Sector Salud");
            }
        }
    }

    private void removeUnnamespacedElements(Document doc, String localName) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList nodes = (NodeList) xp.evaluate("//*[local-name()='" + localName + "']", doc, XPathConstants.NODESET);
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            toRemove.add(nodes.item(i));
        }
        for (Node node : toRemove) {
            node.getParentNode().removeChild(node);
        }
    }

    private void renameCodigoPrestador(Document doc) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList names = (NodeList) xp.evaluate(
                "//*[local-name()='Interoperabilidad']//*[local-name()='Name']",
                doc,
                XPathConstants.NODESET
        );
        for (int i = 0; i < names.getLength(); i++) {
            Node node = names.item(i);
            String original = node.getTextContent().trim();
            node.setTextContent(original.replaceAll("\\s+", "_").toUpperCase());
        }
    }

    private void replaceCustomizationId(Document doc) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList ids = (NodeList) xp.evaluate("//cbc:CustomizationID", doc, XPathConstants.NODESET);
        for (int i = 0; i < ids.getLength(); i++) {
            Node node = ids.item(i);
            if ("10".equals(node.getTextContent().trim())) {
                node.setTextContent("SS-SinAporte");
            }
        }
    }

    private void adjustValueElements(Document doc) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList values = (NodeList) xp.evaluate("//*[local-name()='Value']", doc, XPathConstants.NODESET);
        for (int i = 0; i < values.getLength(); i++) {
            Element value = (Element) values.item(i);
            String text = value.getTextContent().trim();
            if (text.equalsIgnoreCase("Cobertura Póliza SOAT")) {
                value.setAttribute("schemeID", "04");
                value.setAttribute("schemeName", "salud_cobertuta.gc");
            } else if (text.equalsIgnoreCase("Pago por evento")) {
                value.setAttribute("schemeID", "04");
                value.setAttribute("schemeName", "salud_modalidad_pago.gc");
            }
        }
    }

    private void truncateCodigoPrestador(Document doc) throws Exception {
        XPath xp = XmlSupport.newXPath();
        NodeList values = (NodeList) xp.evaluate(
                "//*[local-name()='AdditionalInformation' and " +
                        "(normalize-space(*[local-name()='Name'][1])='CODIGO PRESTADOR' or " +
                        "normalize-space(*[local-name()='Name'][1])='CODIGO_PRESTADOR')]" +
                        "/*[local-name()='Value'][1]",
                doc,
                XPathConstants.NODESET
        );
        for (int i = 0; i < values.getLength(); i++) {
            Element value = (Element) values.item(i);
            String text = value.getTextContent().trim();
            if (text.matches("\\d{12,}")) {
                value.setTextContent(text.substring(0, 10));
            }
        }
    }

    private void removeByQualifiedName(Document doc, String qName) {
        String[] parts = qName.split(":");
        if (parts.length != 2) {
            return;
        }
        String namespace = XmlSupport.namespace(parts[0]);
        String localName = parts[1];
        NodeList nodes = doc.getElementsByTagNameNS(namespace, localName);
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            toRemove.add(nodes.item(i));
        }
        for (Node node : toRemove) {
            node.getParentNode().removeChild(node);
        }
    }

    private void insertInvoicePeriod(Document doc, String fechaSuministro) throws Exception {
        if (fechaSuministro == null || fechaSuministro.isBlank()) {
            return;
        }

        XPath xp = XmlSupport.newXPath();
        Node ublVersionNode = (Node) xp.evaluate("(//cbc:UBLVersionID)[1]", doc, XPathConstants.NODE);
        if (ublVersionNode == null) {
            return;
        }

        Element parent = (Element) ublVersionNode.getParentNode();
        Element invoicePeriod = (Element) xp.evaluate("(//cac:InvoicePeriod)[1]", doc, XPathConstants.NODE);
        boolean alreadyExists = invoicePeriod != null;
        if (!alreadyExists) {
            invoicePeriod = doc.createElementNS(XmlSupport.namespace("cac"), "cac:InvoicePeriod");
        } else {
            while (invoicePeriod.hasChildNodes()) {
                invoicePeriod.removeChild(invoicePeriod.getFirstChild());
            }
        }

        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdfInput.parse(fechaSuministro));

        cal.add(Calendar.DATE, -1);
        String startDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        cal.add(Calendar.DATE, 1);
        String endDate = fechaSuministro.substring(0, 10);

        addChild(doc, invoicePeriod, "cbc:StartDate", startDate);
        addChild(doc, invoicePeriod, "cbc:StartTime", "00:00:00-05:00");
        addChild(doc, invoicePeriod, "cbc:EndDate", endDate);
        addChild(doc, invoicePeriod, "cbc:EndTime", "00:00:00-05:00");

        if (!alreadyExists) {
            Node next = ublVersionNode.getNextSibling();
            while (next != null && next.getNodeType() == Node.TEXT_NODE && next.getTextContent().trim().isEmpty()) {
                next = next.getNextSibling();
            }

            if (next != null) {
                parent.insertBefore(invoicePeriod, next);
            } else {
                parent.appendChild(invoicePeriod);
            }
        }

        System.out.println("Bloque <cac:InvoicePeriod> generado usando la fecha " + fechaSuministro);
    }

    private void addChild(Document doc, Element parent, String tag, String value) {
        String prefix = tag.split(":")[0];
        Element element = doc.createElementNS(XmlSupport.namespace(prefix), tag);
        element.setTextContent(value);
        parent.appendChild(element);
    }
}
