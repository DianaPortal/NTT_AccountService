package com.nttdata.account_service.repository;

import com.nttdata.account_service.model.entity.Account;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {

}
