package de.turing85.quarkus.camel.massive.xa;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.ConnectionFactory;

import io.smallrye.common.annotation.Identifier;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.jms;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.scheduler;

@ApplicationScoped
public class MassiveRoute extends RouteBuilder {
  public static final String PROPERTY_COUNT = "count";
  public static final String PROPERTY_LOOP_COUNT = "loopCount";

  private final ConnectionFactory connectionFactory;
  private final PlatformTransactionManager globalTransactionManager;

  public MassiveRoute(
      @SuppressWarnings("CdiInjectionPointsInspection") ConnectionFactory connectionFactory,
      @Identifier(TransactionManagerConfig.GLOBAL_PLATFORM_TRANSACTION_MANAGER_NAME) PlatformTransactionManager globalTransactionManager) {
    this.connectionFactory = connectionFactory;
    this.globalTransactionManager = globalTransactionManager;
  }

  @Override
  public void configure() {
    // @formatter:off
    from(
        scheduler("send-jms")
            .initialDelay(Duration.ofSeconds(5).toMillis())
            .repeatCount(1))
        .transacted()
        .setProperty(PROPERTY_LOOP_COUNT, constant(1000))
        .log("Starting to send ${exchangeProperty.count} messages")
        .setProperty(PROPERTY_COUNT, constant(0))
        .loop(exchangeProperty(PROPERTY_LOOP_COUNT))
            .process(exchange ->
                exchange.setProperty(PROPERTY_COUNT, exchange.getProperty(PROPERTY_COUNT, int.class) + 1))
            .log("Sending message #${exchangeProperty.count}")
            .setBody(simple("Hello ${exchangeProperty.count}"))
            .to(jms("send-message")
                .connectionFactory(connectionFactory)
                .advanced()
                    .transactionManager(globalTransactionManager))
        .end()
        .log("${exchangeProperty.count} messages sent")
        .removeProperty(PROPERTY_LOOP_COUNT)
        .removeProperty(PROPERTY_COUNT);
    // @formatter:on
  }
}
