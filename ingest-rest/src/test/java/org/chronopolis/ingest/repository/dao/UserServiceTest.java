package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.models.UserRequest;
import org.chronopolis.ingest.models.UserRole;
import org.chronopolis.ingest.repository.AuthoritiesRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test methods in the user service to ensure correct behavior
 *
 * Created by shake on 6/11/15.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserServiceTest extends IngestTest {

    private final String EXISTING = "umiacs";
    private final String NEW_USER = "new-user";
    private final String PASSWORD = "new-password";

    // Beans beans the magical fruit
    @Autowired NodeRepository nodes;
    @Autowired UserDetailsManager manager;
    @Autowired AuthoritiesRepository authorities;
    @MockBean PasswordEncoder encoder;

    private UserService service;

    @Before
    public void setup() {
        service = new UserService(authorities, manager, nodes, encoder);
    }

    @Test
    public void testCreateExistingUser() throws Exception {
        when(encoder.encode(eq(PASSWORD))).thenReturn(PASSWORD);
        UserRequest request = new UserRequest();
        request.setRole(UserRole.ROLE_USER);
        request.setNode(false);
        request.setUsername(EXISTING);
        request.setPassword(PASSWORD);
        service.createUser(request);

        UserDetails details = service.getUser(EXISTING);
        Assert.assertNotEquals(PASSWORD, details.getPassword());
    }

    @Test
    public void testCreateNewUser() throws Exception {
        when(encoder.encode(eq(PASSWORD))).thenReturn(PASSWORD);
        UserRequest request = new UserRequest();
        request.setRole(UserRole.ROLE_USER);
        request.setNode(false);
        request.setUsername(NEW_USER);
        request.setPassword(PASSWORD);
        service.createUser(request);

        UserDetails details = service.getUser(NEW_USER);
        Assert.assertNotNull(details);
        Assert.assertEquals(NEW_USER, details.getUsername());
    }
}