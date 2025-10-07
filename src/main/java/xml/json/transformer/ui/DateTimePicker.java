package xml.json.transformer.ui;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.Date;

/** Simple date+time picker using JDateChooser + two spinners for HH:mm */
public class DateTimePicker extends JPanel {
    private final JDateChooser date = new JDateChooser();
    private final JSpinner hour   = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
    private final JSpinner minute = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    public DateTimePicker() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        date.setDateFormatString("yyyy-MM-dd");
        add(date);
        add(new JLabel("Hora:"));
        add(hour);
        add(new JLabel(":"));
        add(minute);
        setPreferredSize(new Dimension(360, 28));
    }

    public void set(java.time.LocalDateTime ldt) {
        if (ldt == null) ldt = java.time.LocalDateTime.now();
        ZoneId z = ZoneId.systemDefault();
        date.setDate(Date.from(ldt.atZone(z).toInstant()));
        hour.setValue(ldt.getHour());
        minute.setValue(ldt.getMinute());
    }

    public LocalDateTime get() {
        Date d = date.getDate();
        if (d == null) return null;
        ZoneId z = ZoneId.systemDefault();
        LocalDate base = d.toInstant().atZone(z).toLocalDate();
        return LocalDateTime.of(base, java.time.LocalTime.of((int) hour.getValue(), (int) minute.getValue()));
    }
}
