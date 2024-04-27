package com.webflux.reactive.publishers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webflux.reactive.models.Payment;
import com.webflux.reactive.models.PubSubMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class PaymentPublisher {

    private final Sinks.Many<PubSubMessage> sink;
    private final ObjectMapper mapper;

    public Mono<Payment> onPaymentCreated(final Payment payment){
        return Mono.fromCallable(() -> {
            final String userId = payment.getUserId();
            final String data = mapper.writeValueAsString(payment);
            return new PubSubMessage(userId, data);
        })
                .subscribeOn(Schedulers.parallel())
                .doOnNext(next -> this.sink.tryEmitNext(next))
                .thenReturn(payment);
    }
}
