# CR Echo Server
CR Echo Server is a low-level non-blocking echo server.

It will echo-back the contents of the buffer if it has reached 1024 characters, or encountered a carriage return.

## Links
* [Website](https://konloch.com/CR-Echo-Server/)
* [Discord Server](https://discord.gg/aexsYpfMEf)
* [Download Releases](https://github.com/Konloch/CR-Echo-Server/releases)

## How To Use
You can run the Java Jar file directly, or you can use it as a library
```java
CREchoServer server = new CREchoServer(7, 1);
server.start();
```

### How To Connect
Using `telnet` you can easily test the server.
```
telnet localhost 7
```
