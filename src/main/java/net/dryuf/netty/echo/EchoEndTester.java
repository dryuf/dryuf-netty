package net.dryuf.netty.echo;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.DuplexChannelConfig;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyServer;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.pipeline.FullFlowControlHandler;
import net.dryuf.netty.test.ClientServerTester;
import net.dryuf.netty.util.NettyFutures;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


/**
 * Example of Echo tester.
 *
 * TODO: Should be nicer without static functions, original comes from unit test.
 */
@Log4j2
public class EchoEndTester
{
	private static final String output = StringUtils.repeat("Hello world\n", 1000);

	public static void testEcho() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = runEchoServer(tester);

			try (ClientServerTester tester2 = new ClientServerTester()) {
				runEchoClient(tester2, serverAddress, 1);
			}
		}
	}

	public static InetSocketAddress runEchoServer(ClientServerTester tester)
	{
		return runEchoServer(tester, InetSocketAddress.createUnresolved("localhost", 0));
	}

	public static <T extends SocketAddress> T runEchoServer(ClientServerTester tester, T listenAddress)
	{
		AtomicInteger serverCount = new AtomicInteger();
		Server server = new NettyServer(
			tester.nettyEngine().listen(
				AddressSpec.fromSocketAddress(listenAddress),
				new ChannelInitializer<DuplexChannel>()
				{
					@Override
					protected void initChannel(DuplexChannel ch) throws Exception
					{
						ch.pipeline().addLast(
							new EchoServerHandler(ch, serverCount)
						);
					}
				}
			).join());
		tester.addServer(server);
		@SuppressWarnings("unchecked")
		T address = (T) server.listenAddress();
		log.info("EchoServer listening: {}", address);
		return address;
	}

	public static double runEchoClient(ClientServerTester tester, SocketAddress serverAddress, int attempts)
	{
		AtomicInteger pending = new AtomicInteger();
		return tester.runNettyClientLoop(
			ClientServerTester.TestConfig.builder()
				.batchSize(1000)
				.build(),
			serverAddress,
			(future) -> new ChannelInitializer<DuplexChannel>()
			{
				@Override
				protected void initChannel(DuplexChannel ch) throws Exception
				{
					ch.pipeline().addLast(
						new StringDecoder(),
						new FullFlowControlHandler(),
						new EchoClientHandler(future, ch, pending)
					);
				}
			},
			channel -> {
				//log.info("Connected, running client: local={}", channel.localAddress());
				AtomicInteger counter = new AtomicInteger(1000);
				return CompletableFuture.completedFuture((Void) null)
					.thenComposeAsync(
						new Function<Void, CompletableFuture<Void>>()
						{
							@Override
							public CompletableFuture<Void> apply(Void arg)
							{
								if (counter.decrementAndGet() < 0)
									return NettyFutures.toCompletable(channel.shutdownOutput());
								return NettyFutures.toCompletable(channel.writeAndFlush(Unpooled.wrappedBuffer("Hello world\n".getBytes(StandardCharsets.UTF_8))))
									.thenComposeAsync(this::apply);
							}
						}
					);
			}
		);
	}

	@Log4j2
	public static class EchoServerHandler extends ChannelInboundHandlerAdapter
	{
		private final AtomicInteger serverCount;

		public EchoServerHandler(DuplexChannel channel, AtomicInteger serverCount)
		{
			this.serverCount = serverCount;
			((DuplexChannelConfig) channel.config()).setAutoRead(false);
			((DuplexChannelConfig) channel.config()).setAllowHalfClosure(true);
			channel.closeFuture().addListener(f -> {
				log.debug("Pending server: {}", serverCount.decrementAndGet());
			});
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx)
		{
			serverCount.incrementAndGet();
			ctx.read();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
		{
			ctx.writeAndFlush(msg);
			ctx.read();
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof ChannelInputShutdownEvent) {
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener((ChannelFuture f) -> {
					ChannelFuture newFuture = f;
					if (f.isSuccess()) {
						newFuture = ((DuplexChannel) ctx.channel()).shutdownOutput();
					}
					newFuture.addListener(ChannelFutureListener.CLOSE);
				});
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			log.error("Exception in server:", cause);
			ctx.close();
		}
	}

	@Log4j2
	public static class EchoClientHandler extends ChannelInboundHandlerAdapter
	{
		StringBuilder sb = new StringBuilder();

		private final CompletableFuture<Void> closedPromise;

		AtomicInteger pending;

		public EchoClientHandler(CompletableFuture<Void> closedPromise, DuplexChannel channel, AtomicInteger pending)
		{
			this.closedPromise = closedPromise;
			((DuplexChannelConfig) channel.config()).setAutoRead(false);
			((DuplexChannelConfig) channel.config()).setAllowHalfClosure(true);
			this.pending = pending;
			NettyFutures.copy(channel.closeFuture(), closedPromise);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx)
		{
			pending.incrementAndGet();
			ctx.read();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg)
		{
			sb.append((String) msg);
			ReferenceCountUtil.release(msg);
			ctx.read();
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof ChannelInputShutdownEvent) {
				try {
					if (!sb.toString().equals(output)) {
						throw new IllegalStateException("Unexpected output: expected=[" + output + "] actual=[" + sb.toString() + "]");
					}
				}
				catch (Throwable ex) {
					closedPromise.completeExceptionally(new AssertionError("Channel test failed: " + ctx.channel(), ex));
				}
				finally {
					log.info("Pending: {}", pending.decrementAndGet());
					NettyFutures.copy(ctx.close(), closedPromise);
				}
			}
			else {
				ctx.read();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
			log.error("Exception in client:", cause);
			closedPromise.completeExceptionally(cause);
			ctx.close();
		}
	}
}
