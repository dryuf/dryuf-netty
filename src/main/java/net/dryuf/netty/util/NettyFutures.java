package net.dryuf.netty.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Netty Future utilities.
 */
public class NettyFutures
{
	/**
	 * Converts Netty Future to CompletableFuture.
	 */
	public static <V> CompletableFuture<V> toCompletable(Future<V> future)
	{
		return new CompletableFuture<V>() {
			{
				future.addListener((f) -> {
					try {
						complete(future.getNow());
					}
					catch (Throwable ex) {
						completeExceptionally(ex);
					}
				});
			}

			public boolean cancel(boolean interrupt)
			{
				try {
					return future.cancel(interrupt);
				}
				finally {
					super.cancel(interrupt);
				}
			}
		};
	}

	/**
	 * Propagates Netty Future to existing CompletableFuture.
	 */
	public static <V> void copy(Future<V> future, CompletableFuture<V> target)
	{
		future.addListener((f) -> {
			try {
				@SuppressWarnings("unchecked")
				V result = (V) f.get();
				target.complete(result);
			}
			catch (ExecutionException ex) {
				target.completeExceptionally(ex.getCause());
			}
			catch (Throwable ex) {
				target.completeExceptionally(ex);
			}
		});
	}

	/**
	 * Joins two Netty Future objects, propagating any exception or last result.
	 */
	public static <V> CompletableFuture<V> join(Future<V> one, Future<V> two)
	{
		return new CompletableFuture<V>() {
			{
				AtomicInteger count = new AtomicInteger(2);
				GenericFutureListener<Future<V>> listener = (f) -> {
					try {
						V v = f.getNow();
						if (count.decrementAndGet() == 0)
							complete(v);
					}
					catch (Throwable ex) {
						completeExceptionally(ex);
					}
				};
				one.addListener(listener);
				two.addListener(listener);
			}

			@Override
			public boolean cancel(boolean interrupt)
			{
				try {
					return one.cancel(interrupt)|two.cancel(interrupt);
				}
				finally {
					super.cancel(interrupt);
				}
			}
		};
	}
}
