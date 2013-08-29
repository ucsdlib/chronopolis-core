/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

/**
 *
 * @author shake
 */
public interface TagProcessor {
    /** Validate the tag file.
     *      ex: bag-info.txt: Calculate the payload oxum and validate against
     *                        the held record
     * 
     * @return If the tag file is valid
     */
    public boolean valid();

    /** Create a new tag file for the defined tag. 
     * 
     * 
     */
    public void create();
}
