package com.nttdata.accountservice.cache;

import com.nttdata.accountservice.integration.customers.EligibilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static com.nttdata.accountservice.cache.CacheKeys.CREDITS_HAS_CARD;
import static com.nttdata.accountservice.cache.CacheKeys.ELIGIBILITY;
/**
 * Servicio de caché para datos de catálogo como elegibilidad y estado de tarjetas de crédito.
 */
@Service
@RequiredArgsConstructor
public class CatalogCacheService {
  // Redis templates para diferentes tipos de datos en caché
  private final ReactiveRedisTemplate<String, EligibilityResponse> eligibilityRedis;
  private final ReactiveRedisTemplate <String, Boolean> booleanRedis;
// Duraciones de TTL configurables para cada tipo de caché
  @Value("${cache.ttl.eligibility:PT10M}")
  private Duration eligibilityTtl;
  @Value("${cache.ttl.boolean:PT5M}")
  private Duration creditsHasCardTtl;
// Método genérico para obtener datos de caché o cargar desde la fuente si no está en caché
  public Mono<EligibilityResponse> getEligibility(String documentType, String documentNumber,
                                                  Supplier<Mono<EligibilityResponse>> loader) {
    final String key = ELIGIBILITY + ":" + documentType + ":" + documentNumber;
    // Intenta obtener de caché, si no está presente, usa el loader para obtener y almacenar en caché
    return eligibilityRedis.opsForValue().get(key)
        // Si no está en caché, llama al loader
        .switchIfEmpty(Mono.defer(() -> loader.get()
            // Almacena el valor obtenido en caché con TTL
            .flatMap(value -> eligibilityRedis.opsForValue().set(key, value, eligibilityTtl)
                // Retorna el valor cargado
                .thenReturn(value))));

  }
// Método específico para verificar si un cliente tiene una tarjeta de crédito activa
  public Mono<Boolean> hasActiveCreditCard(String customerId,
                                           Supplier<Mono<Boolean>> loader) {
    final String key = CREDITS_HAS_CARD + ":" + customerId;
    // Intenta obtener de caché, si no está presente, usa el loader para obtener y almacenar en caché
    return booleanRedis.opsForValue().get(key)
        // Si no está en caché, llama al loader
        .switchIfEmpty(Mono.defer(() -> loader.get()
            // Almacena el valor obtenido en caché con TTL
            .flatMap(value -> booleanRedis.opsForValue().set(key, value, creditsHasCardTtl)
                // Retorna el valor cargado
                .thenReturn(value))));
  }

}
