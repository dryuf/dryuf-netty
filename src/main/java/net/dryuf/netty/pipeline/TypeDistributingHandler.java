package net.dryuf.netty.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.dryuf.base.function.delegate.TypeDelegatingTriFunction3;


/**
 * Handler distributing the channelRead calls to its own functions based on message type.
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
 * @param <C>
 *      type of message class
 * @param <X>
 *      type of thrown exception by channelRead
 */
public class TypeDistributingHandler<TP, C, X extends Exception> extends ChannelInboundHandlerAdapter
{
	private final TypeDelegatingTriFunction3<TP, ChannelHandlerContext, C, Void, X> callbacks;

	public TypeDistributingHandler(TypeDelegatingTriFunction3<TP, ChannelHandlerContext, C, Void, X> callbacks)
	{
		this.callbacks = callbacks;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws X
	{
		@SuppressWarnings("unchecked")
		TP this0 = (TP) this;
		@SuppressWarnings("unchecked")
		C msg0 = (C) msg;
		callbacks.apply(this0, ctx, msg0);
	}
}
