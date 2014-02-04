/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

/**
 * TODO: Organize alphabetically
 *
 * @author shake
 */
public enum MessageConstant {
    // Headers
    CORRELATION_ID("correlation-id"),
    DATE("date"),
    ORIGIN("origin"),
    RETURN_KEY("return-key"),

    // Message vals
    DEPOSITOR("depositor"),
    COLLECTION("collection"),
    TOKEN_STORE("token-store"),
    AUDIT_PERIOD("audit-period"),
    DIGEST("digest"),
    DIGEST_TYPE("digest-type"),
    PACKAGE_NAME("package-name"),
    FILENAME("filename"),
    LOCATION("location"),
    PROTOCOL("protocol"),
    SIZE("size"),
    STATUS("status"),
    FAILED_ITEMS("failed-items"),
    FIXITY_ALGORITHM("fixity-algorithm"),
    MESSAGE_ATT("message-att"),

    // Supported Algorithms (should we move to their own enum?)
    // can retrieve message digest -> enum.digestName() -> SHA-256
    // and retrieve manifest name  -> enum.manifestName() -> sha256
    SHA_256("sha256"),

    // Misc 
    STATUS_SUCCESS("success"),
    STATUS_FAIL("failed"),
    ;
    
    private final String text;

    MessageConstant(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
