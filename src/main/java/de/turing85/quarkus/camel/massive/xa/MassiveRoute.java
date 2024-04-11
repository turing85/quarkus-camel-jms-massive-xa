package de.turing85.quarkus.camel.massive.xa;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import io.agroal.api.AgroalDataSource;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.scheduler;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.sql;

@ApplicationScoped
public class MassiveRoute extends RouteBuilder {
  public static final String PROPERTY_COUNT = "count";
  public static final String PROPERTY_LOOP_COUNT = "loopCount";

  private final AgroalDataSource dataSource;

  public MassiveRoute(
      @SuppressWarnings("CdiInjectionPointsInspection") AgroalDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void configure() {
    // @formatter:off
    from(
        scheduler("write-to-database")
            .initialDelay(Duration.ofSeconds(5).toMillis())
            .repeatCount(1))
        .transacted()
        .setProperty(PROPERTY_LOOP_COUNT, constant(1000))
        .log("Starting to insert ${exchangeProperty.count} entries")
        .setProperty(PROPERTY_COUNT, constant(0))
        .loop(exchangeProperty(PROPERTY_LOOP_COUNT))
            .process(exchange ->
                exchange.setProperty(PROPERTY_COUNT, exchange.getProperty(PROPERTY_COUNT, int.class) + 1))
            .log("Inserting entry #${exchangeProperty.count}")
            .to(sql("INSERT INTO data(id) VALUES(:#${exchangeProperty.count})")
                .dataSource(dataSource))
        .end()
        .log("${exchangeProperty.count} entries inserted")
        .removeProperty(PROPERTY_LOOP_COUNT)
        .removeProperty(PROPERTY_COUNT);
    // @formatter:on
  }
}
