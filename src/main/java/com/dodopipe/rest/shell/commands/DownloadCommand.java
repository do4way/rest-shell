package com.dodopipe.rest.shell.commands;

import com.dodopipe.rest.shell.RestShellContext;
import com.dodopipe.rest.shell.converters.ByteArrayHttpMessageConverter;
import com.dodopipe.rest.shell.utils.DiscoveryCommandHelperUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.shell.commands.ConfigurationCommands;
import org.springframework.data.rest.shell.commands.DiscoveryCommands;
import org.springframework.data.rest.shell.commands.PathOrRel;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
@Component
public class DownloadCommand implements CommandMarker, InitializingBean {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryCommands discoveryCommands;

    @Autowired
    private ConfigurationCommands configCommands;

    @Autowired
    private RestShellContext context;

    private URI requestUri;

    @Override
    public void afterPropertiesSet() throws Exception {

        // after view the spring sources, we found there are a existed ByteArrayHttpMessageConverter.
        //List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        //converters.add(0, new ByteArrayHttpMessageConverter());
    }

    @CliCommand(value = "download",
                help = "Download binary data.")
    public String download(@CliOption(key = {"", "rel"},
                                      mandatory = false,
                                      help = "The path to th resource to download",
                                      unspecifiedDefaultValue = "")
                           PathOrRel path,
                           @CliOption(key = "to",
                                      mandatory = true,
                                      help = "The file path, write the file.")
                           String to) throws IOException {

        try {
            UriComponentsBuilder ucb =
                    DiscoveryCommandHelperUtils.createUriComponentsBuilder(
                            discoveryCommands, configCommands.getBaseUri(),
                            path.getPath());


            requestUri = ucb.build().toUri();

            byte[] response = restTemplate.getForObject(requestUri,
                                                             byte[].class);

            String writeTo = context.evalAsString(to);

            try (FileOutputStream fos = new FileOutputStream(new File(
                    writeTo))) {

                fos.write(response);

            }
        }catch(Throwable e ) {
            e.printStackTrace();
            return "Failed";
        }
        return "Ok";

    }


}
