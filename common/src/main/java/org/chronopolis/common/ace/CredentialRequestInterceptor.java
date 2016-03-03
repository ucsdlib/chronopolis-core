package org.chronopolis.common.ace;

import javax.annotation.Nonnull;

/**
 * Created by shake on 5/19/14.
 */
@Deprecated
public class CredentialRequestInterceptor { // implements RequestInterceptor {
    private final String user;
    private final String password;

    public CredentialRequestInterceptor(@Nonnull String user, @Nonnull String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Intercept a http request sent by retrofit and add in basic authorization
     * from the user and password fields of the class
     *
     * @param requestFacade the intercepted request
    @Override
    public void intercept(final RequestFacade requestFacade) {
        String credentials = user + ":" + password;
        String basicAuth = "Basic " + Base64.encodeBase64String(credentials.getBytes());

        requestFacade.addHeader("Authorization", basicAuth);
    }
     */
}
