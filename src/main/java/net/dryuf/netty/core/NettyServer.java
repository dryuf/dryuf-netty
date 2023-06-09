package net.dryuf.netty.core;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import net.dryuf.netty.util.NettyFutures;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
public class NettyServer implements Server
{
	private final Channel channel;

	@Override
	public CompletableFuture<Void> closedFuture()
	{
		return NettyFutures.toCompletable(channel.closeFuture());
	}

	public SocketAddress listenAddress()
	{
		return channel.localAddress();
	}

	@Override
	public CompletableFuture<Void> cancel()
	{
		return NettyFutures.toCompletable(channel.close());
	}

	@Override
	public void close()
	{
		channel.close().syncUninterruptibly();
	}
}
