package com.webflux.reactive.repositories;

import com.webflux.reactive.models.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRepository {

    private final Database database;

    public Mono<Payment> create(final String userId){
      final Payment payment =  Payment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .status(Payment.PayementStatus.PENDING)
                .build();

      return Mono.fromCallable(() -> {
          log.info("Saving payment");
          return this.database.save(userId, payment);
      })
              .subscribeOn(Schedulers.boundedElastic())
              .doOnNext(next -> log.info("Payment received ", next.getUserId()));
    }

    public Mono<Payment> getPayment(final String userId){
        return Mono.defer(() -> {
            log.info("Getting payment from data - {}", userId);
            final Optional<Payment> payment = this.database.get(userId, Payment.class);
            return Mono.justOrEmpty(payment);
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Payment> processPayment(String key, Payment.PayementStatus status) {
        log.info("On payment {} to status {}", key, status);
        return getPayment(key)
                .flatMap(payment -> Mono.fromCallable(() -> {
                    log.info("Processing payment {} to statys", key, status);
                    return this.database.save(key, payment.withStatus(status));
                })
                        .subscribeOn(Schedulers.boundedElastic())
                );
    }
}
