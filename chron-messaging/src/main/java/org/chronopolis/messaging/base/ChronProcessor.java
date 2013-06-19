/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.base;

/**
 *
 * @author shake
 */
public interface ChronProcessor {

    public void process(ChronMessage2 chronMessage);
    
}
