package com.nttdata.accountservice.cache;

/**
 * Claves de caché utilizadas en la aplicación.
 */
public class CacheKeys {

  public static final String ELIGIBILITY = "eligibility::";
  public static final String CREDITS_HAS_CARD = "credits::hasActiveCard::";
  private CacheKeys() {
    // Constructor privado para evitar instanciación
  }
}
