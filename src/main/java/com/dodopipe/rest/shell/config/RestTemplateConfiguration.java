package com.dodopipe.rest.shell.config;

import com.dodopipe.rest.shell.commands.RequestSignatureKeyCommand;
import com.dodopipe.rest.shell.template.interceptors.RequestSignatureInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
@Configuration
public class RestTemplateConfiguration {

    private static final String PROXYSERVER_PROPERTY_NAME = "http.proxyServer";
    private static final String PROXYPORT_PROPERTY_NAME = "http.proxyPort";
    @Autowired
    private RequestSignatureInterceptor interceptor;

    @Bean
    public RestTemplate restTemplate() {

        System.out.println("create rest template");

        String proxyServer = System.getProperty(PROXYSERVER_PROPERTY_NAME);
        RestTemplate template;
        if ( proxyServer != null ) {

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            int port = 8081;
            try {
                port = Integer.parseInt(System.getProperty(PROXYPORT_PROPERTY_NAME));
            } catch (Throwable t ) {

            }
            System.out.println("proxy server:" + proxyServer);
            System.out.println("proxy port:" + port);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServer, port));
            requestFactory.setProxy(proxy);
            template = new RestTemplate(requestFactory);
        }else {
            template = new RestTemplate();
        }

        template.setInterceptors(Collections.<ClientHttpRequestInterceptor>singletonList(interceptor));

        return template;
    }

}
