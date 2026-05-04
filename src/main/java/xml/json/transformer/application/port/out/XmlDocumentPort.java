package xml.json.transformer.application.port.out;

import org.w3c.dom.Document;

public interface XmlDocumentPort {
    Document readXml(String path) throws Exception;
    Document extractEmbeddedXml(Document doc) throws Exception;
    void writeXml(Document doc, String path) throws Exception;
    void applyManualTransformations(Document doc, String fechaSuministro) throws Exception;
}
