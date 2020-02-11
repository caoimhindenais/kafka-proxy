package utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.proxy.KafkaRequest;

public class KafkaProxyUtils {
    public KafkaProxyUtils() {
    }

    public ByteBuf prependBufferWithSize(ByteBuffer buffer) {
        int length = buffer.array().length;
        ByteBuffer sizedBuffer = ByteBuffer.allocate(4 + length);
        sizedBuffer.putInt(length);
        sizedBuffer.put(buffer);
        sizedBuffer.flip();
        return Unpooled.wrappedBuffer(sizedBuffer);

    }

    public static BlockingQueue<KafkaRequest> queue = new LinkedBlockingQueue<>();
}