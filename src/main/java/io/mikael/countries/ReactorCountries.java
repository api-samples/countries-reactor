package io.mikael.countries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.toMap;

public class ReactorCountries {

    private final static ConcurrentMap<String, Map<String, Object>> COUNTRIES;

    private final static ObjectMapper JSON;

    volatile static HttpServer SERVER;

    volatile static Mono<Void> START;

    volatile static DisposableServer DISPOSABLE;

    static {
        JSON = new ObjectMapper();
        JSON.enable(SerializationFeature.INDENT_OUTPUT);
        try (final InputStream is = ReactorCountries.class.getClassLoader().getResourceAsStream("countries.json")) {
            final var input = JSON.readValue(is, new TypeReference<List<Map<String, Object>>>() { });
            COUNTRIES = input.stream().collect(toMap(c -> c.get("cca2").toString(), c -> c,
                    (a, b) -> a, ConcurrentHashMap::new));
        } catch (final IOException e) {
            throw new RuntimeException("unable to parse countries", e);
        }
    }

    private static Publisher<Void> findCountry(final HttpServerRequest request, final HttpServerResponse response) {
        response.header("Content-Type", "text/json");
        final var cca2 = request.params().get("cca2");
        if (COUNTRIES.containsKey(cca2)) {
            try {
                response.status(200);
                return response.sendString(Mono.just(JSON.writeValueAsString(COUNTRIES.get(cca2))));
            } catch (JsonProcessingException e) {
                return null;
            }
        } else {
            response.status(404);
            return response.sendString(Mono.just("{}"));
        }
    }

    public static void main(final String ... args) {
        SERVER = HttpServer.create()
                .port(8080)
                .route(routes ->
                        routes.get("/countries/{cca2}", ReactorCountries::findCountry)
                );
        DISPOSABLE = SERVER.bindNow();
        START = DISPOSABLE.onDispose();
        START.block();
    }

}
