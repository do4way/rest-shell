package com.dodopipe.rest.shell.template.interceptors;

import com.dodopipe.rest.shell.commands.RequestSignatureKeyCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.codec.binary.Base64;


/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
@Component
public class RequestSignatureInterceptor
        implements ClientHttpRequestInterceptor {

    private static Logger
            logger = LoggerFactory.getLogger(RequestSignatureInterceptor.class);


    @Autowired
    private RequestSignatureKeyCommand signCommand;

    private final static String HEADER_AUTHORIZATION = "Authorization";
    private final static String HEADER_CONTENT_MD5 = "Content-MD5";
    private final static String SIGNATURE_PREFIX = "DOP ";
    private final static String ALGORITHM = "HmacSHA1";
    private final static String CHARSET_NAME = "UTF-8";

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            Locale.US);


    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution)
            throws
            IOException,IllegalStateException {

        if (!signCommand.isNeededSign()) {
            return execution.execute(request,
                                     body);
        }
        String secretKey = signCommand.getSecretKey();
        String accessKeyId = signCommand.getAccessId();
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders()
               .setDate(new Date().getTime());
        String headersToSign = extractHeadersToSign(request);

        String signature = null;
        try {
            signature = hmacSign(headersToSign,
                                 secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Headers to signature (Header Interceptor) :\n" +
                                 "{}\n" +
                                 "signature:{}",
                         headersToSign,
                         signature);
        }

        wrapper.getHeaders()
               .set(HEADER_AUTHORIZATION,
                    SIGNATURE_PREFIX + accessKeyId + ":" + signature);

        return execution.execute(wrapper,
                                 body);
    }

    private String hmacSign(String headersToSign,
                            String secretKey)
            throws
            InvalidKeyException {

        SecretKey key = new SecretKeySpec(base64Decode(secretKey),
                                          ALGORITHM);
        byte[] rowHmac;
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            rowHmac = mac.doFinal(headersToSign.getBytes(CHARSET_NAME));
        }catch (Throwable e ) {
            throw new IllegalStateException(e);
        }

        return base64Encode(rowHmac);
    }

    private String extractHeadersToSign(HttpRequest request) {

        List<String> parts = new ArrayList<>();
        parts.add(null2Empty(request.getMethod()
                                    .name()
                                    .toUpperCase()));
        parts.add(null2Empty(getContentMd5(request)));
        parts.add(getContentType(request));
        long date = request.getHeaders()
                           .getDate();
        parts.add(DATE_FORMAT.format(new Date(date)));
        parts.add(null2Empty(getServletPath(request)));

        return String.join("\n",
                           parts);

    }

    private String null2Empty(String str) {

        return str == null ? "" : str;
    }

    private String getServletPath(HttpRequest request) {

        return request.getURI()
                      .getPath();
    }

    private String getContentMd5(HttpRequest request) {

        List<String> list = request.getHeaders()
                                   .get(HEADER_CONTENT_MD5);
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",",
                           list);
    }

    private String getContentType(HttpRequest request) {

        MediaType mediaType = request.getHeaders()
                                     .getContentType();
        if (mediaType == null) {
            return "";
        }
        String type = mediaType.getType();
        assert (type != null);
        String subType = mediaType.getSubtype();
        if (subType == null || subType.length() == 0) {
            return type;
        }
        return String.join("/",
                           new String[]{type,
                                        subType});
    }

    private static String base64Encode(byte[] data) {

        if ( data == null )
            return null;
        return Base64.encodeBase64String(data);

    }

    private static byte[] base64Decode(String secretKey) {

        return Base64.decodeBase64(secretKey);
    }

}
