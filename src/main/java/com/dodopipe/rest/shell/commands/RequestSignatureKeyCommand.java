package com.dodopipe.rest.shell.commands;

import com.dodopipe.rest.shell.RestShellContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
@Component
public class RequestSignatureKeyCommand
        implements CommandMarker {

    @Autowired
    private RestShellContext context;
    private String accessId;
    private String secretKey;


    @CliCommand(value = "http accessKey set",
                help = "set the secret key and access key.")
    public void setKey(
            @CliOption(
                    key = "accessId",
                    mandatory = false,
                    help = "Set the secret key to sign the http request.")
            String accessId,
            @CliOption(
                    key = "secretKey",
                    mandatory = false,
                    help = "Set the access key to sign the http request.")
            String secretKey
                      ) {

        this.accessId = evalUserSetting(accessId,
                                        "accessId");
        this.secretKey = evalUserSetting(secretKey,
                                         "secretKey");

        if ( !this.isNeededSign() ) {
            throw new IllegalStateException("should user --accessId, --secretKey to set the value");
        }
    }

    @CliCommand(value = "http accessKey reset",
                help = "reset to no-need sign http")
    public void resetKey() {

        this.accessId = null;
        this.secretKey = null;
    }

    @CliCommand(value = "http accessKey list",
                help = "show the http access key")
    public String listKey() {

        StringBuilder sb = new StringBuilder();
        sb.append("accessId = ")
          .append(this.accessId)
          .append("\n")
          .append("secretKey = ")
          .append(this.secretKey)
          .append("\n");

        return sb.toString();

    }

    public boolean isNeededSign() {

        return null != accessId && null != secretKey;
    }

    public String getSecretKey() {

        return secretKey;
    }

    public String getAccessId() {

        return accessId;
    }

    private String readVariable(String name) {

        Object obj =  context.getContextVariable(name);
        if ( obj == null )
            return null;
        return obj.toString();
    }

    private String evalUserSetting(String setting,
                                   String variableName) {

        if (setting == null) {
            return readVariable(variableName);
        }
        if (setting.contains("#{")) {
            return context.evalAsString(setting);
        }
        return setting;
    }

}
