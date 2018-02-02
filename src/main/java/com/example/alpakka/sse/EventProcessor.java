package com.example.alpakka.sse;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.http.javadsl.model.Uri;
import akka.stream.*;
import akka.stream.javadsl.*;
import akka.stream.alpakka.sse.javadsl.*;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.sse.ServerSentEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.function.Function;

import akka.util.ByteString;
import scala.concurrent.duration.FiniteDuration;

public class EventProcessor {

  // Actor system and materializer
  private static ActorSystem system = ActorSystem.create();
  private static Materializer materializer = ActorMaterializer.create(system);

  // we configure logging, to signal elements and completion on INFO and stream errors on WARN levels
  private static Attributes streamLoggingAttributes =
      ActorAttributes.logLevels(Logging.InfoLevel(), Logging.InfoLevel(), Logging.WarningLevel());

  private static final Http http = Http.get(system);

  private static final int nrOfSamples = 100;

  // Logger and configuration
  private static final Logger LOGGER = Logger.getLogger("EventProcessor");

  /**
   * Akka Alpakka SSE example
   */
  public static void main(String[] args) throws Exception {
    LOGGER.info("Init");

    Uri targetUri = Uri.create("http://emojitrack-gostreamer.herokuapp.com/subscribe/eps");
    Source<ServerSentEvent, NotUsed> eventSource = eventsSource(targetUri);

    LOGGER.info("run");

    int elements = 100;
    FiniteDuration per = FiniteDuration.create(10, TimeUnit.SECONDS);
    int maximumBurst = 100;

    Source<String, NotUsed> jsonObjects = eventSource
        // we only want to consume `nrOfSamples` samples:
        .take(nrOfSamples)
        .map(ServerSentEvent::getData);

    // EXTRA: IF we'd be getting an unstructured stream of data, where we're not sure if each data chunk contains
    // the entire JSON object (but could contain part of it), we could use JsonFraming to get valid objects,
    // regardless of how they were sent to us. We don't need to do this here since we're using Server Sent Events,
    // which already guarantee proper framing of the messages;
//        .map(event -> ByteString.fromString(event.getData()))
//        .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
//        .map(ByteString::utf8String)

    CompletionStage<List<String>> collectedJsons = jsonObjects
        // we throttle the processing a bit, so it's nicer to look at the console printout:
        .throttle(elements, per, maximumBurst, ThrottleMode.shaping())
        // log elements:
        .log("objects").withAttributes(streamLoggingAttributes)
        // we run the stream; this is where it starts the requests and processing of the data;
        // we can get all elements into a sequence (a List), since we know it's of finite size (due to the take())
        .runWith(Sink.seq(), materializer);

    try {
      // since the stream above is running asynchronously, if we want to get a strict value out of it
      // we either block (as we do in the example below, using get() with a timeout), or we would use map()
      // and other operators on CompletableFuture
      collectedJsons
          .toCompletableFuture()
          .get(10, TimeUnit.SECONDS);
    } finally {
      // finally, we terminate the system, causing the app to exit
      // we do so in a finally block, because if the stream failed, its failure would be passed through
      // into the CompletionStage it materialized, which would make the above get() call throw as well.
      system.terminate();
    }

  }

  private static Source<ServerSentEvent, NotUsed> eventsSource(Uri targetUri) {
    final Function<HttpRequest, CompletionStage<HttpResponse>> send =
        (request) -> http.singleRequest(request, materializer);

    final Optional<String> lastEventId = Optional.of("10");

    return EventSource.create(targetUri, send, lastEventId, materializer);
  }

}
