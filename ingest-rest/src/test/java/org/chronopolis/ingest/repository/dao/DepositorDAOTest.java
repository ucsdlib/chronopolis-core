package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.QNode;
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
import java.util.List;

/**
 * Tests for common operations for a Depositor
 *
 * @author shake
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DepositorDAOTest extends IngestTest {

    private final String ncar = "ncar";
    private final String sdsc = "sdsc";
    private final String ucsd = "ucsd";
    private final String umiacs = "umiacs";
    private final String namespace = "test-depositor";
    private final QDepositor qDepositor = QDepositor.depositor;

    @Autowired
    private EntityManager entityManager;

    private PagedDAO dao;

    @Before
    public void setup() {
        dao = new PagedDAO(entityManager);
    }

    @Test
    public void newDepositor() {
        Depositor depositor = new Depositor();
        depositor.setNamespace("new-namespace");
        depositor.setSourceOrganization("new-source-organization");
        depositor.setOrganizationAddress("new-organization-address");

        depositor.addContact(new DepositorContact()
                .setContactName("new-name")
                .setContactPhone("new-phone")
                .setContactEmail("new-email"));
        depositor.addContact(new DepositorContact()
                .setContactName("new-name-2")
                .setContactPhone("new-phone-2")
                .setContactEmail("new-email-2"));
        dao.findAll(QNode.node).forEach(depositor::addNodeDistribution);

        dao.save(depositor);

        Depositor saved = dao.findOne(qDepositor, qDepositor.namespace.eq("new-namespace"));
        Assert.assertNotNull(saved);
        Assert.assertEquals("new-source-organization", saved.getSourceOrganization());
        Assert.assertEquals("new-organization-address", saved.getOrganizationAddress());
        Assert.assertEquals(4, saved.getNodeDistributions().size());
        Assert.assertEquals(2, saved.getContacts().size());
    }

    @Test
    public void testDeleteAllReplicatingNodes() {
        List<Node> all = dao.findAll(QNode.node, QNode.node.username.in(umiacs, ncar, sdsc));
        Depositor one = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        all.forEach(one::removeNodeDistribution);
        dao.save(one);

        Depositor saved = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        Assert.assertEquals(0, saved.getNodeDistributions().size());
    }

    @Test
    public void testAddOneNode() {
        Depositor one = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        dao.save(one);
        Node ucsdNode = dao.findOne(QNode.node, QNode.node.username.eq(ucsd));
        one.addNodeDistribution(ucsdNode);
        dao.save(one);

        Depositor saved = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        Assert.assertEquals(4, saved.getNodeDistributions().size());
    }

    @Test
    public void testRemoveAndAddNodes() {
        Depositor one = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        dao.findAll(QNode.node, QNode.node.username.in(umiacs, ncar, sdsc)).forEach(one::removeNodeDistribution);
        dao.save(one);

        dao.findAll(QNode.node).forEach(one::addNodeDistribution);
        dao.save(one);

        Depositor saved = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        Assert.assertEquals(4, saved.getNodeDistributions().size());
    }

    @Test
    public void testAddContact() {
        DepositorContact contact = new DepositorContact()
                .setContactName("test-name")
                .setContactEmail("test-email")
                .setContactPhone("test-phone");
        Depositor one = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        one.addContact(contact);
        dao.save(one);

        Depositor saved = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        Assert.assertEquals(1, saved.getContacts().size());
        Assert.assertTrue(saved.getContacts().contains(contact));
    }

    @Test
    public void testAddContacts() {
        DepositorContact contact = new DepositorContact()
                .setContactName("test-name")
                .setContactEmail("test-email")
                .setContactPhone("test-phone");
        DepositorContact contact2 = new DepositorContact()
                .setContactName("test-name-2")
                .setContactEmail("test-email-2")
                .setContactPhone("test-phone-2");

        Depositor one = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        one.addContact(contact);
        one.addContact(contact2);

        Depositor saved = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        Assert.assertEquals(2, saved.getContacts().size());
        Assert.assertTrue(saved.getContacts().contains(contact));
        Assert.assertTrue(saved.getContacts().contains(contact2));
    }

}
