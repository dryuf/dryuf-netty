package net.dryuf.netty.echo;

import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.test.ClientServerTester;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;


@Log4j2
public class EchoEndTest
{
	private static final String output = StringUtils.repeat("Hello world\n", 1000);

	@Test
	public void testEcho() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			InetSocketAddress serverAddress = EchoEndTester.runEchoServer(tester);

			try (ClientServerTester tester2 = new ClientServerTester()) {
				EchoEndTester.runEchoClient(tester2, serverAddress, 1);
			}
		}
	}
}
