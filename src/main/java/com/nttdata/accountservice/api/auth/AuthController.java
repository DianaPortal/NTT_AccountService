package com.nttdata.accountservice.api.auth;

import com.nttdata.accountservice.model.auth.*;
import com.nttdata.accountservice.security.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final ReactiveAuthenticationManager loginAuthenticationManager;
  private final MapReactiveUserDetailsService uds;
  private final JwtService jwtService;

  public AuthController(
      @Qualifier("loginAuthenticationManager")
      ReactiveAuthenticationManager loginAuthenticationManager,
      MapReactiveUserDetailsService uds,
      JwtService jwtService
  ) {
    this.loginAuthenticationManager = loginAuthenticationManager;
    this.uds = uds;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<LoginResponse>> login(
      @RequestBody LoginRequest req) {

    UsernamePasswordAuthenticationToken authReq =
        new UsernamePasswordAuthenticationToken(
            req.getUsername(), req.getPassword());

    return loginAuthenticationManager.authenticate(authReq)
        .map(Authentication::getPrincipal)
        .cast(UserDetails.class)
        .map(u -> new LoginResponse()
            .setTokenType("Bearer")
            .setToken(jwtService.generateToken(u)))
        .map(ResponseEntity::ok);
  }
}