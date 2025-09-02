package com.nttdata.account_service.integration.transactions;

import com.nttdata.account_service.integration.customers.EligibilityResponse;
import com.nttdata.account_service.integration.transactions.dtos.TransactionCreateDTO;
import com.nttdata.account_service.integration.transactions.dtos.TransactionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionsClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.transactions.url}")
    private String baseUrl; // http://localhost:8086/api/v1

    public Mono<TransactionResponseDTO> postTransaction(TransactionCreateDTO transactionCreateDTO) {
        return webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/transactions")
                .bodyValue(transactionCreateDTO)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(TransactionResponseDTO.class);
    }
}
