package xml.json.transformer.application.ui;

import java.time.LocalDate;
import java.util.Date;

record FormState(
        String tipoNota,
        String numNota,
        String userTipoDoc,
        String userNumDoc,
        String userTipoUsuario,
        Date userFechaNac,
        String userCodSexo,
        String userCodPaisRes,
        String userCodMunRes,
        String userCodZona,
        String userIncapacidad,
        String userCodPaisOrigen,
        Integer userConsecutivo,
        String servTipoOS,
        String servCodTec,
        String servNomTec,
        Integer servCant,
        String servTipoDoc,
        String servNumDoc,
        String servVr,
        String servConcepto,
        Integer servValorPagoMod,
        String servNumFEV,
        Integer servConsecutivo,
        String servIdMIPRES,
        Date fechaSum,
        Integer horaSum,
        Integer minutoSum,
        LocalDate issueDate
) {
}
