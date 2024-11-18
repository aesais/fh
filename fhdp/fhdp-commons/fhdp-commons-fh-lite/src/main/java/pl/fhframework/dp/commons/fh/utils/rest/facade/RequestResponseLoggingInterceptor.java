package pl.fhframework.dp.commons.fh.utils.rest.facade;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    @NonNull
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
          throws IOException {
        if (log.isDebugEnabled()){
            logRequest(request, body);
            ClientHttpResponse response = execution.execute(request, body);
            return logResponse(response);
        } else {
            return execution.execute(request, body);
        }
    }

    private void logRequest(HttpRequest request, byte[] body) {
        log.debug("===========================request begin================================================");
        log.debug("URI         : {}", request.getURI());
        log.debug("Method      : {}", request.getMethod());
        log.debug("Headers     : {}", request.getHeaders());
        log.debug("Request body: {}", new String(body, StandardCharsets.UTF_8));
        log.debug("==========================request end================================================");

    }

    private ClientHttpResponse logResponse(ClientHttpResponse response) throws IOException {
        CustomBufferingClientHttpResponseWrapper bufferedResponse = new CustomBufferingClientHttpResponseWrapper(response);
        log.debug("============================response begin==========================================");
        log.debug("Status code  : {}", response.getStatusCode());
        log.debug("Status text  : {}", response.getStatusText());
        log.debug("Headers      : {}", response.getHeaders());
        log.debug("Response body: {}", StreamUtils.copyToString(bufferedResponse.getBody(), Charset.defaultCharset()));
        log.debug("=======================response end=================================================");
        return bufferedResponse;
    }
}
