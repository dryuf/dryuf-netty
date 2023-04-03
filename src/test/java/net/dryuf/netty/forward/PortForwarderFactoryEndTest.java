package net.dryuf.netty.forward;

import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.Server;
import net.dryuf.netty.echo.EchoEndTester;
import net.dryuf.netty.test.ClientServerTester;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


@Log4j2
public class PortForwarderFactoryEndTest
{
	@Test
	public void testForward() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = EchoEndTester.runEchoServer(tester);

			SocketAddress forward0Address = runForward(tester, serverAddress);

			EchoEndTester.runEchoClient(tester, forward0Address, 2);
		}
	}

	@Test
	public void testLongForward() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = EchoEndTester.runEchoServer(tester);

			SocketAddress last = serverAddress;
			for (int i = 0; i < 10; ++i) {
				last = runForward(tester, last);
			}

			EchoEndTester.runEchoClient(tester, last, 10);
		}
	}

	public static InetSocketAddress runForward(ClientServerTester tester, SocketAddress destination)
	{
		return runForward(tester, destination, InetSocketAddress.createUnresolved("localhost", 0));
	}

	public static <T extends SocketAddress> T runForward(ClientServerTester tester, SocketAddress destination, T source)
	{
		Server forward0 = new NettyPortForwarderFactory(tester.nettyEngine()).runForward(
			PortForwarderFactory.ForwardConfig.builder()
				.connect(AddressSpec.fromSocketAddress(destination))
				.bind(AddressSpec.fromSocketAddress(source))
				.build()
		).join();
		tester.addServer(forward0);

		@SuppressWarnings("unchecked")
		T address = (T) forward0.listenAddress();
		log.info("Forwarder listening: {}", address);
		return address;
	}
}
