package xml.json.transformer.application;

import org.w3c.dom.Document;
import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;

public class JsonBuilderService {

    private final LocalDate issueDate;
    private String fechaSuministro;

    public JsonBuilderService(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getFechaSuministro() {
        return fechaSuministro;
    }

    public InvoiceData buildInvoiceData(
            FormInput in,
            String nitObligado,
            String parentDocID,
            String codPrestador,
            String numAutorizacion
    ) throws Exception {

        validate(in);

        this.fechaSuministro = in.fechaSumStr;

        InvoiceData invoice = new InvoiceData();
        invoice.numDocumentoIdObligado = nitObligado;
        invoice.numFactura = parentDocID;
        invoice.tipoNota = nullIfEmpty(in.tipoNota);
        invoice.numNota = nullIfEmpty(in.numNota);

        UserData user = new UserData();
        user.tipoDocumentoIdentificacion = in.user_tipoDoc;
        user.numDocumentoIdentificacion = in.user_numDoc;
        user.tipoUsuario = in.user_tipoUsuario;
        user.fechaNacimiento = in.user_fechaNacStr;
        user.codSexo = in.user_codSexo;
        user.codPaisResidencia = in.user_codPaisRes;
        user.codMunicipioResidencia = in.user_codMunRes;
        user.codZonaTerritorialResidencia = in.user_codZona;
        user.incapacidad = in.user_incapacidad;
        user.codPaisOrigen = in.user_codPaisOrigen;
        user.consecutivo = in.user_consecutivo;

        UserData.OtrosServicios os = new UserData.OtrosServicios();
        os.codPrestador = codPrestador;
        os.numAutorizacion = numAutorizacion;
        os.idMIPRES = nullIfEmpty(in.serv_idMIPRES);
        os.fechaSuministroTecnologia = in.fechaSumStr;
        os.tipoOS = in.serv_tipoOS;
        os.codTecnologiaSalud = in.serv_codTec;
        os.nomTecnologiaSalud = in.serv_nomTec;
        os.cantidadOS = in.serv_cant;
        os.tipoDocumentoIdentificacion = in.serv_tipoDoc;
        os.numDocumentoIdentificacion = in.serv_numDoc;
        os.vrUnitOS = in.serv_vr;
        os.vrServicio = in.serv_vr;
        os.conceptoRecaudo = in.serv_concepto;
        os.valorPagoModerador = in.serv_valorPagoMod;
        os.numFEVPagoModerador = nullIfEmpty(in.serv_numFEV);
        os.consecutivo = in.serv_consecutivo;

        user.servicios.otrosServicios.add(os);
        invoice.usuarios.add(user);

        return invoice;
    }

    private void validate(FormInput in) throws Exception {

        if (in.user_fechaNac != null) {
            Calendar hoy = Calendar.getInstance();
            hoy.set(Calendar.HOUR_OF_DAY, 0);
            hoy.set(Calendar.MINUTE, 0);
            hoy.set(Calendar.SECOND, 0);
            hoy.set(Calendar.MILLISECOND, 0);

            if (!in.user_fechaNac.before(hoy.getTime())) {
                throw new Exception("La fecha de nacimiento debe ser anterior a hoy.");
            }
        }

        if (in.fechaSum == null) {
            throw new Exception("Debe seleccionar una fecha de suministro.");
        }

        if (issueDate != null) {
            Calendar fs = Calendar.getInstance();
            fs.setTime(in.fechaSum);
            LocalDate fSum = LocalDate.of(
                    fs.get(Calendar.YEAR),
                    fs.get(Calendar.MONTH) + 1,
                    fs.get(Calendar.DAY_OF_MONTH)
            );

            if (fSum.isAfter(issueDate)) {
                throw new Exception("La fecha de suministro no puede ser posterior a la IssueDate del XML (" + issueDate + ").");
            }
        }
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
