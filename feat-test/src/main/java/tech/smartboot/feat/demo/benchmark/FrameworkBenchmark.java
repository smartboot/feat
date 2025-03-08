package tech.smartboot.feat.demo.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 10)
@Fork(1)
public class FrameworkBenchmark {
    private HttpClient featHelloRequest;
    private HttpClient featJsonRequest;
    private HttpClient springBootHelloRequest;
    private HttpClient springBootJsonRequest;
    private HttpClient vertxHelloRequest;
    private HttpClient vertxJsonRequest;

    @Setup
    public void setup() {

        // Spring Boot requests
//        springBootHelloRequest = Feat.httpClient("http://localhost:8080/hello", opt -> {
//        }).get();
//
//        springBootJsonRequest = Feat.httpClient("http://localhost:8080/json", opt -> {
//        }).get();
//
        // Vert.x requests
        vertxHelloRequest = Feat.httpClient("http://localhost:8081/hello", opt -> {
        });
        vertxJsonRequest = Feat.httpClient("http://localhost:8081/json", opt -> {
        });

        // Feat requests
        featHelloRequest = Feat.httpClient("http://localhost:8082/hello", opt -> {
//            opt.debug(true);
        });
        featJsonRequest = Feat.httpClient("http://localhost:8082/json", opt -> {
//            opt.debug(true);
        });
    }

//    @Benchmark
//    public String springBootHello() throws Exception {
//        return springBootHelloRequest.submit().get().body();
//    }
//
//    @Benchmark
//    public String springBootJson() throws Exception {
//        return springBootJsonRequest.submit().get().body();
//    }
//
    @Benchmark
    public String vertxHello() throws Exception {
        return vertxHelloRequest.get("/hello").header(header -> header.keepalive(true)).submit().get().body();
    }

    @Benchmark
    public String vertxJson() throws Exception {
        return vertxJsonRequest.get("/json").header(header -> header.keepalive(true)).submit().get().body();
    }

//    @Benchmark
//    public String featHello() throws Exception {
//        return featHelloRequest.get("/hello").header(header -> header.keepalive(true)).submit().get().body();
//    }
//
//    @Benchmark
//    public String featJson() throws Exception {
//        return featJsonRequest.get("/json").header(header -> header.keepalive(true)).submit().get().body();
//    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FrameworkBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
} 