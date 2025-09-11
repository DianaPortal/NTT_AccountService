package com.nttdata.accountservice.cache;

import com.nttdata.accountservice.integration.customers.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.*;
import reactor.core.publisher.*;
import reactor.test.*;

import java.time.*;

import static com.nttdata.accountservice.cache.CacheKeys.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CatalogCacheService.
 */
@ExtendWith(MockitoExtension.class)
class CatalogCacheServiceTest {

  @Mock
  private ReactiveRedisTemplate<String, EligibilityResponse> eligibilityRedis;

  @Mock
  private ReactiveRedisTemplate<String, Boolean> booleanRedis;

  @Mock
  private ReactiveValueOperations<String, EligibilityResponse> eligibilityOps;

  @Mock
  private ReactiveValueOperations<String, Boolean> booleanOps;

  private CatalogCacheService service;

  @BeforeEach
  void setUp() {
    lenient().when(eligibilityRedis.opsForValue()).thenReturn(eligibilityOps);
    lenient().when(booleanRedis.opsForValue()).thenReturn(booleanOps);
    service = new CatalogCacheService(eligibilityRedis, booleanRedis);
    ReflectionTestUtils.setField(service, "eligibilityTtl", Duration.ofMinutes(10));
    ReflectionTestUtils.setField(service, "creditsHasCardTtl", Duration.ofMinutes(5));
  }

  @Test
  void eligibility_cacheMiss_thenStores() {
    String key = ELIGIBILITY + ":DNI:123";
    EligibilityResponse value = new EligibilityResponse();
    value.setCustomerId("C1");

    when(eligibilityOps.get((key))).thenReturn(Mono.empty());
    when(eligibilityOps.set((key), (value), (Duration.ofMinutes(10))))
        .thenReturn(Mono.just(true));

    StepVerifier.create(service.getEligibility("DNI", "123", () -> Mono.just(value)))
        .expectNextMatches(v -> "C1".equals(v.getCustomerId()))
        .verifyComplete();

    verify(eligibilityOps).get((key));
    verify(eligibilityOps).set((key), (value), (Duration.ofMinutes(10)));
  }

  @Test
  void eligibility_cacheHit_doesNotCallLoader() {
    String key = ELIGIBILITY + ":DNI:987";
    EligibilityResponse cached = new EligibilityResponse();
    cached.setCustomerId("C2");

    when(eligibilityOps.get((key))).thenReturn(Mono.just(cached));

    StepVerifier.create(service.getEligibility("DNI", "987", () -> Mono.error(new RuntimeException("no"))))
        .expectNextMatches(v -> "C2".equals(v.getCustomerId()))
        .verifyComplete();

    verify(eligibilityOps).get((key));
  }

  @Test
  void hasActiveCreditCard_cacheMiss_thenStores() {
    String key = CREDITS_HAS_CARD + ":C55";

    when(booleanOps.get((key))).thenReturn(Mono.empty());
    when(booleanOps.set((key), (true), (Duration.ofMinutes(5))))
        .thenReturn(Mono.just(true));

    StepVerifier.create(service.hasActiveCreditCard("C55", () -> Mono.just(true)))
        .expectNext(true)
        .verifyComplete();

    verify(booleanOps).get((key));
    verify(booleanOps).set((key), (true), (Duration.ofMinutes(5)));
  }

  @Test
  void hasActiveCreditCard_cacheHit() {
    String key = CREDITS_HAS_CARD + ":C99";

    when(booleanOps.get((key))).thenReturn(Mono.just(false));

    StepVerifier.create(service.hasActiveCreditCard("C99", () -> Mono.just(true)))
        .expectNext(false)
        .verifyComplete();

    verify(booleanOps).get((key));
  }
}