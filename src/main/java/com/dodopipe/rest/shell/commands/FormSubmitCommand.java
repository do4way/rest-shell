package com.dodopipe.rest.shell.commands;

import com.dodopipe.rest.shell.RestShellContext;
import com.dodopipe.rest.shell.utils.DiscoveryCommandHelperUtils;
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
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                             @CliOption(key = "binDataFrom",
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


        try {

            UriComponentsBuilder ucb =
                    DiscoveryCommandHelperUtils.createUriComponentsBuilder(
                            discoveryCommands, configCmds.getBaseUri(),
                            path.getPath());

            this.requestUri = ucb.build().toUri();

            MediaType contentType = null;


            Object jsonObj = stringToJsonObject(data);
            List<Resource> resources = readFromFileOrFolder(binDataFrom);

            MediaType mediaType = MediaType.APPLICATION_FORM_URLENCODED;
            if (resources != null) {
                mediaType = MediaType.MULTIPART_FORM_DATA;
            }

            StringBuilder sb = new StringBuilder();
            outputResponse(submitForm(jsonObj, resources, mediaType, sb), sb);
            return sb.toString();

        }catch (Throwable t ) {

            t.printStackTrace();
        }

        return "To specify the data to submit using --data option or " +
                "the binary data using --binDataFrom";

    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<String> submitForm(Object jsonData,
                                              List<Resource> resources,
                                              MediaType contentType,
                                              StringBuilder sb) {

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
                addToFormData(resource, formData);
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

        outputRequest(requestData, sb);

        return restTemplate.postForEntity(this.requestUri, requestData,
                                          String.class);

    }


    private void addToFormData(Map<Object, Object> map,
                               MultiValueMap<String, Object> formData) {

        if (map == null) {
            return;
        }

        map.forEach((key, value) -> {
            if (key instanceof String) {
                formData.add((String) key, value);
            } else {
                formData.add(key.toString(), value);
            }
        });

    }

    private void addToFormData(Resource resource,
                               MultiValueMap<String, Object> formData) {

        formData.add(resource.getFilename(), resource);
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

        FileFilter ff = new FileFilter() {

            @Override
            public boolean accept(File pathname) {

                if (pathname.isDirectory()) {
                    return true;
                }

                String ext = getExtension(pathname);

                if (ext != null) {
                    return (ext.equals("tiff") || ext.equals("tif") ||
                            ext.equals("gif") || ext.equals("jpeg") ||
                            ext.equals("jpg") || ext.equals("png"));
                }

                return false;

            }
        };

        File file = new File(fileOrFolderPath);

        List<Resource> resources = new ArrayList<>();
        if (file.isDirectory()) {
            File[] filesInFolder = file.listFiles(ff);

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

    private <T> void outputRequest(HttpEntity<T> request, StringBuilder sb) {


        sb.append("> ")
          .append(this.requestUri)
          .append(OsUtils.LINE_SEPARATOR)
          .append("> ")
          .append(request.getHeaders().getContentType())
          .append(OsUtils.LINE_SEPARATOR)
          .append(OsUtils.LINE_SEPARATOR);


    }

    private void outputResponse(ResponseEntity<String> response,
                                StringBuilder buffer) {

        buffer.append("< ")
              .append(response.getStatusCode().value())
              .append(" ")
              .append(response.getStatusCode().name())
              .append(OsUtils.LINE_SEPARATOR);
        for (Map.Entry<String, List<String>> entry : response.getHeaders()
                                                             .entrySet()) {
            buffer.append("< ")
                  .append(entry.getKey())
                  .append(": ");
            boolean first = true;
            for (String s : entry.getValue()) {
                if (!first) {
                    buffer.append(",");
                } else {
                    first = false;
                }
                buffer.append(s);
            }
            buffer.append(OsUtils.LINE_SEPARATOR);
        }
        buffer.append("< ").append(OsUtils.LINE_SEPARATOR);
        if (null != response.getBody()) {
            final org.springframework.data.rest.shell.formatter.Formatter
                    formatter = formatProvider.getFormatter(
                    response.getHeaders().getContentType().getSubtype());
            buffer.append(formatter.format(response.getBody()));
        }
    }

    private String getExtension(File file) {

        String filename = file.getName();
        if ( filename == null ) {
            return null;
        }

        int pos = filename.lastIndexOf(".");
        if (pos < 0) {
            return null;
        }

        return filename.substring(pos + 1).toLowerCase();

    }

}
