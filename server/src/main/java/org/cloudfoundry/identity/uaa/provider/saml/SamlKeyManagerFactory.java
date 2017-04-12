/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2016] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.provider.saml;

import org.cloudfoundry.identity.uaa.saml.SamlKey;
import org.cloudfoundry.identity.uaa.util.KeyWithCert;
import org.cloudfoundry.identity.uaa.zone.SamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public final class SamlKeyManagerFactory {

    protected final static Logger logger = LoggerFactory.getLogger(SamlKeyManagerFactory.class);

    private SamlKeyManagerFactory() {}

    public static KeyManager getKeyManager(SamlConfig config) {
        return getKeyManager(config.getKeys(), config.getActiveKeyId());
    }

    private static KeyManager getKeyManager(Map<String, SamlKey> keys, String activeKeyId) {
        SamlKey activeKey = keys.get(activeKeyId);

        if(activeKey == null || !StringUtils.hasText(activeKey.getKey())) {
            return null;
        }


        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            Map<String, String> aliasPasswordMap = new HashMap<>();
            for (Map.Entry<String, SamlKey> entry : keys.entrySet()) {


                String password = ofNullable(entry.getValue().getPassphrase()).orElse("");
                KeyWithCert keyWithCert = new KeyWithCert(entry.getValue().getKey(), password, entry.getValue().getCertificate());
                X509Certificate cert = keyWithCert.getCert();
                KeyPair pkey = keyWithCert.getPkey();


                String alias = entry.getKey();
                keystore.setCertificateEntry(alias, cert);
                keystore.setKeyEntry(alias, pkey.getPrivate(), password.toCharArray(), new Certificate[]{cert});
                aliasPasswordMap.put(alias, password);
            }


            JKSKeyManager keyManager = new JKSKeyManager(keystore, aliasPasswordMap, activeKeyId);

            if (null == keyManager) {
                throw new IllegalArgumentException(
                        "Could not load service provider certificate. Check serviceProviderKey and certificate parameters");
            }

            logger.info("Loaded service provider certificate " + keyManager.getDefaultCredentialName());

            return keyManager;
        } catch (Throwable t) {
            logger.error("Could not load certificate", t);
            throw new IllegalArgumentException(
                    "Could not load service provider certificate. Check serviceProviderKey and certificate parameters",
                    t);
        }
    }
}
