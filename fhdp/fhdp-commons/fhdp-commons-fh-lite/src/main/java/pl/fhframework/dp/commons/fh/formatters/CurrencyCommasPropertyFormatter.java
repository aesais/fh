package pl.fhframework.dp.commons.fh.formatters;

import org.springframework.format.Formatter;
import pl.fhframework.format.FhFormatter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Locale;

@FhFormatter("currencyCommasPropertyFormatter")
public class CurrencyCommasPropertyFormatter implements Formatter<BigDecimal> {

    private DecimalFormat formatter = new DecimalFormat("#,###.00");

    @Override
    public BigDecimal parse(String s, Locale locale) throws ParseException {
        return null;
    }

    @Override
    public String print(BigDecimal s, Locale locale) {
        return formatter.format(s);
    }
}
