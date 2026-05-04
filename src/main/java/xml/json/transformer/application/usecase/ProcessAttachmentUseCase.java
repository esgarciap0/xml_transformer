package xml.json.transformer.application.usecase;

import org.w3c.dom.Document;
import xml.json.transformer.application.JsonBuilderService;
import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.application.model.ProcessAttachmentResult;
import xml.json.transformer.application.port.out.FormInputPort;
import xml.json.transformer.application.port.out.JsonOutputPort;
import xml.json.transformer.application.port.out.XmlDocumentPort;
import xml.json.transformer.domain.InvoiceData;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class ProcessAttachmentUseCase {

    private final XmlDocumentPort xmlPort;
    private final JsonOutputPort jsonPort;
    private final FormInputPort formInputPort;

    public ProcessAttachmentUseCase(
            XmlDocumentPort xmlPort,
            JsonOutputPort jsonPort,
            FormInputPort formInputPort
    ) {
        this.xmlPort = xmlPort;
        this.jsonPort = jsonPort;
        this.formInputPort = formInputPort;
    }

    public ProcessAttachmentResult process(File inputFile) throws Exception {
        Document originalDoc = xmlPort.readXml(inputFile.getAbsolutePath());
        XPath xp = XPathFactory.newInstance().newXPath();

        String issueDateStr = xp.evaluate(
                "string(//*[local-name()='IssueDate'][1])", originalDoc).trim();
        LocalDate issueDate = LocalDate.parse(issueDateStr);

        String factura = xp.evaluate(
                "string(//*[local-name()='ParentDocumentID'][1])", originalDoc).trim();

        if (factura.isBlank()) {
            throw new IllegalStateException("El XML no contiene ParentDocumentID.");
        }

        String safeFacturaName = sanitizeForPath(factura);
        if (safeFacturaName.isBlank()) {
            safeFacturaName = "salida";
        }

        Path outDir = inputFile.getParentFile().toPath().resolve(safeFacturaName);
        Files.createDirectories(outDir);

        String outXml = outDir.resolve(safeFacturaName + ".xml").toString();
        String outJson = outDir.resolve(safeFacturaName + ".json").toString();

        Document embeddedXml = xmlPort.extractEmbeddedXml(originalDoc);
        if (embeddedXml == null) {
            throw new IllegalStateException("No se encontró la factura embebida dentro del AttachmentDocument.");
        }

        String codPrestador = xp.evaluate(
                "string(//*[local-name()='AdditionalInformation']/*[local-name()='Name' " +
                        "and (normalize-space(text())='CODIGO PRESTADOR' or normalize-space(text())='CODIGO_PRESTADOR')]/" +
                        "following-sibling::*[local-name()='Value'][1])",
                embeddedXml).trim();

        String numAutorizacion = xp.evaluate(
                "string(//*[local-name()='InvoiceAuthorization'][1])", embeddedXml).trim();

        String codTec = xp.evaluate(
                "string(//*[local-name()='StandardItemIdentification']/*[local-name()='ID'][1])",
                embeddedXml).trim();

        String nomTec = xp.evaluate(
                "string(//*[local-name()='Item']/*[local-name()='Description'][1])",
                embeddedXml).trim();

        int vr = 0;
        try {
            String v = xp.evaluate(
                    "string(//*[local-name()='LineExtensionAmount'][1])",
                    embeddedXml).replaceAll("[^0-9.]", "");
            vr = (int) Math.floor(Double.parseDouble(v));
        } catch (Exception ignored) {
        }

        String docIdentServicio = xp.evaluate(
                "string(//*[local-name()='AccountingCustomerParty']//*[local-name()='CompanyID'][1])",
                originalDoc).trim();

        String noteHeader = xp.evaluate(
                "string(//*[local-name()='Note'][1])",
                embeddedXml).trim();

        String nitObligado = xp.evaluate(
                "string(//*[local-name()='CompanyID'][@schemeID='8'][1])",
                originalDoc).trim();

        FormInput input = formInputPort.request(new FormInputPort.FormPrefillData(
                nitObligado,
                factura,
                codPrestador,
                numAutorizacion,
                codTec,
                nomTec,
                vr,
                docIdentServicio,
                noteHeader,
                issueDate
        ));

        if (input == null) {
            return new ProcessAttachmentResult(ProcessAttachmentResult.Status.CANCELLED, outDir, outXml, outJson);
        }

        if (input == FormInput.BACK) {
            return new ProcessAttachmentResult(ProcessAttachmentResult.Status.BACK, outDir, outXml, outJson);
        }

        JsonBuilderService builder = new JsonBuilderService(issueDate);
        InvoiceData data = builder.buildInvoiceData(
                input, nitObligado, factura, codPrestador, numAutorizacion
        );

        Document modified = xmlPort.readXml(inputFile.getAbsolutePath());
        xmlPort.applyManualTransformations(modified, builder.getFechaSuministro());

        jsonPort.writeJson(data, outJson);
        xmlPort.writeXml(modified, outXml);

        return new ProcessAttachmentResult(ProcessAttachmentResult.Status.COMPLETED, outDir, outXml, outJson);
    }

    private String sanitizeForPath(String value) {
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        sanitized = sanitized.replaceAll("\\s+", " ");
        sanitized = sanitized.replaceAll("[. ]+$", "");
        return sanitized;
    }
}
