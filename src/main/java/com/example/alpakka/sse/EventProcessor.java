package com.example.alpakka.sse;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.model.Uri;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.stream.alpakka.sse.javadsl.*;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.sse.ServerSentEvent;
import akka.stream.ThrottleMode;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.function.Function;
import scala.concurrent.duration.FiniteDuration;

/**
 *
 */
public class EventProcessor {

    // Actor system and materializer
    private static ActorSystem system = ActorSystem.create();
    private static Materializer materializer = ActorMaterializer.create(system);

    private static final Http http = Http.get(system);
    private static final Function<HttpRequest, CompletionStage<HttpResponse>> send
            = (request) -> http.singleRequest(request, materializer);

    private static final String host = "emojitrack-gostreamer.herokuapp.com/subscribe/eps";
    private static final int port = 80;
    private static final int nrOfSamples = 10;
    private static final Uri targetUri = Uri.create(String.format("http://%s:%d", host, port));

    // Logger and configuration
    private static final Logger LOGGER = Logger.getLogger("EventProcessor");
    //  private static final Config CONFIG = ConfigFactory.load();

    /**
     * Akka Alpakka SSE example
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Init");

        final Optional<String> lastEventId = Optional.of("10");
        Source<ServerSentEvent, NotUsed> eventSource
                = EventSource.create(targetUri, send, lastEventId, materializer);

        LOGGER.info(
                "run");

        int elements = 100;
        FiniteDuration per = FiniteDuration.create(10, TimeUnit.SECONDS);
        int maximumBurst = 100;

        eventSource.map(sse -> {
            System.out.println(sse.getData());
            return sse;
                })
                .throttle(elements, per, maximumBurst, ThrottleMode.shaping())
                .take(nrOfSamples)
                .runWith(Sink.seq(), materializer);

    }

}
