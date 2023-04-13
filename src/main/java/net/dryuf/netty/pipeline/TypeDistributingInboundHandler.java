package net.dryuf.netty.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.dryuf.base.function.delegate.TypeDelegatingTriFunction3;


/**
 * Handler distributing the channelRead calls to its own functions based on message type.
 *
 * The message is automatically released in parent handler, no need to call ReferenceCountUtil.release() again.
 *
 * <pre>
	public static class TestHandler extends TypeDistributingHandler<TestHandler, Object, RuntimeException>
	{
		private static final TypeDelegatingTriFunction3<TestHandler, ChannelHandlerContext, Object, Void, RuntimeException> distributingCallbacks =
			TypeDelegatingTriFunction3.<TestHandler, ChannelHandlerContext, Object, Void, RuntimeException>callbacksBuilder()
				.add(First.class, TestHandler::firstHandler)
				.add(Second.class, TestHandler::secondHandler)
				.build();

		int called = 0;

		public TestHandler()
		{
			super(distributingCallbacks);
		}

		public Void firstHandler(ChannelHandlerContext ctx, First msg)
		{
			called = 1;
			return null;
		}

		public Void secondHandler(ChannelHandlerContext ctx, Second msg)
		{
			called = 2;
			return null;
		}
	}
 * </pre>
 *
 * @param <TP>
 *      type of this class
 * @param <I>
 *      type of input message
 * @param <X>
 *      type of thrown exception by channelRead
 */
public class TypeDistributingInboundHandler<TP, I, X extends Exception> extends SimpleChannelInboundHandler<I>
{
	private final TypeDelegatingTriFunction3<TP, ChannelHandlerContext, I, Void, X> callbacks;

	public TypeDistributingInboundHandler(TypeDelegatingTriFunction3<TP, ChannelHandlerContext, I, Void, X> callbacks)
	{
		this.callbacks = callbacks;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, I msg) throws X
	{
		@SuppressWarnings("unchecked")
		TP this0 = (TP) this;
		callbacks.apply(this0, ctx, msg);
	}
}
