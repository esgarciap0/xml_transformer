package xml.json.transformer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class InvoiceData {

    public String numDocumentoIdObligado;
    public String numFactura;
    public String tipoNota;
    public String numNota;
    public List<UserData> usuarios = new ArrayList<>();
}
