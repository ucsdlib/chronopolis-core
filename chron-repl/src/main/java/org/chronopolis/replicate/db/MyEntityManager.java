/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.db;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 *
 * @author toaster
 */
public class MyEntityManager extends AbstractManager<MyEntity> {

    public MyEntityManager(Dba dba) {
        super(MyEntity.class, dba);
    }

    /**
     * Find an entity by name
     * @param name name to search for
     * @return found entity or null of none found
     */
    public MyEntity findByName(String name) {
        checkDba();
        // These are stored on the Entity class
        TypedQuery<MyEntity> q = getDba().getActiveEm().createNamedQuery("MyEntity.findByName", MyEntity.class);
        // there is a corresponding :name in the where clause
        q.setParameter("name", name);
        try {
            return q.getSingleResult(); // default throws error on multiple or null
        } catch (NoResultException e) {
            return null;
        }
    }
}
