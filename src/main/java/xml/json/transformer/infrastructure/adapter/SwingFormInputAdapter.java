package xml.json.transformer.infrastructure.adapter;

import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.application.port.out.FormInputPort;
import xml.json.transformer.application.ui.JsonFormUI;

public class SwingFormInputAdapter implements FormInputPort {

    private final JsonFormUI jsonFormUI;

    public SwingFormInputAdapter() {
        this.jsonFormUI = new JsonFormUI();
    }

    @Override
    public FormInput request(FormPrefillData data) {
        return jsonFormUI.show(
                data.nitObligado(),
                data.factura(),
                data.codPrestador(),
                data.numAutorizacion(),
                data.codTecnologia(),
                data.nomTecnologia(),
                data.valorUnitario(),
                data.docIdentServicio(),
                data.noteHeader(),
                data.issueDate()
        );
    }
}
