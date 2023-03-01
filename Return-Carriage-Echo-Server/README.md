# Return Carriage Echo Server
Return Carriage Echo Server is a low-level non-blocking echo server.

It will echo-back the contents of the buffer if it has reached 1024 characters, or encountered a return carriage.

## Links
* [Website](https://konloch.com/Return-Carriage-Echo-Server/)
* [Discord Server](https://discord.gg/aexsYpfMEf)
* [Download Releases](https://github.com/Konloch/Return-Carriage-Echo-Server/releases)

## How To Use
You can run the Java Jar file directly, or you can use it as a library
```java
ReturnCarriageEchoServer server = new ReturnCarriageEchoServer(7, 1);
server.start();
```