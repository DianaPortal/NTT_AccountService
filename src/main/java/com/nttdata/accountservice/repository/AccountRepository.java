package com.nttdata.accountservice.repository;

import org.springframework.data.mongodb.repository.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;


@Repository
public interface AccountRepository extends ReactiveMongoRepository<com.nttdata.accountservice.model.entity.Account, String> {
  Flux<com.nttdata.accountservice.model.entity.Account> findByHolderDocument(String holderDocument);

}
