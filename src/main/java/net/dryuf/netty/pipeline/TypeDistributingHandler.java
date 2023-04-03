package net.dryuf.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.dryuf.concurrent.collection.TypeDelegatingOwnerBiFunction;

public class TypeDistributingHandler<TP> extends ChannelInboundHandlerAdapter
{
	private final TypeDelegatingOwnerBiFunction<TP, Object, Void> callbacks;

	public TypeDistributingHandler(TypeDelegatingOwnerBiFunction<TP, Object, Void> callbacks)
	{
		this.callbacks = callbacks;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		@SuppressWarnings("unchecked")
		TP this0 = (TP) this;
		callbacks.apply(this0, msg);
	}
}
