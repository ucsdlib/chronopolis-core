/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

/**
 *
 * @author shake
 */
public interface BagElementProcessor {
    public boolean valid();
    public void create();
}
