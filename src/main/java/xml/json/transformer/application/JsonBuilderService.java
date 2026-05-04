package xml.json.transformer.application;

import xml.json.transformer.application.model.FormInput;
import xml.json.transformer.domain.InvoiceData;
import xml.json.transformer.domain.UserData;

import java.time.LocalDate;

public class JsonBuilderService {

    private final LocalDate issueDate;
    private final FormInputValidator formInputValidator;
    private String fechaSuministro;

    public JsonBuilderService(LocalDate issueDate) {
        this.issueDate = issueDate;
        this.formInputValidator = new FormInputValidator(issueDate);
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

        formInputValidator.validate(in);
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

        UserData.OtrosServicios otherService = new UserData.OtrosServicios();
        otherService.codPrestador = codPrestador;
        otherService.numAutorizacion = numAutorizacion;
        otherService.idMIPRES = nullIfEmpty(in.serv_idMIPRES);
        otherService.fechaSuministroTecnologia = in.fechaSumStr;
        otherService.tipoOS = in.serv_tipoOS;
        otherService.codTecnologiaSalud = in.serv_codTec;
        otherService.nomTecnologiaSalud = in.serv_nomTec;
        otherService.cantidadOS = in.serv_cant;
        otherService.tipoDocumentoIdentificacion = in.serv_tipoDoc;
        otherService.numDocumentoIdentificacion = in.serv_numDoc;
        otherService.vrUnitOS = in.serv_vr;
        otherService.vrServicio = in.serv_vr;
        otherService.conceptoRecaudo = in.serv_concepto;
        otherService.valorPagoModerador = in.serv_valorPagoMod;
        otherService.numFEVPagoModerador = nullIfEmpty(in.serv_numFEV);
        otherService.consecutivo = in.serv_consecutivo;

        user.servicios.otrosServicios.add(otherService);
        invoice.usuarios.add(user);

        return invoice;
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
