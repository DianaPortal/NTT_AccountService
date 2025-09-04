package com.nttdata.accountservice.repository;

import com.nttdata.accountservice.model.entity.*;
import org.springframework.data.mongodb.repository.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;


@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
  Flux<Account> findByHolderDocument(String holderDocument);

}
