package xml.json.transformer.application;

import org.w3c.dom.Document;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.infrastructure.JsonAdapter;
import xml.json.transformer.infrastructure.XmlAdapter;



public class ProcessInvoiceService {
    private final XmlAdapter xmlAdapter;
    private final JsonAdapter jsonAdapter;

    public ProcessInvoiceService(XmlAdapter xmlAdapter, JsonAdapter jsonAdapter){
        this.xmlAdapter = xmlAdapter;
        this.jsonAdapter = jsonAdapter;
    }
    public void process(String inputXmlPath, String outXmlPath, String outputJsonPath) throws Exception {
        Document doc = xmlAdapter.readXml(inputXmlPath);
        xmlAdapter.applyManualTransformations(doc);
        xmlAdapter.writeXml(doc, outXmlPath);

        InvoiceData data = xmlAdapter.buildInvoiceData(doc);
        jsonAdapter.writeJson(data, outputJsonPath);
    }
}
