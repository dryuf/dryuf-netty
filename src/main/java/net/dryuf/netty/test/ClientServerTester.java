package net.dryuf.netty.test;

import com.google.common.base.Stopwatch;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DuplexChannel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.core.NettyEngine;
import net.dryuf.netty.core.Server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


/**
 * Handy test wrapper for integration testing of client-server.
 *
 * See {@link net.dryuf.netty.echo.EchoEndTester} for an example.
 */
@Log4j2
public class ClientServerTester implements AutoCloseable
{
	public static final long RUN_LENGTH = 2000;

	@Getter
	@Accessors(fluent = true)
	private final NettyEngine nettyEngine;

	private final List<Server> servers = new ArrayList<>();

	/**
	 * Creates the tester.
	 */
	public ClientServerTester()
	{
		this(new NettyEngine());
	}

	public ClientServerTester(NettyEngine nettyEngine)
	{
		this.nettyEngine = nettyEngine;
	}

	/**
	 * Adds a server.
	 *
	 * @param server
	 * 	server to be added.
	 */
	public void addServer(Server server)
	{
		servers.add(server);
	}

	/**
	 * Runs client loop.
	 *
	 * @param config
	 * 	testing configuration
	 * @param runClient
	 * 	client implementation
	 *
	 * @return
	 * 	number of requests processed per second.
	 */
	public double runClientLoop(TestConfig config, Function<NettyEngine, CompletableFuture<Void>> runClient)
	{
		long started = System.currentTimeMillis();
		Stopwatch stopwatch = Stopwatch.createStarted();
		AtomicInteger counter = new AtomicInteger(0);
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (int i = 0; i < Runtime.getRuntime().availableProcessors()*2*1+1; ++i) {
			Function<Void, CompletableFuture<Void>> code = new Function<Void, CompletableFuture<Void>>()
			{
				@Override
				public CompletableFuture<Void> apply(Void v)
				{
					if (System.currentTimeMillis()-started >= config.runPeriod()) {
						return CompletableFuture.completedFuture(null);
					}
					else {
						counter.incrementAndGet();
						return runClient.apply(nettyEngine)
							.thenComposeAsync(this);
					}
				}
			};
			CompletableFuture<Void> future = CompletableFuture.completedFuture((Void) null)
				.thenComposeAsync(code);
			futures.add(future);
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		double performance = counter.get() * 1_000_000_000.0 * config.batchSize() / stopwatch.elapsed(TimeUnit.NANOSECONDS);
		log.info("Performance: time={} count={} ops/s={}", stopwatch.toString(), counter.get() * config.batchSize(), performance);
		return performance;
	}

	/**
	 * Runs Netty client loop.
	 *
	 * @param config
	 * 	test configuration
	 * @param connectAddress
	 * 	address to connect
	 * @param clientInitializer
	 * 	client channel initializer
	 * @param runner
	 * 	loop runner
	 * @return
	 * 	number of requests processed per second
	 *
	 * @param <T>
	 *      type of channel
	 */
	public <T extends DuplexChannel> double runNettyClientLoop(
		TestConfig config,
		SocketAddress connectAddress,
		Function<CompletableFuture<Void>, ChannelInitializer<T>> clientInitializer,
		Function<DuplexChannel, ? extends CompletionStage<Void>> runner
	)
	{
		return runClientLoop(
			config,
			(runtime) -> new CompletableFuture<Void>()
			{
				{
					nettyEngine.connect("tcp4",
							connectAddress,
							clientInitializer.apply(this)
						)
						.thenCompose(runner);
				}
			}
		);
	}

	@Override
	public void close()
	{
		servers.forEach(s -> s.close());
	}

	/**
	 * Test configuration.
	 */
	@Builder
	@Value
	@Accessors(fluent = true)
	public static class TestConfig
	{
		public static TestConfig DEFAULT = TestConfig.builder().build();

		/** Number of items processed at a time. */
		@Builder.Default
		int batchSize = 1;

		/** Time to run the test in milliseconds. */
		@Builder.Default
		long runPeriod = RUN_LENGTH;
	}
}
