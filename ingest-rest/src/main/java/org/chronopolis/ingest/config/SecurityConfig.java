package org.chronopolis.ingest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import javax.sql.DataSource;

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

        http.csrf().disable().authorizeRequests()
                .antMatchers("/api/**").hasRole("USER")
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

}
