package org.chronopolis.ingest.repository.dao;

import com.querydsl.core.QueryModifiers;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.PersistableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

/**
 * DAO to support basic fetching, saving, and removal from an EntityManager. Can be extended to
 * provide additional support for queries if any are needed.
 * <p>
 * Note that some deprecated methods are still called here while we require less of
 * Spring for JPA work. Soon we'll need to replace that with our own utilities I guess, or see
 * if there are some lightweight libraries for Pagination. Either way it should only be a class
 * or two so not much.
 * <p>
 *
 * @author shake
 */
public class PagedDAO {

    private final EntityManager em;

    public PagedDAO(EntityManager em) {
        this.em = em;
    }

    /**
     * Find a single entity based on a filter (which should give a unique result)
     *
     * @param path   the EntityPath to query on
     * @param filter the filter containing the query parameters
     * @param <T>    the Type of the Entity
     * @return the entity retrieved from the database or null if it does not exist
     */
    public <T extends PersistableEntity> T findOne(EntityPath<T> path, Paged filter) {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        return factory.selectFrom(path)
                .where(filter.getQuery())
                .fetchOne();
    }

    /**
     * Find a single entity based on a Predicate
     *
     * @param path      the EntityPath to query on
     * @param predicate the predicate containing the query parameters
     * @param <T>       the Type of the Entity
     * @return the entity retrieved from the database or null if it does not exist
     */
    public <T extends PersistableEntity> T findOne(EntityPath<T> path,
                                                   Predicate predicate) {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        return factory.selectFrom(path)
                .where(predicate)
                .fetchOne();
    }

    /**
     * Find all entities of a certain type
     * <p>
     * Normally receiving a Page instead of a List is preferred; this should only be used when
     * absolutely necessary.
     *
     * @param path the EntityPath to query on
     * @param <T>  the Type of the Entity
     * @return a List containing all entities
     */
    public <T extends PersistableEntity> List<T> findAll(EntityPath<T> path) {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        return factory.selectFrom(path).fetch();
    }

    /**
     * Find all entities of a certain type matching a Predicate
     *
     * @param path the EntityPath to query on
     * @param predicate the Predicate to filter on
     * @param <T> the Type of the Entity
     * @return a List containing all Entities that match the Predicate
     */
    public <T extends PersistableEntity> List<T> findAll(EntityPath<T> path, Predicate predicate) {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        return factory.selectFrom(path)
                .where(predicate)
                .fetch();
    }

    /**
     * Find all entities of a certain type matching a Predicate and restrict/order based on a given
     * modifier and specifier.
     *
     * @param path the EntityPath to query on
     * @param predicate the Predicate to filter on
     * @param specifier the OrderSpecifier to sort the data
     * @param modifiers the QueryModifiers to restrict/limit data (replicate paging)
     * @param <T> the Type of the Entity
     * @return a List containing all Entities that match the Predicate
     */
    public <T extends PersistableEntity> List<T> findAll(EntityPath<T> path,
                                                         Predicate predicate,
                                                         OrderSpecifier specifier,
                                                         QueryModifiers modifiers) {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        return factory.selectFrom(path)
                .where(predicate)
                .orderBy(specifier)
                .restrict(modifiers)
                .fetch();
    }

    /**
     * Find entities of a certain type, limited in scope based on a filter which should give both
     * query parameters and size restrictions for the ResultSet
     *
     * @param path   the EntityPath to query on
     * @param filter the filter containing the query parameters
     * @param <T>    the Type of the Entity
     * @return a Page containing the ResultSet from querying
     */
    public <T extends PersistableEntity> Page<T> findPage(EntityPath<T> path, Paged filter) {
        PageRequest req = filter.createPageRequest();

        JPAQueryFactory factory = new JPAQueryFactory(em);
        JPAQuery<T> count = factory.selectFrom(path)
                .where(filter.getQuery());
        JPAQuery<T> query = factory.selectFrom(path)
                .where(filter.getQuery())
                .restrict(filter.getRestriction())
                .orderBy(filter.getOrderSpecifier());
        return PageableExecutionUtils.getPage(query.fetch(), req, count::fetchCount);
    }

    /**
     * Save an Entity to the database using the EntityManager
     * <p>
     * This combines em.persist and em.merge into one method. If the entity does not have an ID,
     * it will be persisted; otherwise it will be merged.
     * <p>
     * Note that for the moment transaction management is handled by Spring
     *
     * @param t   The entity to merge
     * @param <T> The type of the entity. Must extend PersistableEntity in order to get the id
     */
    @Transactional
    public <T extends PersistableEntity> void save(T t) {
        // em.getTransaction().begin();
        if (t.getId() == 0L) {
            em.persist(t);
        } else {
            em.merge(t);
        }
        em.flush();
        // em.getTransaction().commit();
    }

    /**
     * Remove an entity from the database using the EntityManager
     * <p>
     * Note that for the moment transaction management is handled by Spring
     *
     * @param t   the entity to remove
     * @param <T> the type of the entity
     */
    @Transactional
    public <T extends PersistableEntity> void delete(T t) {
        if (t.getId() != 0L) {
            // em.getTransaction().begin();
            em.remove(t);
            em.flush();
            // em.getTransaction().commit();
        }
        // warn if null?
    }

    /**
     * Return a {@link JPAQueryFactory} for use in a more customized query
     *
     * @return a new JPAQueryFactory
     */
    public JPAQueryFactory getJPAQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
