package com.dodopipe.rest.shell.converters;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
public class ByteArrayHttpMessageConverter implements
                                             HttpMessageConverter<byte[]> {

    private final List<MediaType> supportedMediaType = Arrays.asList(
            MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG,
            MediaType.APPLICATION_OCTET_STREAM);

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {

        return clazz.isAssignableFrom(byte[].class);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {

        return false;

    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {

        return supportedMediaType;
    }

    @Override
    public byte[] read(Class<? extends byte[]> clazz,
                            HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        System.out.println("read ========================");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(inputMessage.getBody(), baos);
        return baos.toByteArray();

    }

    @Override
    public void write(byte[] bytes, MediaType contentType,
                      HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        throw new UnsupportedOperationException("Not implemented yet");
    }
}
