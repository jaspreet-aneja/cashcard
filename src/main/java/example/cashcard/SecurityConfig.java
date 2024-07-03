package example.cashcard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		/*
		 * All HTTP requests to cashcards/ endpoints are required to be authenticated
		 * using HTTP Basic Authentication security (username and password).
		 * 
		 * Also, do not require CSRF security.
		 */

		// enable RBAC: Replace the .authenticated() call with the hasRole(...) call.
		http.authorizeHttpRequests(request -> request.requestMatchers("/cashcards/**").hasRole("CARD-OWNER"))
				.httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * For tests, we'll configure a test-only service that Spring Security will use
	 * for this purpose: An InMemoryUserDetailsManager. Similar to how we configured
	 * an in-memory database using H2 for testing Spring Data, we'll configure an
	 * in-memory service with test users to test Spring Security.
	 * 
	 * 
	 * @param passwordEncoder
	 * @return
	 */
	@Bean
	UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
		User.UserBuilder userBuilder = User.builder();

		/**
		 * configure a user named sarah1 with password abc123.
		 * 
		 * Spring's IoC container will find the UserDetailsService Bean and Spring Data
		 * will use it when needed
		 */

		UserDetails sarah = userBuilder.username("sarah1").password(passwordEncoder.encode("abc123"))
				.roles("CARD-OWNER").build();
		UserDetails hankOwnNoCards = userBuilder.username("hank").password(passwordEncoder.encode("abcdef"))
				.roles("NON-OWNER").build();

		return new InMemoryUserDetailsManager(sarah, hankOwnNoCards);
	}
}