package com.nttdata.accountservice.config;

import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.context.annotation.*;
import org.openapitools.jackson.nullable.JsonNullableModule;

public class JacksonConfig {
  @Bean
  Jackson2ObjectMapperBuilderCustomizer jsonNullableModule() {
    return builder -> builder.modules(new JsonNullableModule());
  }
}
