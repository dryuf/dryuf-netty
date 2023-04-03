package net.dryuf.netty.address;

import io.netty.channel.unix.DomainSocketAddress;
import lombok.Builder;
import lombok.Value;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * Address specification, including protocol.
 */
@Builder(builderClassName = "Builder")
@Value
public class AddressSpec
{
	public static final String PROTO_TCP = "tcp";
	public static final String PROTO_TCP4 = "tcp4";
	public static final String PROTO_TCP6 = "tcp6";
	public static final String PROTO_UDP = "udp";
	public static final String PROTO_UDP4 = "udp4";
	public static final String PROTO_UDP6 = "udp6";
	public static final String PROTO_UNIX = "unix";
	public static final String PROTO_DOMAIN = "domain";

	public static final String ANY_HOST = "*";

	/**
	 * Address family, one of tcp4, tcp6, unix, domain:
	 */
	String proto;
	/**
	 * Domain socket path:
	 */
	String path;
	/**
	 * Inet socket host:
	 */
	String host;
	/**
	 * Inet socket port:
	 */
	int port;

	public static AddressSpec fromSocketAddress(SocketAddress address)
	{
		if (address instanceof DomainSocketAddress) {
			return AddressSpec.builder()
				.proto("domain")
				.path(((DomainSocketAddress) address).path())
				.build();
		}
		else if (address instanceof InetSocketAddress) {
			InetSocketAddress a = (InetSocketAddress) address;
			return AddressSpec.builder()
				.proto(a.getAddress() instanceof Inet6Address ? "tcp6" : "tcp4")
				.host(a.getHostString())
				.port(a.getPort())
				.build();
		}
		else {
			throw new UnsupportedOperationException("Unsupported SocketAddress: " + address.getClass().getName());
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(proto);
		sb.append("://");
		if (proto.equals(PROTO_UNIX) || proto.equals(PROTO_DOMAIN)) {
			sb.append(path);
		}
		else {
			sb.append(host).append(":").append(port);
		}
		return sb.toString();
	}
}
