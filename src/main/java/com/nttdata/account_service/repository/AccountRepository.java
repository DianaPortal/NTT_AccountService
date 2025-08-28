package com.nttdata.account_service.repository;

import com.nttdata.account_service.model.entity.Account;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
    Flux<Account> findByHolderDocument(String holderDocument);

}
