package de.turing85.quarkus.camel.massive.xa;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import io.quarkus.runtime.StartupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SendMessages {
  private static final int MESSAGES_TO_SEND = 1_000;

  private final XAConnectionFactory xaConnectionFactory;
  private final TransactionManager transactionManager;

  public void send(@Observes StartupEvent ignored) throws HeuristicMixedException,
      HeuristicRollbackException, NotSupportedException, RollbackException, SystemException {
    transactionManager.begin();
    log.info("Starting to send messages");
    Transaction transaction = transactionManager.getTransaction();
    for (int message = 1; message <= MESSAGES_TO_SEND; ++message) {
      log.info("Sending message #{}", message);
      XAJMSContext context = xaConnectionFactory.createXAContext();
      transaction.registerSynchronization(new Synchronization() {
        @Override
        public void beforeCompletion() {
          // noop
        }

        @Override
        public void afterCompletion(int i) {
          context.close();
        }
      });
      transaction.enlistResource(context.getXAResource());
      context.createProducer().send(context.createQueue("send-message"),
          "Hello %d".formatted(message));
    }
    log.info("{} messages sent", MESSAGES_TO_SEND);
    transactionManager.commit();
  }
}
