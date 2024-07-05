package example.cashcard;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {

	private final CashCardRepository cashCardRepository;

	private CashCardController(CashCardRepository cashCardRepository) {
		this.cashCardRepository = cashCardRepository;
	}

	@GetMapping("/{requestedId}")
	private ResponseEntity<CashCard> findById(@PathVariable("requestedId") Long requestedId, Principal principal) {
//		Optional<CashCard> cashCardOptional = cashCardRepository.findById(requestedId);

//		Principal is available for us to use in our Controller. The Principal holds our user's authenticated, authorized information
//		Note that principal.getName() will return the username provided from Basic Auth.
		Optional<CashCard> cashCardOptional = Optional
				.ofNullable(cashCardRepository.findByIdAndOwner(requestedId, principal.getName()));

		if (cashCardOptional.isPresent()) {
			return ResponseEntity.ok(cashCardOptional.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping
	private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCard, UriComponentsBuilder ucb, Principal principal) {
		CashCard cashCardWithOwner = new CashCard(null, newCashCard.amount(), principal.getName());
		CashCard createdCCard = cashCardRepository.save(cashCardWithOwner);
		URI locationOfNewlyCreatedCCard = ucb.path("cashcards/{id}").buildAndExpand(createdCCard.id()).toUri();
		return ResponseEntity.created(locationOfNewlyCreatedCCard).build();
	}

	@GetMapping
	private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
//		Page<CashCard> page = cashCardRepository.findAll(PageRequest.of(pageable.getPageNumber(),
//				pageable.getPageSize(), pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))));
		Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
				PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
						pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))));

		return ResponseEntity.ok(page.getContent());
	}
}
