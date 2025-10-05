package xml.json.transformer.domain;

import java.util.ArrayList;
import java.util.List;

public class UserData {
    public String tipoDocumentoIdentificacion;
    public String numDocumentoIdentificacion;
    public String tipoUsuario;
    public String fechaNacimiento;
    public String codSexo;
    public String codPaisResidencia;
    public String codMunicipioResidencia;
    public String codZonaTerritorialResidencia;
    public String incapacidad;
    public String codPaisOrigen;
    public Integer consecutivo;
    public Servicios servicios = new Servicios();

    public static class Servicios {
        public List<OtrosServicios> otrosServicios = new ArrayList<>();
    }

    public static class OtrosServicios {
        public String codPrestador;
        public String numAutorizacion;
        public String idMIPRES;
        public String fechaSuministroTecnologia;
        public String tipoOS;
        public String codTecnologiaSalud;
        public String nomTecnologiaSalud;
        public Integer cantidadOS;
        public String tipoDocumentoIdentificacion;
        public String numDocumentoIdentificacion;
        public Integer vrUnitOS;
        public Integer vrServicio;
        public String conceptoRecaudo;
        public Integer valorPagoModerador;
        public String numFEVPagoModerador;
        public Integer consecutivo;
    }
}
