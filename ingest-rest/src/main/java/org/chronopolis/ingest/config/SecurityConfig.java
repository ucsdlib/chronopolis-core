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
    private static final String UI_STORAGE = "/regions/**";
    private static final String UI_DEPOSITOR = "/depositors/**";
    private static final String UI_REPLICATION = "/replications/**";
    private static final String UI_USER = "/users/**";
    private static final String UI_USER_ADD = "/users/add";
    private static final String UI_USER_UPDATE = "/users/update";

    private static final String API_ROOT = "/api/**";
    private static final String API_BAG_ROOT = "/api/bags/**";
    private static final String API_REPAIR_ROOT = "/api/repairs/**";
    private static final String API_STORAGE_ROOT = "/api/storage/**";
    private static final String API_DEPOSITOR_ROOT = "/api/depositors/**";
    private static final String API_REPLICATION_ROOT = "/api/replications/**";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, DataSource dataSource) throws Exception {
        // We're going to keep our user and node domain objects split for now
        // ie: let the spring security stuff worry about authentication
        // otherwise we could do something like this to use our node domain object
        // .usersByUsernameQuery("select username, password, enabled from node where username=?");

        auth.jdbcAuthentication()
                .passwordEncoder(passwordEncoder())
                .dataSource(dataSource);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        /*
         * Most of the time the client only interacts through GETs and POSTs,
         * whereas the admin user will also PUT in order to create bags or
         * restore requests. However, a client may also PUT on
         * /api/restorations/{id}, so we need to add that as well
         */

        AccessDecisionManager decisionManager = accessDecisionManager();

        http.csrf().disable().authorizeRequests()
                .accessDecisionManager(decisionManager)
                // api paths
                .antMatchers(HttpMethod.GET, API_ROOT).hasRole("SERVICE")
                .antMatchers(HttpMethod.PUT, API_ROOT).hasRole("USER")
                .antMatchers(HttpMethod.POST, API_REPAIR_ROOT).hasRole("USER")
                .antMatchers(HttpMethod.POST, API_BAG_ROOT, API_REPLICATION_ROOT, API_STORAGE_ROOT, API_DEPOSITOR_ROOT).hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, API_ROOT).hasRole("ADMIN")
                // Webapp paths
                // resources
                .antMatchers("/css/**", "/js/**", "/").permitAll()
                // controllers
                .antMatchers(HttpMethod.GET, UI_BAG, UI_REPLICATION, UI_USER, UI_REPAIR, UI_DEPOSITOR, UI_STORAGE).hasRole("USER")
                .antMatchers(HttpMethod.POST, UI_REPAIR, UI_USER_UPDATE).hasRole("USER")
                .antMatchers(HttpMethod.POST, UI_BAG, UI_REPLICATION, UI_USER_ADD, UI_DEPOSITOR, UI_STORAGE).hasRole("ADMIN")
                // .antMatchers("/").permitAll()
                //     .anyRequest().permitAll()
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
    public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager();
        manager.setDataSource(dataSource);
        return manager;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER\nROLE_USER > ROLE_SERVICE");
        return hierarchy;
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        WebExpressionVoter webExpressionVoter = new WebExpressionVoter();
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        webExpressionVoter.setExpressionHandler(expressionHandler);
        List<AccessDecisionVoter<?>> voters = new ArrayList<>();
        voters.add(webExpressionVoter);
        return new AffirmativeBased(voters);
    }

}
