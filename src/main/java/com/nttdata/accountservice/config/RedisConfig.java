package com.nttdata.accountservice.config;
import com.nttdata.accountservice.integration.customers.EligibilityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuración de Redis para la serialización y deserialización de objetos.
 */
@Configuration
public class RedisConfig {

  // Valores de configuración para la conexión a Redis
  @Value("${spring.redis.host:localhost}") String host;
  @Value("${spring.redis.port:6379}") int port;



  // Configuración del template para serializar/deserializar objetos EligibilityResponse
  @Bean
  public ReactiveRedisTemplate<String, EligibilityResponse> eligibilityRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    // Configura el serializador para EligibilityResponse
    Jackson2JsonRedisSerializer<EligibilityResponse> serializer =
        new Jackson2JsonRedisSerializer<>(EligibilityResponse.class);
    // Configura el contexto de serialización
    RedisSerializationContext<String, EligibilityResponse> context =
        RedisSerializationContext.<String, EligibilityResponse>newSerializationContext(
            new StringRedisSerializer())
        .value(serializer)
        .build();
    // Crea y retorna el ReactiveRedisTemplate
    return new ReactiveRedisTemplate<>(factory, context);
  }

  // Configuración del template para serializar/deserializar objetos Boolean
  @Bean
  public ReactiveRedisTemplate<String, Boolean> booleanRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    // Configura el serializador para Boolean
    Jackson2JsonRedisSerializer<Boolean> serializer =
        new Jackson2JsonRedisSerializer<>(Boolean.class);
    // Configura el contexto de serialización
    RedisSerializationContext<String, Boolean> context =
        RedisSerializationContext.<String, Boolean>newSerializationContext(
            new StringRedisSerializer())
        .value(serializer)
        .build();
    // Crea y retorna el ReactiveRedisTemplate
    return new ReactiveRedisTemplate<>(factory, context);
  }

}
