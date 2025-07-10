<filename>extras/regression-tester/src/main/java/org/sipfoundry/siptester/ItspAccounts.java<fim_prefix>

/*
 * Copyright (C) 2010 Avaya, certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 */
package org.sipfoundry.siptester;

import java.util.HashSet;

import org.apache.commons.digester.Digester;
import org.xml.sax.InputSource;

public class ItspAccounts  {
    HashSet<ItspAccount> itspAccounts = new HashSet<ItspAccount>();
    private static final String BRIDGE_CONFIG = "sipxbridge-config/bridge-configuration";
    private static final String ITSP_CONFIG = "sipxbridge-config/itsp-account";

    public ItspAccounts() {

    }

    public void addItspAccount(ItspAccount itspAccount) {
        this.itspAccounts.add(itspAccount);
    }


    /**
     * Add the digester rules.
     *
     * @param digester
     */
    private static void addRules(Digester digester) {

        digester.addObjectCreate("sipxbridge-config", ItspAccounts.class);

        /*
         * ITSP configuration support parameters.
         */
        digester.addObjectCreate(ITSP_CONFIG, ItspAccount.class);
        digester.addSetNext(ITSP_CONFIG, "addItspAccount");


        digester.addCallMethod(String.format("%s/%s", ITSP_CONFIG,"itsp-proxy-address"), "setItspProxyAddress",0);
        digester.addCallMethod(String.format("%s/%s", ITSP_CONFIG,"itsp-proxy-port"), "setItspProxyPort",0);
        digester.addCallMethod(String.format("%s/%s", ITSP_CONFIG,"itsp-proxy-domain"), "setItspProxyDomain",0);


        /*
         * Authentication user name
         */
        digester
                .addCallMethod(String.format("%s/%s", ITSP_CONFIG, "user-name"), "setUserName", 0);

        /*
         * Authentication password.
         */
        digester.addCallMethod(String.format("%s/%s", ITSP_CONFIG, "password"), "setPassword", 0);

    }


    public  static ItspAccounts createItspAccounts(String url) throws Exception {
        System.out.println("create ITSP accounts " + url);
        Digester digester = new Digester();
        <fim_suffix>
        digester.parse(new InputSource(url));
        return (ItspAccounts) digester.getRoot();
    }

    public ItspAccount getItspAccount( int emulatedPort) {
        for (ItspAccount itspAccount : this.itspAccounts ) {
            if (itspAccount.getItspProxyAddress().equals(SipTester.getTesterConfig().getTesterIpAddress()) &&
                    emulatedPort == itspAccount.getItspProxyPort()) {
                return itspAccount;
            }
        }
        return null;
    }


}
<fim_middle>
