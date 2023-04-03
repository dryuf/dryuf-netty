package net.dryuf.netty.provider;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.unix.DomainSocketAddress;
import net.dryuf.netty.core.NettyEngine;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.util.concurrent.TimeUnit;


public class EpollChannelProvider implements ChannelProvider
{
	static {
		EpollEventLoopGroup events = new EpollEventLoopGroup();
		events.shutdownGracefully(0, 0, TimeUnit.SECONDS);
	}

	@Override
	public EventLoopGroup createEventLoopGroup(int threads)
	{
		return new EpollEventLoopGroup(threads);
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
			return () -> NettyEngine.isProtoNeutral(proto) ?
				new EpollServerSocketChannel() :
				new EpollServerSocketChannel(NettyEngine.getNettyProtocolByAddress(((InetSocketAddress)address).getAddress()));
		}
		else if (address instanceof DomainSocketAddress || address instanceof UnixDomainSocketAddress) {
			return EpollServerDomainSocketChannel::new;
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
					new EpollSocketChannel() :
					new EpollSocketChannel(NettyEngine.getNettyProtocolByAddress(((InetSocketAddress)address).getAddress()));
		}
		else if (address instanceof DomainSocketAddress || address instanceof UnixDomainSocketAddress) {
			return EpollDomainSocketChannel::new;
		}
		else {
			throw new UnsupportedOperationException("Unsupported socket address: class="+address.getClass());
		}
	}

	@Override
	public ChannelFactory<? extends DatagramChannel> getDatagramChannel(String proto, SocketAddress address)
	{
		return NettyEngine.isProtoNeutral(proto) ?
			EpollDatagramChannel::new :
			() -> new EpollDatagramChannel(NettyEngine.getNettyProtocolByAddress(((InetSocketAddress) address).getAddress()));
	}
}
