package net.dryuf.netty.forward.test;

import com.google.common.collect.ImmutableList;
import net.dryuf.base.concurrent.future.FutureUtil;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.NettyEngine;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.forward.NettyPortForwarderFactory;
import net.dryuf.netty.forward.PortForwarderFactory;


/**
 * Proxy runner
 */
public class PortForwarderRunner
{
	public static void main(String[] args) throws Exception
	{
		try (NettyEngine nettyEngine = new NettyEngine()) {
			Server.waitOneAndClose(FutureUtil.nestedAllOrCancel(new NettyPortForwarderFactory(nettyEngine)
				.runForwards(ImmutableList.of(
					PortForwarderFactory.ForwardConfig.builder()
						.bind(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3300)
							.build())
						.connect(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3301)
							.build())
						.build(),
					PortForwarderFactory.ForwardConfig.builder()
						.bind(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3301)
							.build())
						.connect(AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.build(),
					PortForwarderFactory.ForwardConfig.builder()
						.bind(AddressSpec.builder()
							.proto("unix")
							.path("target/forward.socket")
							.build())
						.connect(AddressSpec.builder()
							.proto("tcp4")
							.host("localhost")
							.port(3302)
							.build())
						.build()
				))).get()).get();
			throw new IllegalStateException("Unreachable");
		}
	}
}
