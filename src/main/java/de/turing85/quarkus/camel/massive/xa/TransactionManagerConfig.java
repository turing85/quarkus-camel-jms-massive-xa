package de.turing85.quarkus.camel.massive.xa;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import io.smallrye.common.annotation.Identifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

public class TransactionManagerConfig {
  public static final String GLOBAL_PLATFORM_TRANSACTION_MANAGER_NAME =
      "globalPlatformTransactionManager";

  @Produces
  @ApplicationScoped
  @Identifier(GLOBAL_PLATFORM_TRANSACTION_MANAGER_NAME)
  public PlatformTransactionManager globalPlatformTransactionManager(
      UserTransaction userTransaction,
      @SuppressWarnings("CdiInjectionPointsInspection") TransactionManager transactionManager) {
    return new JtaTransactionManager(userTransaction, transactionManager);
  }
}
