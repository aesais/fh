package pl.fhframework.dp.commons.fh.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import pl.fhframework.core.services.FhService;
import pl.fhframework.dp.commons.fh.helper.AESCypher;
import pl.fhframework.dp.commons.fh.utils.FhUtils;
import pl.fhframework.event.EventRegistry;

import static pl.fhframework.SessionManager.getUserSession;

@Getter
@FhService
@RequiredArgsConstructor
public class PinupService {
   @Value("${fhdp.pinup.url:none}")
   private String pinupUrl;
   @Value("${fhdp.pinup.password:none}")
   private String pinupCypherPassword;

   private final EventRegistry eventRegistry;

   public void openPinup(Long docId) throws Exception {
      String id = AESCypher.encrypt(pinupCypherPassword, docId.toString());
      String url = pinupUrl
                   + "?id=" + id
                   + "&lng=" + getUserSession().getLanguage().toLanguageTag();

      String theme = FhUtils.getCookieByKey("theme");
      if(null != theme) {
         url += "&theme=" + theme;
      }
      eventRegistry.fireCustomActionEvent("openPinup", url);
   }

}
