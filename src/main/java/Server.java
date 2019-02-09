import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;

import java.util.function.Supplier;

public class Server {

    private final int port;

    public Server(int port) {
        this.port = port;
    }


    public void start(final Supplier<ChannelInboundHandler> handlerSupplier) throws InterruptedException {
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EventLoopGroup workerGroup = new EpollEventLoopGroup(1);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(EpollServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(handlerSupplier.get());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }



    public static void main(String[] args) {

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        Boolean ioBound = Boolean.getBoolean("io");
        Server server = new Server(port);
        try {
            server.start(() -> new FxRateGenerator(!ioBound));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
