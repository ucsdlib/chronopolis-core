package org.chronopolis.ingest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

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
        auth.inMemoryAuthentication().withUser("admin").password("password").roles("ADMIN");
        auth.inMemoryAuthentication().withUser("umiacs").password("umiacs").roles("USER");
        auth.inMemoryAuthentication().withUser("sdsc").password("sdsc").roles("USER");
        auth.inMemoryAuthentication().withUser("ncar").password("ncar").roles("USER");

        // todo testing with
        /*
        auth.jdbcAuthentication().dataSource(dataSource)
                .usersByUsernameQuery("SELECT username,password FROM node where username=?")
                .authoritiesByUsernameQuery("SELECT username, role FROM user_role WHERE username=?");
        */

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
}
