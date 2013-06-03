/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.db;

import java.util.List;
import javax.persistence.TypedQuery;

/**
 *
 * @author toaster
 */
public class AbstractManager<T extends IDEntity> {

    private Class<T> clazz;
    private Dba dba;

    AbstractManager(Class<T> clazz, Dba dba) {
        this.clazz = clazz;
        this.dba = dba;
    }

    public List<T> listAll() {
        checkDba();
        TypedQuery<T> query = dba.getActiveEm().createQuery("SELECT o FROM " + clazz.getName() + " o", clazz);
        return query.getResultList();
    }

    /**
     * Find an entity by its primary key
     *
     * @param id
     * @return found entity or null if no entity found
     */
    public T getById(long id) {

        checkDba();

        return dba.getActiveEm().find(clazz, id);
    }

    /**
     * Refresh an objects state from the database. Good for syncing objects that
     * may have become detached in a users session.
     *
     * @param obj
     * @return
     */
    public T refresh(T obj) {
        checkDba();
        return (T) dba.getActiveEm().find(obj.getClass(), obj.getId());
    }

    /**
     * Persist or merge an object
     *
     * @param obj object to persist
     * @return persisted object
     */
    public T save(T obj) {

        checkDba();

        if (obj.getId() > 0L) {

            return dba.getActiveEm().merge(obj);

        } else {
            dba.getActiveEm().persist(obj);
            return obj;
        }
    }

    protected Dba getDba() {
        return dba;
    }

    protected void checkDba() throws IllegalStateException {
        if (!dba.isActive()) {
            throw new IllegalStateException("No transaction was active!");
        }
    }
}
