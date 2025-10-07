package xml.json.transformer.application;

public final class Defaults {
    private Defaults() {}

    // Invoice-level defaults
    public static final String TIPO_USUARIO = "10";
    public static final String COD_PAIS_RESIDENCIA = "170";
    public static final String COD_MPIO_RESIDENCIA = "23001";
    public static final String COD_PAIS_ORIGEN = "170";
    public static final String COD_ZONA_TERRITORIAL = "02";
    public static final Integer CONSECUTIVO = 1;

    public static final String TIPO_OS = "02";
    public static final String CONCEPTO_RECAUDO = "03";
    public static final Integer VALOR_PAGO_MODERADOR = 0;

    // Select options
    public static final String[] SEXO = {"M", "F"};
    public static final String[] INCAPACIDAD = {"NO", "SI"};
    public static final String[] TIPO_DOC = {"CC", "CI", "PS", "DNI", "CE", "TI", "PA"};
    public static final Integer[] CANTIDAD = {1,2,3,4,5,6,7,8,9,10};
}
