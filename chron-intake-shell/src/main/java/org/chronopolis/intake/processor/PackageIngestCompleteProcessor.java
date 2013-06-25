/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.processor;

import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;

/**
 *
 * @author shake
 */
public class PackageIngestCompleteProcessor implements ChronProcessor {

    @Override
    public void process(ChronMessage2 chronMessage) {
        if ( !(chronMessage instanceof PackageIngestCompleteMessage) ) {

        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
