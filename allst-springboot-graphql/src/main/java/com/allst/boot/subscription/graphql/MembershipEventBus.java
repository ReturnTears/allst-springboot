package com.allst.boot.subscription.graphql;

import com.allst.boot.subscription.model.Membership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class MembershipEventBus {
    private static final Logger log = LoggerFactory.getLogger(MembershipEventBus.class);
    private final Sinks.Many<Membership> sink = Sinks.many().multicast().onBackpressureBuffer(64, false);

    public Flux<Membership> flux() {
        return sink.asFlux();
    }

    public void publish(Membership membership) {
        Sinks.EmitResult result = sink.tryEmitNext(membership);
        if (result.isFailure()) {
            log.debug("订阅事件未投递 id={} result={}", membership.getId(), result);
        }
    }
}
