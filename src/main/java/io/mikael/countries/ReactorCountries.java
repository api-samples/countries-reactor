package io.mikael.countries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import reactor.Environment;
import reactor.io.buffer.Buffer;
import reactor.io.net.NetStreams;
import reactor.io.net.http.HttpServer;
import reactor.io.net.http.model.Status;
import reactor.rx.Promise;
import reactor.rx.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;

public class ReactorCountries {

    private final static ConcurrentMap<String, Map> COUNTRIES;

    private final static ObjectMapper JSON;

    volatile static HttpServer<Buffer, Buffer> SERVER;

    volatile static Promise<Void> START;

    static {
        JSON = new ObjectMapper();
        JSON.enable(SerializationFeature.INDENT_OUTPUT);
        try (final InputStream is = ReactorCountries.class.getClassLoader().getResourceAsStream("countries.json")) {
            final List<Map> input = JSON.readValue(is, new TypeReference<List<Map>>() { });
            COUNTRIES = input.stream().collect(toMap(c -> c.get("cca2").toString(), c -> c,
                    (a, b) -> a, ConcurrentHashMap::new));
        } catch (final IOException e) {
            throw new RuntimeException("unable to parse countries", e);
        }
    }

    public static void main(final String ... args) {
        Environment.initializeIfEmpty().assignErrorJournal();
        SERVER = NetStreams.httpServer(
                s -> s.listen(8080).dispatcher(Environment.sharedDispatcher()));
        SERVER.get("/countries/{cca2}", channel -> {
            final String cca2 = channel.params().get("cca2");
            if (COUNTRIES.containsKey(cca2)) {
                try {
                    channel.header("Content-Type", "text/json");
                    return channel.writeWith(Streams.just(
                            Buffer.wrap(JSON.writeValueAsString(COUNTRIES.get(cca2)), false)));
                } catch (JsonProcessingException e) {
                    return null;
                }
            } else {
                channel.responseStatus(Status.NOT_FOUND);
                channel.header("Content-Type", "text/json");
                return channel.writeWith(Streams.just(Buffer.wrap("{}", false)));
            }
        });
        START = SERVER.start();
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

}
