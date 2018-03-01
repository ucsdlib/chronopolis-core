package org.chronopolis.ingest.repository.dao;

import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.PersistableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.support.PageableExecutionUtils;

import javax.persistence.EntityManager;

/**
 * DAO to support basic fetching, saving, and removal from an EntityManager. Can be extended to
 * provide additional support for queries if any are needed.
 *
 * Note that some deprecated methods are still called here while we require less of
 * Spring for JPA work. Soon we'll need to replace that with our own utilities I guess, or see
 * if there are some lightweight libraries for Pagination. Either way it should only be a class
 * or two so not much.
 *
 * todo: would it be better to send a predicate instead of a filter?
 *       maybe have support for both
 *
 * @author shake
 */
public class PagedDAO {

    private final EntityManager em;

    public PagedDAO(EntityManager em) {
        this.em = em;
    }

    public <T extends PersistableEntity> T findOne(EntityPath<T> path, Paged filter) {
        JPAQueryFactory factory = new JPAQueryFactory(em);
        return factory.selectFrom(path)
                .where(filter.getQuery())
                .fetchOne();
    }

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
     *
     * This combines em.persist and em.merge into one method. If the entity does not have an ID,
     * it will be persisted; otherwise it will be merged.
     *
     * @param t The entity to merge
     * @param <T> The type of the entity. Must extend PersistableEntity in order to get the id
     */
    public <T extends PersistableEntity> void save(T t) {
        em.getTransaction().begin();
        if (t.getId() == null) {
            em.persist(t);
        } else {
            em.merge(t);
        }
        em.getTransaction().commit();
    }

    /**
     * Remove an entity from the database using the EntityManager
     *
     * @param t the entity to remove
     * @param <T> the type of the entity
     */
    public <T extends PersistableEntity> void delete(T t) {
        if (t.getId() == null) {

        } else {
            em.getTransaction().begin();
            em.remove(t);
            em.getTransaction().commit();
        }
    }

}
