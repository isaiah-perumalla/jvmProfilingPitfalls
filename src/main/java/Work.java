import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class Work extends ChannelInboundHandlerAdapter {

    private volatile long total = 0;
    private final int[] values = new int[1024*1000];
    private ScheduledFuture<?> schedule;
    private static final ByteBuf BYTES = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("hello-world\n".getBytes(StandardCharsets.US_ASCII)));

    public Work() {
        init(values);
    }

    private static void init(int[] values) {
        for(int i =0; i < values.length; i++) {
            Random random = new Random();
            values[i] = random.nextInt();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        logInfo("connected to: "  + channel.remoteAddress());

        schedule = channel.eventLoop().scheduleAtFixedRate(() -> {
            try {
                doWork(4 * 1000_000);

                //unecessaryIO();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            channel.writeAndFlush(BYTES);
            logInfo("hello-world\n");

        }, 0, 500, TimeUnit.MILLISECONDS);


    }

    private static void logInfo(String msg) {
        System.out.println(msg);
    }


    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        schedule.cancel(false);
        logInfo("connection closed to: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void doWork(int limit) {
        long sum = hogCPUCycles(limit);
        store(sum);
    }

    private long hogCPUCycles(int limit) {
        long sum = 0;
        Random rand = new Random();
        for(int i = 0; i < limit; i++) {
            int randIndex = Math.abs(rand.nextInt() % values.length);
            int v = values[randIndex];
            sum += v;
        }
        return sum;
    }

    private void store(long sum) {
        storeValue0(sum);
    }

    private void storeValue0(long sum) {
        storeValue1(sum);
    }

    private void storeValue1(long sum) {
        total += sum;
    }

    private static void unecessaryIO() throws IOException {
        Socket socket = null;
        try {

            InetSocketAddress remote = new InetSocketAddress("www.google.com", 80);
            socket = new Socket();
            socket.setSoTimeout(2000);
            socket.connect(remote);
            logInfo("connected to "+remote);

            InputStream istream = socket.getInputStream();
            istream.read();

        }
        catch (SocketTimeoutException e) {}
        finally {
            if(socket != null) socket.close();
        }


    }

}
