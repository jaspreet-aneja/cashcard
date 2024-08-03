package example.cashcard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CashcardApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards/99",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards/1000",
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() {
		CashCard cashCard = new CashCard(null, 250.00, null);
		ResponseEntity<Void> createResponse = restTemplate.withBasicAuth("sarah1", "abc123").postForEntity("/cashcards",
				cashCard, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate.withBasicAuth("sarah1", "abc123")
				.getForEntity(locationOfNewCashCard, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
	}

	@Test
	void shouldReturnAPageOfCashCards() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfCashCards() {
//		asc for ascending order 
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
//		Spring provides the default page and size values (they are 0 and 20, respectively)
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("BAD-USER", "abc123").getForEntity("/cashcards/99",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate.withBasicAuth("sarah1", "BAD-PASSWORD").getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("hank", "abcdef").getForEntity("/cashcards/99",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards/102",
				String.class); // kumar2's data
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() {
		CashCard cashCard = new CashCard(null, 19.99, null);

		HttpEntity<CashCard> request = new HttpEntity<CashCard>(cashCard);
		ResponseEntity<Void> response = restTemplate.withBasicAuth("sarah1", "abc123").exchange("/cashcards/99",
				HttpMethod.PUT, request, Void.class);
		/**
		 * The exchange() method is a more general version of the xyzForEntity() methods
		 * we've used in other tests: exchange() requires the verb and the request
		 * entity (the body of the request) to be supplied as parameters.
		 * 
		 * Using getForEntity() as an example, you can imagine that the following two
		 * lines of code accomplish the same goal:
		 * 
		 * .exchange("/cashcards/99", HttpMethod.GET, new HttpEntity(null),
		 * String.class);
		 * 
		 * The above line is functionally equivalent to the following line:
		 * 
		 * .getForEntity("/cashcards/99", String.class);
		 */
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
		ResponseEntity<Void> response = restTemplate.withBasicAuth("sarah1", "abc123").exchange("/cashcards/99999",
				HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		CashCard kumarsCard = new CashCard(null, 333.33, null);
		HttpEntity<CashCard> request = new HttpEntity<>(kumarsCard);
		ResponseEntity<Void> response = restTemplate.withBasicAuth("sarah1", "abc123").exchange("/cashcards/102",
				HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	// Notice that we've added the @DirtiesContext annotation. We'll add this
	// annotation to all tests which change the data. If we don't, then these tests
	// could affect the result of other tests in the file.
	void shouldDeleteAnExistingCashCard() {
		ResponseEntity<Void> response = restTemplate.withBasicAuth("sarah1", "abc123").exchange("/cashcards/99",
				HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

//		Why not use RestTemplate.delete()?
		/**
		 * Notice that we're using RestTemplate.exchange() even though RestTemplate
		 * supplies a method that looks like we could use: RestTemplate.delete().
		 * However, let's look at the signature: public class RestTemplate ... { public
		 * void delete(String url, Object... uriVariables)
		 * 
		 * The other methods we've been using (such as getForEntity() and exchange())
		 * return a ResponseEntity, but delete() doesn't. Instead, it's a void method.
		 * Why is this?
		 * 
		 * The Spring Web framework supplies the delete() method as a convenience, but
		 * it comes with some assumptions:
		 * 
		 * A response to a DELETE request will have no body. The client shouldn't care
		 * what the response code is unless it's an error, in which case, it'll throw an
		 * exception. Given those assumptions, no return value is needed from delete().
		 * 
		 * But, the second assumption makes delete() unsuitable for us: We need the
		 * ResponseEntity in order to assert on the status code! So, we won't use the
		 * convenience method, but rather let's use the more general method: exchange().
		 */

		ResponseEntity<String> getResponse = restTemplate.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
	
	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() {
	    ResponseEntity<Void> deleteResponse = restTemplate
	            .withBasicAuth("sarah1", "abc123")
	            .exchange("/cashcards/99999", HttpMethod.DELETE, null, Void.class);
	    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
	
	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
	    ResponseEntity<Void> deleteResponse = restTemplate
	            .withBasicAuth("sarah1", "abc123")
	            .exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
	    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	    
	    ResponseEntity<String> getResponse = restTemplate
	            .withBasicAuth("kumar2", "xyz789")
	            .getForEntity("/cashcards/102", String.class);
	    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
