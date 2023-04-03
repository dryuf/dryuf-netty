# Dryuf Netty

Set of reusable Netty components.

## Release

```
<dependency>
	<groupId>net.dryuf</groupId>
	<artifactId>dryuf-netty</artifactId>
	<version>0.0.1</version>
</dependency>
```


## Usage

### Basic construction

```
try (NettyEngine engine = new NettyEngine()) {
	CompletableFuture<ServerChannel> server1 = engine.listen(AddressSpec.builder()
		.family(AddressSpec.AF_TCP4)
		.host("*")
		.port(8090)
		.build()
	);
	CompletableFuture<ServerChannel> server2 = engine.listen(AddressSpec.builder()
		.family(AddressSpec.AF_TCP4)
		.host("*")
		.port(8091)
		.build()
	);
	server1.get();
	server2.get();
	for (;;) {
		Thread.sleep(Long.MAX_VALUE, TimeUnit.SECONDS);
	}
}
```


## License

The code is released under version 2.0 of the [Apache License][].


## Stay in Touch

Feel free to contact me at kvr000@gmail.com and http://github.com/kvr000/ and http://github.com/dryuf/ and https://www.linkedin.com/in/zbynek-vyskovsky/

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0

<!--- vim: set tw=120: --->
