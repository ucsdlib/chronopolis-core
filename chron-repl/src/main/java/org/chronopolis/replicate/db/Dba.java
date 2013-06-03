/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.db;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.log4j.Logger;

/**
 *
 * @author toaster
 */
public class Dba {

    private static volatile boolean initialized = false;
    private static final Boolean lock = new Boolean(true);
    private static EntityManagerFactory emf = null;
    protected Logger logger = Logger.getLogger(Dba.class);
    private EntityManager outer;

    /**
     * open Dba and also start a transaction
     */
    public Dba() {
        this(false);
    }

    /**
     * open Dba; if readonly no JPA transaction is actually started, meaning you
     * will have no persistence store. You can still persist stuff, but the
     * entities won't become managed.
     */
    public Dba(boolean readOnly) {

        initialize();
        openEm(readOnly);
    }

    public final void openEm(boolean readOnly) {
        if (outer != null) {
            return;
        }

        outer = emf.createEntityManager();

        if (readOnly == false) {
            outer.getTransaction().begin();
        }
    }

    /**
     * Test to see if there is an active transaction (ie, entity manager is open)
     * @return  true if there is an active transaction, false otherwise
     */
    boolean isActive() {
        return (outer != null);
    }
    
    /**
     * Get the outer transaction; an active transaction must already exist for
     * this to succeed.
     */
    public EntityManager getActiveEm() {
        if (outer == null) {
            throw new IllegalStateException("No transaction was active!");
        }

        return outer;
    }

    /**
     * Close the entity manager, properly committing or rolling back a
     * transaction if one is still active.
     */
    public void closeEm() {
        if (outer == null) {
            return;
        }

        try {
            if (outer.getTransaction().isActive()) {

                if (outer.getTransaction().getRollbackOnly()) {
                    outer.getTransaction().rollback();
                } else {
                    outer.getTransaction().commit();
                }
            }

        } finally {
            outer.close();
            outer = null;
        }
    }

    /**
     * Mark the transaction as rollback only, if there is an active transaction
     * to begin with.
     */
    public void markRollback() {

        if (outer != null) {
            outer.getTransaction().setRollbackOnly();
        }
    }

    public boolean isRollbackOnly() {
        return outer != null && outer.getTransaction().getRollbackOnly();
    }
    /**
     * close Entity Manager Factory if open.
     */
    public static void closeFactory() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }

    // thread safe way to initialize the entity manager factory.
    private void initialize() {

        if (initialized) {
            return;
        }

        synchronized (lock) {

            if (initialized) {
                return;
            }

            initialized = true;

            try {
                emf = Persistence.createEntityManagerFactory("chron-replPU");

            } catch (Throwable t) {
                logger.error("Failed to setup persistence unit!", t);
                throw new RuntimeException(t);
            }
        }
    }
}
