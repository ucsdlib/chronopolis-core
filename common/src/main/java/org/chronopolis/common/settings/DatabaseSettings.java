package org.chronopolis.common.settings;

/**
 * Created by shake on 10/16/14.
 */
public interface DatabaseSettings {

    String getDriverClass();
    String getURL();
    String getUsername();
    String getPassword();

}
