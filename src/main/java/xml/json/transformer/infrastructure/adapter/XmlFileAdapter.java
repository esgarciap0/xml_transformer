package xml.json.transformer.infrastructure.adapter;

import org.w3c.dom.Document;
import xml.json.transformer.application.port.out.XmlDocumentPort;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class XmlFileAdapter implements XmlDocumentPort {

    private final EmbeddedInvoiceTransformer embeddedInvoiceTransformer = new EmbeddedInvoiceTransformer();

    @Override
    public Document readXml(String path) throws Exception {
        return XmlSupport.newSecureDocumentBuilder().parse(new File(path));
    }

    @Override
    public Document extractEmbeddedXml(Document doc) throws Exception {
        return embeddedInvoiceTransformer.extractEmbeddedXml(doc);
    }

    @Override
    public void writeXml(Document doc, String path) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        t.transform(
                new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))
        );

        System.out.println("Archivo XML modificado guardado correctamente: " + path);
    }

    @Override
    public void applyManualTransformations(Document doc, String fechaSuministro) throws Exception {
        embeddedInvoiceTransformer.transform(doc, fechaSuministro);
    }
}
