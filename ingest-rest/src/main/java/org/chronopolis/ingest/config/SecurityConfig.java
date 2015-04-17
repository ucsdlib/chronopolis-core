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
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Created by shake on 11/10/14.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    DataSource dataSource;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication()
                .dataSource(this.dataSource);
                // TODO: Get the password encoder working
                // .passwordEncoder(new ShaPasswordEncoder());

        // We're going to keep our user and node domain objects split for now
        // ie: let the spring security stuff worry about authentication
        // otherwise we could do something like this to use our node domain object
        // .usersByUsernameQuery("select username, password, enabled from node where username=?");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        /**
         * <http pattern="/restful/**" create-session="stateless">
         * <intercept-url pattern='/**' access='ROLE_REMOTE' />
         * <http-basic />
         * </http>
         */

        /**
         * Most of the time the client only interacts through GETs and POSTs,
         * whereas the admin user will also PUT in order to create bags or
         * restore requests. However, a client may also PUT on
         * /api/restorations/{id}, so we need to add that as well
         */

        AccessDecisionManager decisionManager = accessDecisionManager();

        http.csrf().disable().authorizeRequests()
                .accessDecisionManager(decisionManager)
                .antMatchers(HttpMethod.GET, "/api/**").hasRole("USER")
                .antMatchers(HttpMethod.POST, "/api/**").hasRole("USER")
                .antMatchers(HttpMethod.PUT, "/api/restorations/**").hasRole("USER")
                .antMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                .antMatchers("/css/**").permitAll()
                .antMatchers("/js/**").permitAll()
                .antMatchers("/bags").hasRole("USER")
                .antMatchers("/replications").hasRole("USER")
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
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        WebExpressionVoter webExpressionVoter = new WebExpressionVoter();
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        webExpressionVoter.setExpressionHandler(expressionHandler);
        return new AffirmativeBased(Arrays.asList((AccessDecisionVoter) webExpressionVoter));
    }

}
