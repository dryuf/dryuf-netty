package net.dryuf.netty.provider;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.unix.DomainSocketAddress;
import net.dryuf.netty.core.NettyEngine;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.util.concurrent.TimeUnit;


public class KqueueChannelProvider implements ChannelProvider
{
	static {
		KQueueEventLoopGroup events = new KQueueEventLoopGroup();
		events.shutdownGracefully(0, 0, TimeUnit.SECONDS);
	}

	@Override
	public EventLoopGroup createEventLoopGroup(int threads)
	{
		return new KQueueEventLoopGroup(threads);
	}

	@Override
	public SocketAddress convertAddress(SocketAddress original)
	{
		if (original instanceof UnixDomainSocketAddress) {
			return new DomainSocketAddress(((UnixDomainSocketAddress) original).getPath().toString());
		}
		else {
			return original;
		}
	}

	@Override
	public ChannelFactory<? extends ServerChannel> getServerChannel(String proto, SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return NettyEngine.isProtoNeutral(proto) ?
				KQueueServerSocketChannel::new :
				() -> new KQueueServerSocketChannel();
		}
		else if (address instanceof DomainSocketAddress || address instanceof UnixDomainSocketAddress) {
			return KQueueServerDomainSocketChannel::new;
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DuplexChannel> getStreamChannel(String proto, SocketAddress address)
	{
		if (address instanceof InetSocketAddress) {
			return () -> NettyEngine.isProtoNeutral(proto) ?
				new KQueueSocketChannel() :
				new KQueueSocketChannel(NettyEngine.getNettyProtocolByAddress(((InetSocketAddress)address).getAddress()));
		}
		else if (address instanceof DomainSocketAddress || address instanceof UnixDomainSocketAddress) {
			return KQueueDomainSocketChannel::new;
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DatagramChannel> getDatagramChannel(String proto, SocketAddress address)
	{
		return NettyEngine.isProtoNeutral(proto) ?
			KQueueDatagramChannel::new :
			() -> new KQueueDatagramChannel(NettyEngine.getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
	}
}
