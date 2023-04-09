package net.dryuf.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;


/**
 * Handler accumulating input data to a buffer and passing for further consumption.
 *
 * Manual alternative to {@link io.netty.handler.codec.ReplayingDecoder} .
 */
@RequiredArgsConstructor
public class CumulatingByteBufHandler extends ChannelDuplexHandler
{
	/** Max length of accumulated data. If breached, connection gets killed. */
	private final int maxLength;

	protected final ByteBuf accumulator = Unpooled.unreleasableBuffer(Unpooled.buffer());

	private ChannelConfig config;

	private boolean shouldConsume = false;

	private boolean needMore = true;

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		config = ctx.channel().config();
		super.handlerAdded(ctx);
	}

	@Override
	public void read(ChannelHandlerContext ctx) throws Exception {
		if (needMore || dequeue(ctx, 1) == 0) {
			shouldConsume = true;
			ctx.read();
		}
		else if (config.isAutoRead()) {
			ctx.read();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		ByteBuf input = (ByteBuf) msg;
		try {
			accumulator.writeBytes(input);
		}
		finally {
			ReferenceCountUtil.release(input);
		}
		needMore = false;
		dequeue(ctx, shouldConsume ? 1 : 0);
		if (accumulator.readableBytes() > maxLength) {
			throw new IllegalStateException("Too much unconsumed data received from client: " +
				"length=" + accumulator.readableBytes() + " client=" +ctx.channel());
		}
	}

	private int dequeue(ChannelHandlerContext ctx, int minConsume)
	{
		int consumed = 0;
		int lastRead = accumulator.readerIndex();
		while (consumed < minConsume || config.isAutoRead()) {
			needMore = true;
			ctx.fireChannelRead(accumulator);
			int newRead = accumulator.readerIndex();
			if (newRead != lastRead) {
				lastRead = newRead;
				consumed++;
				needMore = false;
			}
			else {
				break;
			}
		}
		if (consumed != 0) {
			accumulator.discardReadBytes();
		}
		return consumed;
	}
}
