package xml.json.transformer.infrastructure;

import org.w3c.dom.Document;
import xml.json.transformer.domain.InvoiceData;

public interface XmlAdapter {
    Document readXml(String path) throws Exception;
    void writeXml(Document doc, String path) throws Exception;
    void applyManualTransformations(Document doc) throws Exception;

    InvoiceData buildInvoiceData(Document doc);
}
