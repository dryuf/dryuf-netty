package net.dryuf.netty.provider;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;

import java.net.SocketAddress;


public interface ChannelProvider
{
	default EventLoopGroup createBossEventLoopGroup()
	{
		return createEventLoopGroup(1);
	}

	default EventLoopGroup createWorkerEventLoopGroup()
	{
		return createEventLoopGroup(Runtime.getRuntime().availableProcessors());
	}

	EventLoopGroup createEventLoopGroup(int threads);

	default SocketAddress convertAddress(SocketAddress original)
	{
		return original;
	}

	default ChannelFactory<? extends ServerChannel> getServerChannel(SocketAddress address)
	{
		return getServerChannel(null, address);
	}

	ChannelFactory<? extends ServerChannel> getServerChannel(String proto, SocketAddress address);

	default ChannelFactory<? extends DuplexChannel> getStreamChannel(SocketAddress address)
	{
		return getStreamChannel(null, address);
	}

	ChannelFactory<? extends DuplexChannel> getStreamChannel(String proto, SocketAddress address);

	default ChannelFactory<? extends DatagramChannel> getDatagramChannel(SocketAddress address)
	{
		return getDatagramChannel(null, address);
	}

	ChannelFactory<? extends DatagramChannel> getDatagramChannel(String proto, SocketAddress address);
}
