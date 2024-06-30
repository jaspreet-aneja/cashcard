package example.cashcard;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
	private ResponseEntity<CashCard> findById(@PathVariable("requestedId") Long requestedId) {
		Optional<CashCard> cashCardOptional = cashCardRepository.findById(requestedId);

		if (cashCardOptional.isPresent()) {
			return ResponseEntity.ok(cashCardOptional.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping
	private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCard, UriComponentsBuilder ucb) {
		CashCard createdCCard = cashCardRepository.save(newCashCard);
		URI locationOfNewlyCreatedCCard = ucb.path("cashcards/{id}").buildAndExpand(createdCCard.id()).toUri();
		return ResponseEntity.created(locationOfNewlyCreatedCCard).build();
	}

	@GetMapping
	private ResponseEntity<List<CashCard>> findAll(Pageable pageable) {
		Page<CashCard> page = cashCardRepository
				.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));

		return ResponseEntity.ok(page.getContent());
	}
}
