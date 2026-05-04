package xml.json.transformer.application.port.out;

import xml.json.transformer.application.model.FormInput;

import java.time.LocalDate;

public interface FormInputPort {
    FormInput request(FormPrefillData data) throws Exception;

    record FormPrefillData(
            String nitObligado,
            String factura,
            String codPrestador,
            String numAutorizacion,
            String codTecnologia,
            String nomTecnologia,
            int valorUnitario,
            String docIdentServicio,
            String noteHeader,
            LocalDate issueDate
    ) {
    }
}
