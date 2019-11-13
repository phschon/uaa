package org.cloudfoundry.identity.uaa.cypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "encryption")
@Data
public class EncryptionProperties {

    private String activeKeyLabel;
    private List<EncryptionKeyService.EncryptionKey> encryptionKeys;

}
