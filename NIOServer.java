package interruptible;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Iterator;

public class NIOServer {
    private static String host = "localhost";
    private static int port = 9222;

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(host,port));
        server.register(selector, SelectionKey.OP_ACCEPT);

        while(true){
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();

                if(key.isAcceptable()){
                    SocketChannel channel = server.accept();
                    channel.configureBlocking(false);
                    channel.register(selector,SelectionKey.OP_WRITE);
                    continue;
                }

                if(key.isWritable()){
                    SocketChannel channel = (SocketChannel) key.channel();
                    channel.configureBlocking(false);
                    sendFileToClient(channel);
                    channel.close();
                    return;
                }
            }
        }
    }

    private static void sendFileToClient(SocketChannel channel) throws IOException {
        int sizeBuffer = 10240;
        ByteBuffer buffer = ByteBuffer.allocate(sizeBuffer);
        String pathName = "G:\\Helion.pdf";
        Path path = Paths.get(pathName);
        //EnumSet<StandardOpenOption> enumSet = EnumSet.of(StandardOpenOption.READ);
        FileChannel fileChannel = FileChannel.open(path);

        int readBytes = 0;
        int count = 0;

        do{
            readBytes = fileChannel.read(buffer);
            if(readBytes <= 0) break;
            count += readBytes;
            buffer.flip();

            do{
                readBytes -= channel.write(buffer);
            }while(readBytes > 0);
            buffer.clear();

        }while(true);

        System.out.println("count = "+count);
        fileChannel.close();
    }
}
