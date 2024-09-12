package com.bugbycode.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Order(0)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${spring.web.login.username:root}")
	private String username;
	
	@Value("${spring.web.login.password:root}")
	private String password;
	
	@Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
	public InMemoryUserDetailsManager inMemoryUserDetailsManager() { 
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder().encode(password)).roles("LOGIN").build());
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEventPublisher.class)
    public DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher(ApplicationEventPublisher delegate) { 
        return new DefaultAuthenticationEventPublisher(delegate);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
    	
    	http.anonymous(any -> any.disable())
    	
    	.csrf(csrf -> csrf.disable())
    	
    	.authorizeHttpRequests(authorize -> authorize
    			
    	        .requestMatchers("/home","/query","/updateForwardById").hasRole("LOGIN")
    	        
    	        .anyRequest().authenticated()
    	        
    			).formLogin(Customizer.withDefaults());
    	
    	return http.build();
    }
    
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
