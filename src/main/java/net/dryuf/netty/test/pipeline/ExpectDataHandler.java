package net.dryuf.netty.test.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
public class ExpectDataHandler extends ChannelInboundHandlerAdapter
{
	private final CompletableFuture<Object> finished;

	private final Object expected;

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception
	{
		ctx.read();
		super.handlerAdded(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try {
			if (msg.equals((expected))) {
				finished.complete(expected);
				ReferenceCountUtil.touch(msg);
				ctx.writeAndFlush(msg).addListener((f) -> {
					ctx.close();
				});
			}
			else {
				ctx.read();
			}
		}
		finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		finished.completeExceptionally(cause);
		super.exceptionCaught(ctx, cause);
	}
}
