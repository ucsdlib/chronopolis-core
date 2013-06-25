/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestStatusQueryMessage;

/**
 *
 * @author shake
 */
public class PackageIngestStatusQueryProcessor implements ChronProcessor {

    @Override
    public void process(ChronMessage2 chronMessage) {
        if ( !(chronMessage instanceof PackageIngestStatusQueryMessage) ) {
            // Error out
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
