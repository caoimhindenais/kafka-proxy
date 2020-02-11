package io.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.record.*;
import org.apache.kafka.common.requests.AbstractRequest;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.requests.RequestHeader;
import utils.AES;
import utils.KafkaProxyUtils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.proxy.KafkaProxyInitializer.MESSAGES;
import static utils.KafkaProxyUtils.*;

@Slf4j
public class KafkaProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private final String remoteHost;
    private final int remotePort;

    private final KafkaProxyUtils kafkaProxyUtils = new KafkaProxyUtils();


    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    private Channel outboundChannel;

    KafkaProxyFrontendHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

     private static final AttributeKey<KafkaRequest> KAFKA_REQUEST_DETAILS = new AttributeKey<>("KAFKA_REQUEST");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();


        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
         .channel(ctx.channel().getClass())
         .handler(new KafkaProxyBackendHandler(inboundChannel))
         .option(ChannelOption.AUTO_READ, false);

        ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();

        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // connection complete start to read first data
                inboundChannel.read();
            } else {
                // Close the connection if the connection attempt has failed.
                inboundChannel.close();
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws InterruptedException {



        log.info("------Request-----------");

        byte[] array = ((ByteBuf) msg).array();

        int size = ((ByteBuf) msg).getInt(0);


        ByteBuffer wrap = ByteBuffer.wrap(Arrays.copyOfRange(array,4,size+4));
        RequestHeader requestHeader = RequestHeader.parse(wrap);
        short requestAPIKey = requestHeader.apiKey().id;
        short requestAPIversion = requestHeader.apiVersion();
        System.out.println("Frontend requestHeader = " + requestHeader);
        ApiKeys apiKeys = ApiKeys.forId(requestAPIKey);

        KafkaRequest kafkaRequest = KafkaRequest.builder().key(requestAPIKey).version(requestAPIversion).build();
        queue.put(kafkaRequest);

        ctx.channel().attr(KAFKA_REQUEST_DETAILS).set(kafkaRequest);


        // Uniquely track the request
        InetSocketAddress socketAddress = (InetSocketAddress)ctx.channel().remoteAddress();
        int port = socketAddress.getPort();
        int correlationId = requestHeader.correlationId();

        MESSAGES.put(port+"-"+correlationId, kafkaRequest);

        System.out.println("Address = " + socketAddress);

        Struct requestStruct = apiKeys.parseRequest(requestHeader.apiVersion(), wrap);

        AbstractRequest body = AbstractRequest.parseRequest(apiKeys, requestAPIversion,requestStruct);

        ByteBuf byteBuffer = null;

        switch (apiKeys) {
            case PRODUCE: {
                ProduceRequest produceRequest = (ProduceRequest) body;

                Map<TopicPartition, MemoryRecords> modifiedRecordMap = new HashMap<>();

                Map<TopicPartition, MemoryRecords> topicPartitionMemoryRecordsMap = produceRequest.partitionRecordsOrFail();
                topicPartitionMemoryRecordsMap.forEach((partition, records)-> {
                    modifiedRecordMap.put(partition, encryptNewRecordsMap(partition, records));
                });


                ProduceRequest newProducerRequest = ProduceRequest.Builder.forCurrentMagic((short) 1, 5000, modifiedRecordMap).build(requestAPIversion);
                ByteBuffer serialize = newProducerRequest.serialize(requestHeader);
                byteBuffer = kafkaProxyUtils.prependBufferWithSize(serialize);


            }
        }

        Object data = byteBuffer == null ? msg : byteBuffer;

        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(data).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            });
        }
    }

    static MemoryRecords decryptNewRecordsMap(TopicPartition partition, MemoryRecords memoryRecords) {
        boolean encypt = false;
        return buildNewRecordsMap(partition, memoryRecords, encypt);
    }

    private static MemoryRecords encryptNewRecordsMap(TopicPartition partition, MemoryRecords memoryRecords) {
        boolean encypt = true;
        return buildNewRecordsMap(partition, memoryRecords, encypt );
    }



    private static MemoryRecords buildNewRecordsMap(TopicPartition partition, MemoryRecords memoryRecords, boolean encypt) {
        System.out.println("Working on partition = " + partition);

        long offset;
        Iterator<Record> iterator = memoryRecords.records().iterator();
        MemoryRecordsBuilder memoryRecordsBuilder;

        if(iterator.hasNext()) {
            offset = iterator.next().offset();
            memoryRecordsBuilder = MemoryRecords.builder(ByteBuffer.allocate(1024), CompressionType.NONE,
                    TimestampType.CREATE_TIME, offset);
        } else {
            return MemoryRecords.builder(ByteBuffer.allocate(1024), CompressionType.NONE,
                    TimestampType.CREATE_TIME, 0).build();
        }


        Iterable<Record> records = memoryRecords.records();
        for (Record record : records) {

            ByteBuffer value = getByteBufferSlice(record.value());
            ByteBuffer key = getByteBufferSlice(record.key());


            ByteBuffer cryptoValue;

            if(encypt) {
                cryptoValue = ByteBuffer.wrap(AES.encrypt( AES.TOP_SECRET_KEY, value.array()));
            } else {
                cryptoValue = ByteBuffer.wrap(AES.decrypt( AES.TOP_SECRET_KEY,value.array()));

                System.out.println("Decoded value = " + new String(cryptoValue.array(), StandardCharsets.UTF_8));
            }
            memoryRecordsBuilder.append(record.timestamp(), key,cryptoValue);

        }

        return memoryRecordsBuilder.build();
    }

    private static ByteBuffer getByteBufferSlice(ByteBuffer buffer) {
        return ByteBuffer.wrap(Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.limit()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
