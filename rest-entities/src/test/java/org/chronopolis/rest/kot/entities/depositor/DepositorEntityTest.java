package org.chronopolis.rest.kot.entities.depositor;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.kot.entities.JPAContext;
import org.chronopolis.rest.kot.entities.Node;
import org.chronopolis.rest.kot.entities.QNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.util.HashSet;

/**
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DepositorEntityTest {

    private final String ADDRESS = "test-address";
    private final String ORGANIZATION = "depositor-entity-test";
    private final String contactName = "test-persist-contact";
    private final String contactPhone = "test-persist-contact-phone";
    private final String contactEmail = "test-persist-contact-email";

    @Autowired
    private EntityManager entityManager;

    private Node sdsc;
    private Node ncar;

    @Before
    public void initFromDb() {
        JPAQueryFactory query = new JPAQueryFactory(entityManager);

        sdsc = query.selectFrom(QNode.node)
                .where(QNode.node.username.eq("sdsc"))
                .fetchOne();
        ncar = query.selectFrom(QNode.node)
                .where(QNode.node.username.eq("ncar"))
                .fetchOne();

        Assert.assertNotNull(sdsc);
        Assert.assertNotNull(ncar);
    }

    @Test
    public void testDepositorPersist() {
        final String namespace = "test-persist";

        Depositor depositor = new Depositor(namespace, ORGANIZATION, ADDRESS);
        depositor.setContacts(new HashSet<>());
        depositor.setNodeDistributions(new HashSet<>());

        DepositorContact contact = new DepositorContact(contactName, contactPhone, contactEmail);
        depositor.addContact(contact);
        depositor.addNodeDistribution(sdsc);

        entityManager.persist(depositor);

        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        Depositor fetch = query.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq(namespace))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertEquals(depositor, fetch);
        Assert.assertNotEquals(0L, depositor.getId());

        Assert.assertEquals(1, fetch.getContacts().size());
        Assert.assertEquals(1, fetch.getNodeDistributions().size());

        fetch.getContacts().forEach(fContact -> {
            Assert.assertEquals(contact.getContactName(), fContact.getContactName());
            Assert.assertEquals(contact.getContactEmail(), fContact.getContactEmail());
            Assert.assertEquals(contact.getContactPhone(), fContact.getContactPhone());
        });

        fetch.getNodeDistributions().forEach(dist -> Assert.assertEquals(sdsc, dist));
    }

    @Test
    public void testDepositorMerge() {
        final String namespace = "test-depositor-merge";

        // Similar to bag merge, don't set contacts or distributions
        Depositor depositor = new Depositor(namespace, ORGANIZATION, ADDRESS);
        entityManager.persist(depositor);
        entityManager.refresh(depositor);

        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        Depositor merge = query.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq(namespace))
                .fetchOne();

        Assert.assertNotNull(merge);
        DepositorContact contact = new DepositorContact(contactName, contactPhone, contactEmail);
        merge.addContact(contact);
        merge.addNodeDistribution(sdsc);

        entityManager.merge(merge);
        entityManager.flush();

        Depositor fetch = query.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq(namespace))
                .fetchOne();

        // might be able to put all this in a single function but not really that important
        Assert.assertNotNull(fetch);
        Assert.assertEquals(merge, fetch);

        Assert.assertEquals(1, fetch.getContacts().size());
        Assert.assertEquals(1, fetch.getNodeDistributions().size());

        fetch.getContacts().forEach(fContact -> {
            Assert.assertEquals(contact.getContactName(), fContact.getContactName());
            Assert.assertEquals(contact.getContactEmail(), fContact.getContactEmail());
            Assert.assertEquals(contact.getContactPhone(), fContact.getContactPhone());
        });

        fetch.getNodeDistributions().forEach(dist -> Assert.assertEquals(sdsc, dist));
    }

    @Test
    public void testDepositorRmKeys() {
        // initial setup
        // 1 depositor
        // 2 contacts
        // 2 distributions
        final String namespace = "test-rm";
        final String extraEmail = "extra-email";

        Depositor depositor = new Depositor(namespace, ORGANIZATION, ADDRESS);
        depositor.setContacts(new HashSet<>());
        depositor.setNodeDistributions(new HashSet<>());

        DepositorContact contact = new DepositorContact(contactName, contactPhone, contactEmail);
        DepositorContact contact1 = new DepositorContact(contactName, contactPhone, extraEmail);
        depositor.addContact(contact);
        depositor.addContact(contact1);
        depositor.addNodeDistribution(sdsc);
        depositor.addNodeDistribution(ncar);

        entityManager.persist(depositor);
        entityManager.flush();

        // first rm
        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        Depositor rm = query.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq(namespace))
                .fetchOne();

        Assert.assertNotNull(rm);
        rm.removeContact(contact1);
        rm.removeNodeDistribution(ncar);

        entityManager.merge(rm);
        entityManager.flush();
        entityManager.refresh(rm);

        Assert.assertEquals(1, rm.getContacts().size());
        Assert.assertEquals(1, rm.getNodeDistributions().size());
        Assert.assertFalse(rm.getContacts().contains(contact1));
        Assert.assertFalse(rm.getNodeDistributions().stream()
                .anyMatch(dist -> dist.equals(ncar)));

        // second rm, should be empty
        rm.removeContact(contact);
        rm.removeNodeDistribution(sdsc);

        entityManager.merge(rm);
        entityManager.flush();
        entityManager.refresh(rm);

        Assert.assertEquals(0, rm.getContacts().size());
        Assert.assertEquals(0, rm.getNodeDistributions().size());
    }
}
