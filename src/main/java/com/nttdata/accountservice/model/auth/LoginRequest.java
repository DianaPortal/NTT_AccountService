package com.nttdata.accountservice.model.auth;

import lombok.*;

@Data
public class LoginRequest {
  private String username;
  private String password;
}
