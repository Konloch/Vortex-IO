# Socket Server
Socket Server is a high preformant low-level non-blocking socket server API. [Click here for the core library source code](https://github.com/Konloch/Socket-Server/tree/main/Core).

## 💡 Requirements
+ Java Runtime 1.8 **or higher**

## ⚙️ How To Add As Library
Add it as a maven dependency or just [download the latest release](https://github.com/Konloch/Socket-Server/releases).
```xml
<dependency>
  <groupId>com.konloch</groupId>
  <artifactId>Socket-Server</artifactId>
  <version>0.9.4</version>
</dependency>
```

## 📚 Links
* [Website](https://konloch.com/Socket-Server/)
* [Discord Server](https://discord.gg/aexsYpfMEf)
* [Download Releases](https://github.com/Konloch/Socket-Server/releases)

## 💻 How To Use
You can view an [HTTP Server implementation here](https://github.com/Konloch/HTTPdLib/), or an [IRC Server implementation here](https://github.com/Konloch/OpenIRCd), or an [Echo Server implementation here](https://github.com/Konloch/Socket-Server/tree/main/Example/CR-Echo-Server).

### [RFC-862](https://www.rfc-editor.org/rfc/rfc862) compliant echo server
```java
SocketServer server = new SocketServer(7, client -> 
{
    switch(client.getState())
    {
        //signal we want to start reading into the buffer
        case 0:
            //signal that we want to start reading and to fill up the buffer
            client.setInputRead(true);
            
            //advance to stage 1
            client.setState(1);
            break;
            
        //wait until the stream has signalled the buffer has reached the end
        case 1:
            //when the buffer is full advance to stage 2
            if(!client.isInputRead())
                client.setState(2);
            break;
            
        //announce the read
        case 2:
            //get the bytes written
            byte[] bytes = client.getInputBuffer().toByteArray();
            
            //reset the input buffer
            client.getInputBuffer().reset();
        
            //echo the bytes back
            client.write(bytes);
            
            //loop back to stage 0
            client.setState(0);
            break;
    }
});
server.start();
```
