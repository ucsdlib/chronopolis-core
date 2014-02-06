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
    private static final String COLLECTION_PATH = "rest/collection";
    private static final String TOKENSTORE_PATH = "rest/tokenstore";
    private static final long SSL_PORT = 443;
    private static final long HTTP_DEFAULT_PORT = 80;


    public static String buildACECollectionGet(String fqdn,
                                               long port, 
                                               String acePath, 
                                               String collection,
                                               String group) {
        StringBuilder sb = buildAceUri(fqdn, port, acePath);
        sb.append(COLLECTION_PATH)
        .append(SLASH)
        .append(collection)
        .append(SLASH)
        .append(group);
        return sb.toString();
    }
    
    public static String buildACECollectionPost(String fqdn, 
                                                long port, 
                                                String acePath) {
        StringBuilder sb = buildAceUri(fqdn, port, acePath);
        sb.append(COLLECTION_PATH);
        return sb.toString();
    }

    public static String buildACETokenStorePost(String fqdn, 
                                                long port, 
                                                String acePath,
                                                long id) {
        StringBuilder sb = buildAceUri(fqdn, port, acePath);
        sb.append(TOKENSTORE_PATH);
        sb.append(SLASH);
        sb.append(id);

        return sb.toString();
    }

    private static StringBuilder buildAceUri(String fqdn, long port, String acePath) {
        StringBuilder sb = new StringBuilder();
        if ( port == SSL_PORT ) {
            sb.append("https://");
        } else {
            sb.append("http://");
        }
        sb.append(fqdn);

        if ( port != SSL_PORT && port != HTTP_DEFAULT_PORT ) {
            sb.append(":").append(port);
        } 
        sb.append(SLASH);

        sb.append(acePath);
        sb.append(SLASH);
        return sb;
    }
    
}
