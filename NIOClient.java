package interruptible;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Iterator;

public class NIOClient {
    private static String host = "localhost";
    private static int remotePort = 9222;
    private static int localPort = 9333;

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        SocketChannel client = SocketChannel.open();
        client.bind(new InetSocketAddress(host,localPort));
        client.configureBlocking(false);
        client.connect(new InetSocketAddress(host,remotePort));
        client.register(selector, SelectionKey.OP_CONNECT);

        while(true){
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();

                if(key.isConnectable()){
                    if(client.isConnectionPending()){
                        client.finishConnect();
                    }
                    SocketChannel channel = (SocketChannel) key.channel();
                    channel.configureBlocking(false);
                    channel.register(selector,SelectionKey.OP_READ);
                    continue;
                }

                if(key.isReadable()){
                    SocketChannel channel = (SocketChannel) key.channel();
                    channel.configureBlocking(false);
                    receiveFileFromServer(channel);
                    channel.close();
                    return;
                }
            }
        }
    }

    private static void receiveFileFromServer(SocketChannel channel) throws IOException {
        int sizeBuffer = 10240;
        ByteBuffer buffer = ByteBuffer.allocate(sizeBuffer);
        String pathName = "G:\\Kopia_Helion.pdf";
        Path path = Paths.get(pathName);
        EnumSet<StandardOpenOption> enumSet = EnumSet.of(StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
        FileChannel fileChannel = FileChannel.open(path,enumSet);

        int count = 0;
        int readBytes = 0;

        do{
            readBytes = channel.read(buffer);
            System.out.println("readBytes = "+readBytes);
            buffer.flip();
            //if(readBytes <= 0) break;

            /*do {
                readBytes -= fileChannel.write(buffer);
            }while(readBytes > 0);*/
            if(readBytes > 0){
                fileChannel.write(buffer);
                count += readBytes;
            }
            buffer.clear();
        }while(readBytes >= 0);

        fileChannel.close();
        System.out.println("count = "+count);
    }
}
