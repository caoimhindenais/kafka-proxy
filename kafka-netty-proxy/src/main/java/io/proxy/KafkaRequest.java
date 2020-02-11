package io.proxy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KafkaRequest {

    short key;
    short version;
}
