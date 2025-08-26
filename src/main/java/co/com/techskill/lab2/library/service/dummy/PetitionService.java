package co.com.techskill.lab2.library.service.dummy;

import co.com.techskill.lab2.library.domain.dto.PetitionDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PetitionService {
    private final List<PetitionDTO> petitions = new ArrayList<>();
    LocalDate fechaBase = LocalDate.of(2025, 8, 25);

    public PetitionService(){
        petitions.add(new PetitionDTO("09c09cc8-b", "LEND", 5, "6600ab76-3", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("2f5fca21-b", "RETURN", 7, "12a13228-0", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("4c9ef769-9", "LEND", 7, "51ed516f-a", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("5b2dae36-f", "LEND", 3, "51ed516f-a", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("ad4801f0-9", "RETURN", 5, "51ed516f-a", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("9cc825c1-7", "RETURN", 7, "12a13228-0", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("d5120259-4", "LEND", 4, "11b553eb-b", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("09ef7d35-d", "RETURN", 4, "297c17d8-4", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("0e6a31b1-f", "RETURN", 4, "6600ab76-3", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("905dfc53-7", "LEND", 5, "6600ab76-3", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("4ebc9aa6-f", "RETURN", 7, "3c24c2fa-3", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("6d7e3b2c-5", "LEND", 4, "eb25c2d4-7", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("2a6214f1-c", "RETURN", 3, "eb25c2d4-7", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("8595a9b7-7", "RETURN", 7, "51ed516f-a", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("890fd155-0", "LEND", 2, "51ed516f-a", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("2da99667-d", "LEND", 4, "1940136a-2", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("cbfdd0aa-c", "RETURN", 7, "1940136a-2", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("0ff09c9e-5", "LEND", 6, "11b553eb-b", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("86084e60-e", "RETURN", 7, "11b553eb-b", restarDiasAleatorio(fechaBase)));
        petitions.add(new PetitionDTO("742330cf-0", "LEND", 6, "12a13228-0", restarDiasAleatorio(fechaBase)));
    }
    public static LocalDate restarDiasAleatorio(LocalDate fecha) {
        // Posibles días a restar
        int[] dias = {1, 3};

        // Selección aleatoria
        int idx = ThreadLocalRandom.current().nextInt(dias.length);
        int diasARestar = dias[idx];

        // Restar días y devolver la nueva fecha
        return fecha.minusDays(diasARestar);
    }
    public Flux<PetitionDTO> dummyFindAll(){
        return Flux.fromIterable(petitions);
    }

    public Flux<PetitionDTO> typeReturnAll(){
        return Flux.fromIterable(petitions);
    }

    public Mono<PetitionDTO> dummyFindById(String id){
        return Mono.justOrEmpty(
                petitions.stream()
                        .filter(petitionDTO -> petitionDTO.getPetitionId().equals(id))
                        .findFirst()
        );
    }

    //TO - DO: Challenge #2





}
