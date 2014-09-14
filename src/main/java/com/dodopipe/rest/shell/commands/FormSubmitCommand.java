package com.dodopipe.rest.shell.commands;

import com.dodopipe.rest.shell.RestShellContext;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.rest.shell.commands.ConfigurationCommands;
import org.springframework.data.rest.shell.commands.DiscoveryCommands;
import org.springframework.data.rest.shell.commands.PathOrRel;
import org.springframework.data.rest.shell.formatter.FormatProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
@Component
public class FormSubmitCommand
        implements CommandMarker,
                   ApplicationEventPublisherAware,
                   InitializingBean {

    private static final Logger LOG =
            LoggerFactory.getLogger(FormSubmitCommand.class);
    private static final String LOCATION_HEADER = "Location";

    private ApplicationEventPublisher ctx;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private DiscoveryCommands discoveryCommands;
    @Autowired
    private RestShellContext context;

    @Autowired
    private ConfigurationCommands configCmds;

    @Autowired(required = false)
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FormatProvider formatProvider;

    private URI requestUri;
    private Object lastResult;

    @Override
    public void setApplicationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {

        this.ctx = applicationEventPublisher;
    }

    @Override
    public void afterPropertiesSet()
            throws
            Exception {

        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
                         true);
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT,
                         true);

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

            @Override
            public void handleError(ClientHttpResponse response)
                    throws
                    IOException {

            }
        });
    }

    @CliCommand(value = "submit-form",
                help = "Issue application/x-www-form-urlencoded data post to create a new resource using json format data.")
    public String submitForm(@CliOption(key = {"",
                                               "rel"},
                                        mandatory = false,
                                        help = "The path to the resource collections.",
                                        unspecifiedDefaultValue = "")
                             PathOrRel path,
                             @CliOption(key = "follow",
                                        mandatory = false,
                                        help = "If a Location header is returned, immediately follow it.",
                                        unspecifiedDefaultValue = "false")
                             final String follow,
                             @CliOption(key = "data",
                                        mandatory = false,
                                        help = "The form data to use as the resource.")
                             String data,
                             @CliOption(key = "attachmentName",
                                        mandatory = false,
                                        help = "")
                             String binDataName,
                             @CliOption(key = "attachment",
                                        mandatory = false,
                                        help = "read attachment data from a file to submit as multipart/form-data")
                             String binDataFrom,
                             @CliOption(key = "output",
                                        mandatory = false,
                                        help = "The path to dump the output to.")
                             String outputPath)
            throws
            IOException,
            JsonParseException,
            JsonMappingException {

        if (binDataName != null && binDataFrom == null) {
            return "To use --binDataFrom to give a file name or folder path";
        }

        try {
            UriComponentsBuilder ucb =
                    createUriComponentsBuilder(path.getPath());

            this.requestUri = ucb.build().toUri();

            MediaType contentType = null;


            Object jsonObj = stringToJsonObject(data);
            List<Resource> resources = readFromFileOrFolder(binDataFrom);

            if (resources != null) {

                return submitForm(jsonObj, binDataName, resources,
                                  MediaType.MULTIPART_FORM_DATA);
            }

            if (data != null) {

                return submitForm(jsonObj, null, null,
                                  MediaType.APPLICATION_FORM_URLENCODED);
            }
        }catch (Throwable t ) {

            t.printStackTrace();
        }

        return "To specify the data to submit using --data option or " +
                "the binary data using --binDataName and --binDataFrom";

    }

    @SuppressWarnings("unchecked")
    private String submitForm(Object jsonData, String binDataName,
                              List<Resource> resources,
                              MediaType contentType) {

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        if (jsonData != null) {
            if (jsonData instanceof List) {
                for (Object map : (List) jsonData) {
                    addToFormData((Map<Object, Object>) map, formData);
                }
            } else if (jsonData instanceof Map) {
                addToFormData((Map<Object, Object>) jsonData, formData);

            }
        }

        if (resources != null) {
            for (Resource resource : resources) {
                addToFormData(binDataName, resource, formData);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);

        List<MediaType> accept = Arrays.asList(MediaType.APPLICATION_JSON);
        Object userSpecified = context.getContextVariable("Accept");
        if ( userSpecified != null ) {
            accept.add(MediaType.valueOf(userSpecified.toString()));
        }

        headers.setAccept(accept);

        HttpEntity<MultiValueMap<String,Object>> requestData = new HttpEntity<>(formData, headers);


        ResponseEntity<String> response = restTemplate.postForEntity(this.requestUri, formData, String.class);

        return response.getBody();
    }


    private void addToFormData(Map<Object, Object> map,
                               MultiValueMap<String, Object> formData) {

        if (map == null) {
            return;
        }

        map.forEach((key,value) -> System.out.println(key));

        map.forEach((key, value) -> {
            if (key instanceof String) {
                formData.add((String) key, value);
            } else {
                formData.add(key.toString(), value);
            }
        });

    }

    private void addToFormData(String binDataName, Resource resource,
                               MultiValueMap<String, Object> formData) {

        formData.add(binDataName, resource);
    }


    private Object stringToJsonObject(String data) throws IOException {

        if (data == null) {
            return null;
        }

        Object jsonObj = null;
        if (data.contains("#{")) {
            data = context.evalAsString(data);
        }

        if (data.startsWith("[") || data.startsWith("{")) {

            jsonObj = jsonToObject(data.replaceAll("\\\\", "")
                                       .replaceAll("'", "\""));
        } else {

            throw new IllegalArgumentException(
                    "data are not valid json string, " +
                            "that should start with '{' or '['");
        }
        return jsonObj;

    }

    private Object jsonToObject(String json) throws IOException {

        if (json == null) {
            return null;
        }

        Class<?> targetType;
        targetType = Map.class;
        if (json.startsWith("[")) {
            targetType = List.class;
        }
        return mapper.readValue(json, targetType);
    }

    private List<Resource> readFromFileOrFolder(String fileOrFolderPath)
            throws MalformedURLException {

        if (fileOrFolderPath == null) {
            return null;
        }

        fileOrFolderPath = context.evalAsString(fileOrFolderPath);
        if (null == fileOrFolderPath) {
            return null;
        }
        File file = new File(fileOrFolderPath);

        List<Resource> resources = new ArrayList<>();
        if (file.isDirectory()) {
            File[] filesInFolder = file.listFiles();

            if (filesInFolder != null) {
                for (int i = 0, c = filesInFolder.length; i < c; i++) {
                    resources.add(new UrlResource(
                            filesInFolder[i].toURI().toURL()));
                }
            }

        } else {
            resources.add(new UrlResource(file.toURI().toURL()));
        }

        return resources;
    }


    private String objToUrlEncodedForm(Object obj) throws IOException {

        if (obj instanceof Map) {
            return mapToURLEncodedForm((Map) obj);
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Iterator ite = ((List) obj).iterator(); ite.hasNext(); ) {
                Map m = (Map) ite.next();
                sb.append(mapToURLEncodedForm(m));
                if (ite.hasNext()) {
                    sb.append("&");
                }
            }
            return sb.toString();

        }
        throw new IllegalArgumentException("Not a valid json object");
    }

    private String mapToURLEncodedForm(Map map) {

        StringBuilder sb = new StringBuilder();
        for (Iterator keySet = map.keySet().iterator(); keySet.hasNext(); ) {

            Object key = keySet.next();
            sb.append(key.toString())
              .append("=")
              .append(encode(map.get(key)
                                .toString()));

            if (keySet.hasNext()) {

                sb.append("&");
            }
        }
        return sb.toString();
    }


    private UriComponentsBuilder createUriComponentsBuilder(String path) {

        UriComponentsBuilder ucb;

        if (discoveryCommands.getResources()
                             .containsKey(path)) {
            ucb =
                    UriComponentsBuilder.fromUriString(
                            discoveryCommands.getResources()
                                             .get(path));
        } else {
            if (path.startsWith("http")) {
                ucb = UriComponentsBuilder.fromUriString(path);
            } else {
                ucb = UriComponentsBuilder.fromUri(configCmds.getBaseUri())
                                          .pathSegment(path);
            }

        }

        return ucb;

    }

    private static String encode(String s) {

        try {
            return URLEncoder.encode(s,
                                     "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

}
