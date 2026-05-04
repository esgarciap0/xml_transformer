package xml.json.transformer.application.ui;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

final class FormPreviewBuilder {

    String build(String noteHeader, LocalDate issueDate, Date supplyDate, Integer hour, Integer minute) {
        StringBuilder sb = new StringBuilder();

        if (noteHeader != null && !noteHeader.isBlank()) {
            sb.append(noteHeader.trim());
        } else {
            sb.append("(Sin nota)");
        }
        sb.append("\n\n");
        sb.append("Fecha de factura: ").append(issueDate != null ? issueDate : "(no disponible)");

        if (supplyDate == null) {
            sb.append("\nPeriodo de facturacion: seleccione la fecha de suministro para previsualizar.");
            return sb.toString();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(supplyDate);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        Calendar calStart = (Calendar) cal.clone();
        calStart.add(Calendar.DATE, -1);

        String startDate = new SimpleDateFormat("yyyy-MM-dd").format(calStart.getTime());
        String endDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        sb.append("\nPeriodo de facturacion: ")
                .append(startDate)
                .append(" a ")
                .append(endDate);
        return sb.toString();
    }
}
