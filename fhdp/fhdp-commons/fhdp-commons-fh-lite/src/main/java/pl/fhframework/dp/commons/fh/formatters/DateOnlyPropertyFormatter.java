package pl.fhframework.dp.commons.fh.formatters;

import org.springframework.format.Formatter;
import pl.fhframework.format.FhFormatter;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@FhFormatter("dateOnlyPropertyFormatter")
public class DateOnlyPropertyFormatter implements Formatter<LocalDateTime> {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDateTime parse(String s, Locale locale) throws ParseException {
        return null;
    }

    @Override
    public String print(LocalDateTime s, Locale locale) {
        return formatter.format(s);
    }
}
