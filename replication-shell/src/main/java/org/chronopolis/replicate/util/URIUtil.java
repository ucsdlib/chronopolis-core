/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.util;

/**
 *
 * @author shake
 */
public class URIUtil {
    private static final String SLASH = "/";
    private static final long SSL_PORT = 443;
    private static final long ALT_SSL_PORT = 8443;
    private static final long HTTP_DEFAULT_PORT = 80;


    public static StringBuilder buildAceUri(String fqdn, long port, String acePath) {
        StringBuilder sb = new StringBuilder();
        if (port == SSL_PORT || port == ALT_SSL_PORT) {
            sb.append("https://");
        } else {
            sb.append("http://");
        }
        sb.append(fqdn);

        if (port != SSL_PORT && port != HTTP_DEFAULT_PORT) {
            sb.append(":").append(port);
        } 
        sb.append(SLASH);

        sb.append(acePath);
        sb.append(SLASH);
        return sb;
    }
    
}
