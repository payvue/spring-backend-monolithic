package com.payvue.app.infrastructure.persistence.transaction;

import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionId;
import com.payvue.core.transaction.command.TransactionRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@RequiredArgsConstructor
public class JpaTransactionRepository implements TransactionRepository {

  private final TransactionJpaRepository jpaRepository;
  private final TransactionEntityMapper mapper;

  @Override
  @Transactional
  public void save(Transaction transaction) {
    jpaRepository.save(mapper.toEntity(transaction));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Transaction> findById(TransactionId id) {
    return jpaRepository.findById(id.value()).map(mapper::toDomain);
  }
}
