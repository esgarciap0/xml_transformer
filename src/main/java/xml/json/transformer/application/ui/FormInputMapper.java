package xml.json.transformer.application.ui;

import xml.json.transformer.application.model.FormInput;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;

final class FormInputMapper {

    FormInput map(FormState state) {
        FormInput input = new FormInput();

        input.tipoNota = state.tipoNota();
        input.numNota = state.numNota();
        input.user_tipoDoc = state.userTipoDoc();
        input.user_numDoc = require(state.userNumDoc(), "Numero de documento (usuario)");
        input.user_tipoUsuario = require(state.userTipoUsuario(), "Tipo de usuario");

        if (state.userFechaNac() == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
        }
        input.user_fechaNac = state.userFechaNac();
        input.user_fechaNacStr = formatDate(state.userFechaNac(), "yyyy-MM-dd");

        input.user_codSexo = state.userCodSexo();
        input.user_codPaisRes = require(state.userCodPaisRes(), "Codigo pais de residencia");
        input.user_codMunRes = require(state.userCodMunRes(), "Codigo municipio de residencia");
        input.user_codZona = require(state.userCodZona(), "Codigo zona territorial");
        input.user_incapacidad = state.userIncapacidad();
        input.user_codPaisOrigen = require(state.userCodPaisOrigen(), "Codigo pais de origen");
        input.user_consecutivo = state.userConsecutivo();

        if (state.fechaSum() == null) {
            throw new IllegalArgumentException("Debe seleccionar fecha de suministro.");
        }

        LocalDate fechaSuministro = new Date(state.fechaSum().getTime()).toLocalDate();
        if (state.issueDate() != null && fechaSuministro.isAfter(state.issueDate())) {
            throw new IllegalArgumentException(
                    "La fecha de suministro no puede ser posterior a la IssueDate del XML (" + state.issueDate() + ")."
            );
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(state.fechaSum());
        cal.set(Calendar.HOUR_OF_DAY, state.horaSum());
        cal.set(Calendar.MINUTE, state.minutoSum());
        cal.set(Calendar.SECOND, 0);

        input.fechaSum = cal.getTime();
        input.fechaSumStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(input.fechaSum);

        input.serv_tipoOS = state.servTipoOS();
        input.serv_codTec = state.servCodTec();
        input.serv_nomTec = require(state.servNomTec(), "Nombre tecnologia de salud");
        input.serv_cant = state.servCant();
        input.serv_tipoDoc = state.servTipoDoc();
        input.serv_numDoc = require(state.servNumDoc(), "Numero documento (servicio)");
        input.serv_vr = parseIntSafe(state.servVr(), 0);
        input.serv_concepto = state.servConcepto();
        input.serv_valorPagoMod = state.servValorPagoMod();
        input.serv_numFEV = state.servNumFEV();
        input.serv_consecutivo = state.servConsecutivo();
        input.serv_idMIPRES = state.servIdMIPRES();

        return input;
    }

    private String require(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio.");
        }
        return value.trim();
    }

    private String formatDate(java.util.Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

    private int parseIntSafe(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            String cleaned = value.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? defaultValue : Integer.parseInt(cleaned);
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
