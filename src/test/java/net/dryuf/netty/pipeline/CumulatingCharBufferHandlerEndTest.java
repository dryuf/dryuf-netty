package net.dryuf.netty.pipeline;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DuplexChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.log4j.Log4j2;
import net.dryuf.concurrent.FutureUtil;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyServer;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.test.ClientServerTester;
import net.dryuf.netty.test.pipeline.ExpectDataHandler;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.testng.Assert.expectThrows;


@Log4j2
public class CumulatingCharBufferHandlerEndTest
{
	@Test(timeOut = 10_000L)
	public void cumulation_interrupted_cumulated() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			CompletableFuture<Object> serverFinished = new CompletableFuture<>();
			InetSocketAddress serverAddress = runServer(tester, "Hello\n", serverFinished);

			CompletableFuture<Object> clientFinished = new CompletableFuture<>();
			DuplexChannel channel = connectClient(tester, serverAddress, "Hello\n", clientFinished);
			channel.writeAndFlush(Unpooled.wrappedBuffer("Hello".getBytes(StandardCharsets.UTF_8))).get();
			Thread.sleep(100);
			channel.writeAndFlush(Unpooled.wrappedBuffer("\n".getBytes(StandardCharsets.UTF_8))).get();

			serverFinished.join();
			clientFinished.join();
		}
	}

	@Test(timeOut = 10_000L)
	public void cumulation_bufferBorderUtf8_cumulated() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			CompletableFuture<Object> serverFinished = new CompletableFuture<>();
			InetSocketAddress serverAddress = runServer(tester, "Hel❤️\n", serverFinished);

			CompletableFuture<Object> clientFinished = new CompletableFuture<>();
			DuplexChannel channel = connectClient(tester, serverAddress, "Hel❤️\n", clientFinished);
			channel.writeAndFlush(Unpooled.wrappedBuffer("Hel❤️\n".getBytes(StandardCharsets.UTF_8))).get();

			serverFinished.join();
			clientFinished.join();
		}
	}

	@Test(timeOut = 10_000L)
	public void cumulation_interruptedUtf8_cumulated() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			CompletableFuture<Object> serverFinished = new CompletableFuture<>();
			InetSocketAddress serverAddress = runServer(tester, "Hello❤️\n", serverFinished);

			CompletableFuture<Object> clientFinished = new CompletableFuture<>();
			DuplexChannel channel = connectClient(tester, serverAddress, "Hello❤️\n", clientFinished);
			channel.writeAndFlush(Unpooled.wrappedBuffer("Hello❤️\n".getBytes(StandardCharsets.UTF_8), 0, 6)).get();
			Thread.sleep(100);
			channel.writeAndFlush(Unpooled.wrappedBuffer("Hello❤️\n".getBytes(StandardCharsets.UTF_8), 6, "Hello❤️\n".getBytes(StandardCharsets.UTF_8).length-6)).get();

			serverFinished.join();
			clientFinished.join();
		}
	}

	@Test(timeOut = 10_000L)
	public void cumulation_overflow_exception() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			CompletableFuture<Object> serverFinished = new CompletableFuture<>();
			InetSocketAddress serverAddress = runServer(tester, "Ha", serverFinished);

			CompletableFuture<Object> clientFinished = new CompletableFuture<>();
			DuplexChannel channel = connectClient(tester, serverAddress, "Ha", clientFinished);
			channel.writeAndFlush(Unpooled.wrappedBuffer(StringUtils.repeat("H", 1_000_001).getBytes(StandardCharsets.UTF_8))).get();

			expectThrows(IllegalStateException.class, () -> FutureUtil.sneakyGet(serverFinished));
		}
	}

	public static InetSocketAddress runServer(ClientServerTester tester, String expected, CompletableFuture<Object> finished)
	{
		return runServer(tester, InetSocketAddress.createUnresolved("localhost", 0), expected, finished);
	}

	public static <T extends SocketAddress> T runServer(
		ClientServerTester tester,
		T listenAddress,
		String expected,
		CompletableFuture<Object> finished
	)
	{
		Server server = new NettyServer(
			tester.nettyEngine().listen(
				AddressSpec.fromSocketAddress(listenAddress),
				new ChannelInitializer<DuplexChannel>()
				{
					@Override
					protected void initChannel(DuplexChannel ch) throws Exception
					{
						ch.pipeline().addLast(
							new CumulatingCharBufferHandler(1_000_000),
							new StringEncoder(),
							new ExpectDataHandler(finished, CharBuffer.wrap(expected))
						);
					}
				}
			).join());
		tester.addServer(server);
		@SuppressWarnings("unchecked")
		T address = (T) server.listenAddress();
		log.info("Server listening: {}", address);
		return address;
	}

	public static DuplexChannel connectClient(
		ClientServerTester tester,
		SocketAddress serverAddress,
		String expected,
		CompletableFuture<Object> finished
	)
	{
		DuplexChannel channel = tester.nettyEngine().connect(
			AddressSpec.fromSocketAddress(serverAddress),
			new ChannelInitializer<DuplexChannel>()
			{
				@Override
				protected void initChannel(DuplexChannel ch) throws Exception
				{
					ch.pipeline().addLast(
						new CumulatingCharBufferHandler(1_000_000),
						new ExpectDataHandler(finished, CharBuffer.wrap(expected))
					);
				}
			}
		).join();
		return channel;
	}
}
