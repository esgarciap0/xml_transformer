package xml.json.transformer.application;

import xml.json.transformer.application.model.FormInput;

import java.time.LocalDate;
import java.util.Calendar;

final class FormInputValidator {

    private final LocalDate issueDate;

    FormInputValidator(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    void validate(FormInput input) throws Exception {
        validateBirthDate(input);
        validateSupplyDate(input);
    }

    private void validateBirthDate(FormInput input) throws Exception {
        if (input.user_fechaNac == null) {
            return;
        }

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        if (!input.user_fechaNac.before(today.getTime())) {
            throw new Exception("La fecha de nacimiento debe ser anterior a hoy.");
        }
    }

    private void validateSupplyDate(FormInput input) throws Exception {
        if (input.fechaSum == null) {
            throw new Exception("Debe seleccionar una fecha de suministro.");
        }

        if (issueDate == null) {
            return;
        }

        Calendar supplyDate = Calendar.getInstance();
        supplyDate.setTime(input.fechaSum);
        LocalDate localSupplyDate = LocalDate.of(
                supplyDate.get(Calendar.YEAR),
                supplyDate.get(Calendar.MONTH) + 1,
                supplyDate.get(Calendar.DAY_OF_MONTH)
        );

        if (localSupplyDate.isAfter(issueDate)) {
            throw new Exception(
                    "La fecha de suministro no puede ser posterior a la IssueDate del XML (" + issueDate + ")."
            );
        }
    }
}
