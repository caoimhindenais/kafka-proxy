package io.proxy;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.message.FindCoordinatorResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.record.*;
import org.apache.kafka.common.requests.*;
import utils.KafkaProxyUtils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static io.proxy.KafkaProxyInitializer.MESSAGES;

@Slf4j
public class KafkaProxyBackendHandler extends ChannelInboundHandlerAdapter {

    private static final int PROXY_PORT = 19092;
    private final Channel inboundChannel;
    private final KafkaProxyUtils kafkaProxyUtils = new KafkaProxyUtils();

    KafkaProxyBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {

        log.info("------Response-----------");

        byte[] array = ((ByteBuf) msg).array();
        int size = ((ByteBuf) msg).getInt(0);
        ByteBuffer wrap = ByteBuffer.wrap(Arrays.copyOfRange(array, 4, size + 4));

        ResponseHeader responseHeader = ResponseHeader.parse(wrap);

        KafkaProxyBackendHandler handler = (KafkaProxyBackendHandler)ctx.handler();
        InetSocketAddress socketAddress = (InetSocketAddress)handler.inboundChannel.remoteAddress();
        KafkaRequest kafkaRequest = MESSAGES.remove(socketAddress.getPort() + "-" + responseHeader.correlationId());

        short requestAPIKey;
        try {
            requestAPIKey = kafkaRequest.getKey();
        } catch (java.lang.NullPointerException e) {
            String logwrap = Arrays.toString(wrap.array());
            System.out.println("wrap = " + logwrap.substring(0, Math.min(logwrap.length(), 100)));
            System.out.println(MESSAGES);
            System.out.println("e = " + e);
            throw e;
        }
        log.info("Response apikey = " + requestAPIKey);


        System.out.println("Backend responseHeader sizeOf" + size + ": correlationId" + responseHeader.correlationId());
        log.info("Response id = " + requestAPIKey);
        ApiKeys apiKeys = ApiKeys.forId(requestAPIKey);

        short requestAPIVersion = kafkaRequest.getVersion();
        Struct responseStruct = null;
        try {
            responseStruct = apiKeys.parseResponse(requestAPIVersion, wrap);

        } catch (org.apache.kafka.common.protocol.types.SchemaException e) {
            System.out.println("e = " + e);
            byte[] array1 = ((ByteBuf) msg).array();
            System.out.println("wrap = " + Arrays.toString(array1));
            System.out.println("requestAPIVersion = " + requestAPIVersion);
            System.out.println("responseStruct = " + responseStruct);
            throw e;
        }

        ByteBuf byteBuffer = null;

        AbstractResponse body = AbstractResponse.
                parseResponse(ApiKeys.forId(requestAPIKey), responseStruct, requestAPIVersion);

        switch (apiKeys) {
            case METADATA: {
                MetadataResponse metadataResponse = (MetadataResponse) body;
                Collection<Node> brokers = metadataResponse.brokers();

                List<Node> proxyBrokers = brokers.stream().map(
                        n -> new Node(n.id(), n.host(), PROXY_PORT, n.rack())).collect(Collectors.toList());

                AbstractResponse proxyMetatdataResponse = MetadataResponse.prepareResponse(proxyBrokers,
                        metadataResponse.clusterId(),
                        metadataResponse.cluster().controller().id(),
                        List.copyOf(metadataResponse.topicMetadata()));

                ByteBuffer serialize = proxyMetatdataResponse.serialize(requestAPIVersion, responseHeader);
                byteBuffer = kafkaProxyUtils.prependBufferWithSize(serialize);
                break;
            }
            case FETCH: {
                FetchResponse fetchResponse = (FetchResponse) body;
                System.out.println("fetchResponse = " + fetchResponse);
                LinkedHashMap<TopicPartition, FetchResponse.PartitionData> responseData = fetchResponse.responseData();

                LinkedHashMap<TopicPartition, FetchResponse.PartitionData> modifiedResponseData = new LinkedHashMap<>();

                responseData.forEach((partition, partitionData) -> {
                    MemoryRecords memoryRecords = KafkaProxyFrontendHandler.decryptNewRecordsMap(partition, (MemoryRecords) partitionData.records);


                    FetchResponse.PartitionData partitionData1 = new FetchResponse.PartitionData(partitionData.error,
                            partitionData.highWatermark,
                            partitionData.lastStableOffset,
                            partitionData.logStartOffset,
                            partitionData.preferredReadReplica,
                            partitionData.abortedTransactions,
                            memoryRecords
                    );
                    modifiedResponseData.put(partition, partitionData1);

                });


                FetchResponse fetchResponse1 = new FetchResponse(fetchResponse.error(), modifiedResponseData, fetchResponse.throttleTimeMs(), fetchResponse.sessionId());
                ByteBuffer serialize = fetchResponse1.serialize(requestAPIVersion, responseHeader);
                byteBuffer = kafkaProxyUtils.prependBufferWithSize(serialize);

                break;
            }

            case FIND_COORDINATOR: {
                FindCoordinatorResponse findCoordinatorResponse = (FindCoordinatorResponse) body;
                FindCoordinatorResponseData data = findCoordinatorResponse.data();
                String host = data.host();
                System.out.println("host = " + host);
                int port = data.port();
                System.out.println("port = " + port);

                Node coordinator = new Node(data.nodeId(), data.host(), PROXY_PORT);

                FindCoordinatorResponse proxyFindCoordinatorResponse = FindCoordinatorResponse.prepareResponse(Errors.NONE, coordinator);

                ByteBuffer serialize = proxyFindCoordinatorResponse.serialize(requestAPIVersion, responseHeader);
                byteBuffer = kafkaProxyUtils.prependBufferWithSize(serialize);
                break;


            }
        }

        log.info("----------------------------");


        Object toSend = byteBuffer == null ? msg : byteBuffer;

        inboundChannel.writeAndFlush(toSend).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
            } else {
                future.channel().close();
            }
        });
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        KafkaProxyFrontendHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        KafkaProxyFrontendHandler.closeOnFlush(ctx.channel());
    }
}
