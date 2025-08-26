package co.com.techskill.lab2.library.service.impl;

import co.com.techskill.lab2.library.config.PetitionMapper;
import co.com.techskill.lab2.library.config.PetitionMapperImpl;
import co.com.techskill.lab2.library.domain.dto.PetitionDTO;
import co.com.techskill.lab2.library.repository.IBookRepository;
import co.com.techskill.lab2.library.repository.IPetitionRepository;
import co.com.techskill.lab2.library.service.IPetitionService;
import co.com.techskill.lab2.library.service.dummy.PetitionService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PetitionServiceImpl implements IPetitionService {

    private final IPetitionRepository petitionRepository;
    private final IBookRepository bookRepository;
    private final PetitionMapper petitionMapper;
    private final PetitionService petitionService;

    private static final String TYPE="RETURN";
    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("petitionService");

    public PetitionServiceImpl(IPetitionRepository petitionRepository, IBookRepository bookRepository,PetitionService petitionService) {
        this.petitionRepository = petitionRepository;
        this.bookRepository = bookRepository;
        this.petitionMapper = new PetitionMapperImpl();
        this.petitionService = petitionService;
    }
    @Override
    public Flux<PetitionDTO> findALl() {
        return petitionService
                .dummyFindAll();
    }

    @Override
    public Mono<PetitionDTO> findById(String id) {
        return petitionService
                .dummyFindById(id);
    }

    @Override
    public Mono<PetitionDTO> save(PetitionDTO petitionDTO) {
        petitionDTO.setPetitionId(UUID.randomUUID().toString().substring(0,10));
        petitionDTO.setSentAt(LocalDate.now());
        return petitionRepository
                .save(petitionMapper.toEntity(petitionDTO))
                .map(petitionMapper::toDTO);
    }

    @Override
    public Flux<PetitionDTO> findByPriority(Integer p) {
        return petitionRepository.findAll()
                .filter(petition -> Objects.equals(petition.getPriority(), p))
                .map(petitionMapper::toDTO);
    }

    @Override
    public Flux<String> checkPriorities(PetitionDTO petitionDTO) {
        return findByPriority(petitionDTO.getPriority())
                .map(pt -> LocalTime.now() + " - Check priority with level: " + pt.getPriority()
                        + ", Petition ID: " + pt.getPetitionId()
                        + ", For book ID: " + pt.getBookId() + "\n")
                .delayElements(Duration.ofMillis(1000))
                .doOnNext(System.out::print);
    }

    //TO-DO: Simular una petición con tipo = LEND que falla si el libro asociado no está disponible.
    @Override
    public Flux<String> processPetition(PetitionDTO petitionDTO) {
        return petitionRepository.findAll()
                .filter(petition -> "LEND".equals(petitionDTO.getType()))
                .switchIfEmpty(Mono.error(new RuntimeException("Petition of type LEND not found")))
                .flatMap(petition -> bookRepository.findByBookId(petition.getBookId())
                        .map(book -> "Petition approved for book: " + book.getBookId())

                ).onErrorResume(e -> Mono.just("Petition failed for book: " + petitionDTO.getBookId() + e.getMessage()));

    }


    @Override
    public Mono<String> simulateIntermittency(PetitionDTO petitionDTO) {
        return petitionRepository.findByPetitionId(petitionDTO.getPetitionId())
                .switchIfEmpty(Mono.error(new RuntimeException("Petition not found")))
                .flatMap(petition -> {
                    if (Math.random() < 0.5) {
                        return Mono.error(new RuntimeException("Intermitence failure"));
                        }
                        return Mono.just("Petition processed: "+petition.getPetitionId());
                    }
                )
                .timeout(Duration.ofSeconds(5))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .retry(3)
                .onErrorResume(e -> Mono.just("Petition failed: "+petitionDTO.getPetitionId() + " - " + e.getMessage()));

    }
    static class TransientFailure extends RuntimeException {
        TransientFailure(String msg) { super(msg); }
    }

    @Override
    public Flux<String> peticionesReturn() {
        return findALl()
                .filter(p -> TYPE.equalsIgnoreCase(p.getType()))
                .flatMap(p ->
                        reglasDeNegocio(p)
                                .timeout(Duration.ofMillis(500))
                                .retryWhen(
                                        Retry.backoff(2, Duration.ofMillis(150))
                                                .filter(ex ->
                                                        ex instanceof PetitionServiceImpl.TransientFailure ||
                                                                ex instanceof java.util.concurrent.TimeoutException
                                                )
                                ).timeout(Duration.ofSeconds(5))
                                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                                .onErrorResume(ex -> Mono.just(fallbackReturn(p, ex)))
                );
    }




    private Mono<String> reglasDeNegocio(PetitionDTO p) {

        if (p.getSentAt() != null && p.getSentAt().isBefore(LocalDate.now().minusDays(3))) {
            return Mono.error(new IllegalStateException("RETURN expirada Mayor a 3 días)"));
        }
        int latency = ThreadLocalRandom.current().nextInt(100, 701);
        boolean transientFail = ThreadLocalRandom.current().nextInt(100) < 15;

        return Mono.defer(() -> transientFail
                        ? Mono.<String>error(new PetitionServiceImpl.TransientFailure("Falla por latencia"))
                        : Mono.just("RETURN OK id=%s t=%dms".formatted(p.getPetitionId(), latency)))
                .delayElement(Duration.ofMillis(latency));
    }

    private String fallbackReturn(PetitionDTO p, Throwable ex) {
        if (ex instanceof IllegalStateException) {
            return "[FALLBACK] id=%s → Rechazada por plazo".formatted(p.getSentAt())+"\n";
        }
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return "[FALLBACK] id=%s → Timeout".formatted(p.getPetitionId())+"\n";
        }
        if (ex instanceof PetitionServiceImpl.TransientFailure) {
            return "[FALLBACK] id=%s → Transitorio (reintentos agotados)".formatted(p.getPetitionId())+"\n";
        }
        return "[FALLBACK] id=%s → Error: %s".formatted(p.getPetitionId(), ex.getMessage())+"\n";
    }

}
