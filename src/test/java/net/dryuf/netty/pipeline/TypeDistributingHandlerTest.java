package net.dryuf.netty.pipeline;

import io.netty.channel.ChannelHandlerContext;
import net.dryuf.base.function.delegate.TypeDelegatingTriFunction3;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class TypeDistributingHandlerTest
{
	@Test
	public void channelRead_specificClass_callSpecificMethod()
	{
		TestHandler handler = new TestHandler();

		handler.channelRead(null, new First());
		assertEquals(handler.called, 1);

		handler.channelRead(null, new Second());
		assertEquals(handler.called, 2);
	}

	public static class First {}

	public static class Second {}

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
}
