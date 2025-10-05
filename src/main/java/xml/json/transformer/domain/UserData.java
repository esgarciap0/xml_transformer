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
    public Services servicios = new Services();

    public static class Services {
        public List<OtrosServicios> otrosServicios = new ArrayList<>();
    }

    public static class OtrosServicios {
        public String codPrestador;
        public String numAutorizacion;
        public String codTecnologiaSalud;
        public String nomTecnologiaSalud;
        public Integer cantidadOS;
        public Integer vrUnitOS;
        public Integer vrServicio;
    }
}
