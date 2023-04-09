package net.dryuf.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;


/**
 * Handler accumulating input data to a buffer and passing for further consumption.
 */
public class CumulatingCharBufferHandler extends ChannelDuplexHandler
{
	/** Max length of accumulated data. If breached, connection gets killed. */
	private final int maxLength;

	private final CharsetDecoder decoder;

	private final ByteBuffer nioAccumulator = ByteBuffer.allocate(64);

	protected CharBuffer accumulator = CharBuffer.allocate(4);

	private ChannelConfig config;

	private boolean shouldConsume = false;

	private boolean needMore = true;

	public CumulatingCharBufferHandler(CharsetDecoder decoder, int maxLength)
	{
		this.decoder = decoder;
		this.maxLength = maxLength;
	}

	public CumulatingCharBufferHandler(Charset charset, int maxLength)
	{
		this(charset.newDecoder(), maxLength);
	}

	public CumulatingCharBufferHandler(int maxLength)
	{
		this(StandardCharsets.UTF_8, maxLength);
	}

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
			for (;;) {
				nioAccumulator.limit(Math.min(nioAccumulator.capacity(), input.readableBytes() + nioAccumulator.position()));
				input.readBytes(nioAccumulator);
				nioAccumulator.flip();
				try {
					CoderResult result = decoder.decode(nioAccumulator, accumulator, false);
					if (result.isOverflow()) {
						CharBuffer newAccumulator = CharBuffer.allocate(accumulator.capacity()*2);
						accumulator.flip();
						newAccumulator.put(accumulator);
						accumulator = newAccumulator;
					}
					else if (result.isUnderflow()) {
						if (input.readableBytes() == 0) {
							break;
						}
					}
					else if (result.isError()) {
						throw new IOException("Failed to map input bytes: error="+result);
					}
				}
				finally {
					nioAccumulator.compact();
				}
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to translate bytes input into a string", e);
		}
		finally {
			ReferenceCountUtil.release(input);
		}
		needMore = false;
		dequeue(ctx, shouldConsume ? 1 : 0);
		if (accumulator.position() > maxLength) {
			throw new IllegalStateException("Too much unconsumed data received from client: " +
				"length=" + accumulator.position() + " client=" +ctx.channel());
		}
	}

	private int dequeue(ChannelHandlerContext ctx, int minConsume)
	{
		accumulator.flip();
		try {
			int consumed = 0;
			int lastRead = accumulator.remaining();
			while (consumed < minConsume || config.isAutoRead()) {
				needMore = true;
				ctx.fireChannelRead(accumulator);
				int newRead = accumulator.remaining();
				if (newRead != lastRead) {
					lastRead = newRead;
					consumed++;
					needMore = false;
				}
				else {
					break;
				}
			}
			return consumed;
		}
		finally {
			accumulator.compact();
		}
	}
}
