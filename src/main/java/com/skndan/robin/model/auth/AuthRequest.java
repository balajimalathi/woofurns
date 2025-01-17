package com.skndan.robin.model.auth;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AuthRequest {
  private String redirectUrl;
  private String idpHint;
  private String email;
  private String password;
  private String refreshToken;
  private String authorizationCode;
}