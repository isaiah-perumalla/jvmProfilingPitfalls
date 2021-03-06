import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

class FxRateGenerator extends ChannelInboundHandlerAdapter {

    private static final int[] values = new int[1024*1000];
    private final ToLongFunction<Integer> generateRateFunc;
    private ScheduledFuture<?> schedule;
    private static final ByteBuf GBPUSD = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("GBPUSD=".getBytes(StandardCharsets.US_ASCII)));
    private ByteBuf CR_LF = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("\r\n".getBytes(StandardCharsets.US_ASCII)));
    public FxRateGenerator(boolean cpuBoundWork) {
        if(cpuBoundWork) {
            this.generateRateFunc = x -> hogCPUCycles(x);
        }
        else{
            this.generateRateFunc = x -> unecessaryIO();
        }
    }

    static {
        //do not cache  dns lookups, just to simulate slow IO
        Security.setProperty("newtorkaddress.cache.ttl", "0");
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
        logInfo("connected to: "  + channel.remoteAddress(), System.out);

        schedule = channel.eventLoop().scheduleAtFixedRate(() -> {
            try {
                float rate = computeRandRate(4 * 1000_000);
                String s = Float.toString(rate);
                ByteBuf rateBytes = Unpooled.copiedBuffer(s.getBytes(StandardCharsets.US_ASCII));
                channel.write(GBPUSD);
                channel.write(rateBytes);
                channel.writeAndFlush(CR_LF);
                logInfo(s, System.out);

            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 500, TimeUnit.MILLISECONDS);

    }

    private static void logInfo(String msg, PrintStream printStream) {
        printStream.println(msg);
    }


    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        schedule.cancel(false);
        logInfo("connection closed to: " + ctx.channel().remoteAddress(), System.out);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private float computeRandRate(int limit) {
        long value = 0;
        try {
            value = generateRateFunc.applyAsLong(limit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Random rand = new Random(value);
        return 1 + rand.nextFloat();
    }

    private static long hogCPUCycles(int limit) {
        long sum = 0;
        Random rand = new Random();
        for(int i = 0; i < limit; i++) {
            int randIndex = Math.abs(rand.nextInt() % values.length);
            int v = values[randIndex];
            sum += v;
        }
        return sum;
    }

    private static long unecessaryIO() {
        Socket socket = null;
        try {

            InetSocketAddress remote = new InetSocketAddress("www.google.com", 80);
            socket = new Socket();
            socket.setSoTimeout(2000);
            socket.connect(remote);
            logInfo("connected to "+remote, System.out);

            InputStream istream = socket.getInputStream();
            int i = istream.read();
            return i;

        }
        catch (Exception e) {}
        finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        return 0;
    }

}
