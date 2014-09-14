package com.dodopipe.rest.shell.utils;

import org.springframework.data.rest.shell.commands.DiscoveryCommands;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
public class DiscoveryCommandHelperUtils {

    public static UriComponentsBuilder createUriComponentsBuilder(
            DiscoveryCommands discovery, URI baseUri, String path) {

        UriComponentsBuilder ucb;

        if (discovery.getResources()
                     .containsKey(path)) {
            ucb =
                    UriComponentsBuilder.fromUriString(discovery.getResources()
                                                                .get(path));
        } else {
            if (path.startsWith("http")) {
                ucb = UriComponentsBuilder.fromUriString(path);
            } else {
                ucb = UriComponentsBuilder.fromUri(baseUri)
                                          .pathSegment(path);
            }

        }

        return ucb;
    }

}
