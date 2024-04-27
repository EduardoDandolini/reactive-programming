package com.webflux.reactive.controllers;

import com.webflux.reactive.models.Payment;
import com.webflux.reactive.publishers.PaymentPublisher;
import com.webflux.reactive.repositories.InMemoryDatabase;
import com.webflux.reactive.repositories.PaymentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.awt.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentRepository repository;

    private final PaymentPublisher publisher;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Payment> createPayment(@RequestBody final NewPaymentInput input){
       final String userId = input.getUserId();
       log.info("Payment to be processed {} ", userId);
       return this.repository.create(userId)
               .flatMap(payment -> this.publisher.onPaymentCreated(payment))
               .flatMap(payment ->
                       Flux.interval(Duration.ofSeconds(1))
                               .doOnNext(it -> log.info("Next tick - {}", it))
                               .flatMap(it -> this.repository.getPayment(userId))
                               .filter(it -> Payment.PayementStatus.APPROVED == it.getStatus())
                               .next())
               .doOnNext(payment -> log.info("Payment processed {}", userId))
               .timeout(Duration.ofSeconds(20))
               .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                       .doAfterRetry(sing -> log.info("Error... retryng {}", sing.totalRetries())));

    }

    @GetMapping(value = "/users")
    public Flux<Payment> findAllById(@RequestParam String ids) {
        final List<String> _ids = Arrays.asList(ids.split(","));
        log.info("Collecting {} payments", _ids.size());
        return Flux.fromIterable(_ids)
                .flatMap(id -> this.repository.getPayment(id));
    }

    @GetMapping(value = "/ids")
    public Mono<String> getIds(){
        return Mono.fromCallable(() -> {
           return String.join(",", InMemoryDatabase.DATABASE.keySet());
        })
                .subscribeOn(Schedulers.parallel());
    }

    @Data
    public static class NewPaymentInput {
        private String userId;
        }
}
