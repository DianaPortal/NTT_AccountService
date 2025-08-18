package com.nttdata.account_service.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;


import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .description("API para gestionar cuentas bancarias")
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Servidor local")
                ))
                .tags(List.of(
                        new Tag().name("Accounts").description("Operaciones relacionadas a cuentas bancarias")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación del proyecto"));
    }
}
