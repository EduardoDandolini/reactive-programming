package com.webflux.reactive.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PubSubMessage {

    String key;
    String value;
}
