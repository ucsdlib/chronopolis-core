package org.chronopolis.ingest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for our login/per page authorization
 *
 * Created by shake on 11/10/14.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    // Paths
    private static final String UI_BAG = "/bags/**";
    private static final String UI_REPAIR = "/repairs/**";
    private static final String UI_REPLICATION = "/replications/**";
    private static final String UI_USER = "/users/**";
    private static final String UI_USER_ADD = "/users/add";
    private static final String UI_USER_UPDATE = "/users/update";

    private static final String API_ROOT = "/api/**";
    private static final String API_BAG_ROOT = "/api/bags/**";
    private static final String API_REPLICATION_ROOT = "/api/replications/**";
    private static final String API_REPAIR_ROOT = "/api/repairs/**";

    @Autowired
    DataSource dataSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication()
                .passwordEncoder(passwordEncoder())
                .dataSource(this.dataSource);

        // We're going to keep our user and node domain objects split for now
        // ie: let the spring security stuff worry about authentication
        // otherwise we could do something like this to use our node domain object
        // .usersByUsernameQuery("select username, password, enabled from node where username=?");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        /*
         * <http pattern="/restful/**" create-session="stateless">
         * <intercept-url pattern='/**' access='ROLE_REMOTE' />
         * <http-basic />
         * </http>
         */

        /*
         * Most of the time the client only interacts through GETs and POSTs,
         * whereas the admin user will also PUT in order to create bags or
         * restore requests. However, a client may also PUT on
         * /api/restorations/{id}, so we need to add that as well
         */

        AccessDecisionManager decisionManager = accessDecisionManager();

        http.csrf().disable().authorizeRequests()
                .accessDecisionManager(decisionManager)
                // RESTful paths
                .antMatchers(HttpMethod.GET, API_ROOT).hasRole("SERVICE")
                .antMatchers(HttpMethod.PUT, API_ROOT).hasRole("USER")
                .antMatchers(HttpMethod.POST, API_REPAIR_ROOT).hasRole("USER")
                .antMatchers(HttpMethod.POST, API_BAG_ROOT, API_REPLICATION_ROOT).hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, API_ROOT).hasRole("ADMIN")
                // Webapp paths
                // resources
                .antMatchers("/css/**").permitAll()
                .antMatchers("/js/**").permitAll()
                // controllers
                .antMatchers(HttpMethod.GET, UI_BAG, UI_REPLICATION, UI_USER, UI_REPAIR).hasRole("USER")
                .antMatchers(HttpMethod.POST, UI_REPAIR, UI_USER_UPDATE).hasRole("USER")
                .antMatchers(HttpMethod.POST, UI_BAG, UI_REPLICATION, UI_USER_ADD).hasRole("ADMIN")
                .antMatchers("/").permitAll()
                    .anyRequest().permitAll()
                .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .logout()
                    .permitAll()
                .and()
            .httpBasic();

    }

    @Bean
    // This is for accessing and updating our users
    public JdbcUserDetailsManager jdbcUserDetailsManager(AuthenticationManager authenticationManager) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager();
        manager.setDataSource(dataSource);
        manager.setAuthenticationManager(authenticationManager);
        return manager;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER and ROLE_USER > ROLE_SERVICE");
        return hierarchy;
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        WebExpressionVoter webExpressionVoter = new WebExpressionVoter();
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        webExpressionVoter.setExpressionHandler(expressionHandler);
        List<AccessDecisionVoter<? extends Object>> voters = new ArrayList<>();
        voters.add(webExpressionVoter);
        return new AffirmativeBased(voters);
    }

}
