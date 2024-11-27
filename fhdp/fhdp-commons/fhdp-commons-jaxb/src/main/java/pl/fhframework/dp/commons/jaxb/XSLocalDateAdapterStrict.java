package pl.fhframework.dp.commons.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class XSLocalDateAdapterStrict extends XmlAdapter<String, LocalDate> {

   private static final String pattern = "yyyy-MM-dd";
   private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

   @Override
   public LocalDate unmarshal(String v) {
      return LocalDate.parse(v, formatter);
   }

   @Override
   public String marshal(LocalDate v) {
      if (v == null) {
         return null;
      }
      return formatter.format(v);
   }
}
