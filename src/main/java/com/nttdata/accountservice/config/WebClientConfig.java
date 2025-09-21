package com.nttdata.accountservice.config;


import io.netty.channel.*;
import io.netty.handler.timeout.*;
import lombok.extern.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.http.client.reactive.*;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.*;
import reactor.netty.http.client.*;

import java.time.*;

import static reactor.core.publisher.Mono.*;

@Configuration
@Slf4j
public class WebClientConfig {
  @Bean
  //@LoadBalanced
  public WebClient.Builder webClientBuilder() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(2))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(2))
            .addHandlerLast(new WriteTimeoutHandler(2)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .filter(new ServerBearerExchangeFilterFunction())
        .filter(logRequest())
        .filter(logResponse());
  }


  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(req -> {
      log.debug("WebClient Request: {} {}", req.method(), req.url());
      req.headers().forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, value)));
      return just(req);
    });
  }

  private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(res -> {
      log.debug("WebClient Response: status={}", res.statusCode());
      res.headers().asHttpHeaders()
          .forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, value)));
      return just(res);
    });
  }
}
