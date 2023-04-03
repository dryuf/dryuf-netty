package net.dryuf.netty.echo.test;

import lombok.extern.log4j.Log4j2;
import net.dryuf.netty.echo.EchoEndTester;
import net.dryuf.netty.test.ClientServerTester;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * Simple Echo server and client running in high parallelism.
 */
@Log4j2
public class EchoTestRunner
{
	public static void main(String[] args) throws Exception
	{
		System.exit(new EchoTestRunner().run(args));
	}

	public int run(String[] args) throws Exception
	{
		return execute();
	}

	public int execute() throws Exception
	{
		try (ClientServerTester tester = new ClientServerTester()) {
			SocketAddress serverAddress = EchoEndTester.runEchoServer(tester, InetSocketAddress.createUnresolved("localhost", 40100));
			log.info("EchoServer listening on: {}", serverAddress);
			Thread.sleep(Long.MAX_VALUE);
		}
		return 0;
	}
}
