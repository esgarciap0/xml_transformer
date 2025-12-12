package xml.json.transformer.application.model;

import java.util.Date;

public class FormInput {

    public String tipoNota;
    public String numNota;

    public String user_tipoDoc;
    public String user_numDoc;
    public String user_tipoUsuario;
    public Date   user_fechaNac;
    public String user_fechaNacStr;
    public String user_codSexo;
    public String user_codPaisRes;
    public String user_codMunRes;
    public String user_codZona;
    public String user_incapacidad;
    public String user_codPaisOrigen;
    public Integer user_consecutivo;

    public String serv_tipoOS;
    public String serv_codTec;
    public String serv_nomTec;
    public Integer serv_cant;
    public String serv_tipoDoc;
    public String serv_numDoc;
    public Integer serv_vr;
    public String serv_concepto;
    public Integer serv_valorPagoMod;
    public String serv_numFEV;
    public Integer serv_consecutivo;
    public String serv_idMIPRES;

    public Date fechaSum;
    public String fechaSumStr;
    public static final FormInput BACK = new FormInput();

}
