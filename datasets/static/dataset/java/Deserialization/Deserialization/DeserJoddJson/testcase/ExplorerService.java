<filename>interfacemodules/iscsi/src/main/java/org/treetank/iscsi/jscsi/TreetankConfiguration.java<fim_prefix>

// (c) 2019 The Unbounded Network LTD
package com.hacera;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.common.Common.BlockData;
import org.hyperledger.fabric.protos.common.Common.BlockHeader;
import org.hyperledger.fabric.protos.common.Common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Common.Envelope;
import org.hyperledger.fabric.protos.common.Common.Header;
import org.hyperledger.fabric.protos.common.Common.Payload;
import org.hyperledger.fabric.protos.common.Common.SignatureHeader;
import org.hyperledger.fabric.protos.common.Ledger.BlockchainInfo;
import org.hyperledger.fabric.protos.common.MspPrincipal;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.Identities.SerializedIdemixIdentity;
import org.hyperledger.fabric.protos.msp.Identities.SerializedIdentity;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricTransaction;
import org.hyperledger.fabric.protos.peer.FabricTransaction.ChaincodeActionPayload;
import org.hyperledger.fabric.protos.peer.FabricTransaction.Transaction;
import org.hyperledger.fabric.sdk.SDKUtils;

public class ExplorerService {

    private static class JsonError {
        public String error;
    }

    private static class RestBaseHandler {

        protected FabricContext ctx;
        public RestBaseHandler(FabricContext ctx) {
            this.ctx = ctx;
        }

        protected boolean validateMethod(HttpExchange he, String... allowed) {
            String method = he.getRequestMethod();
            for (String s : allowed) {
                if (s.compareToIgnoreCase(method) == 0)
                    return true; // all ok
            }
            returnError(he, 405, String.format("Method '%s' not allowed on this endpoint", method));
            return false;
        }

        private static byte[] readAllStream(InputStream in) throws IOException {

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int len;

            // read bytes from the input stream and store them in buffer
            while ((len = in.read(buffer)) != -1) {
                // write bytes from the buffer into output stream
                os.write(buffer, 0, len);
            }

            return os.toByteArray();
        }

        protected <T> T readInput(HttpExchange he, Class<T> inputType) {

            try {

                InputStream is = he.getRequestBody();
                //if (is.available() == 0)
                //    return null;
                byte[] bytes = readAllStream(is);
                if (bytes.length == 0)
                    return null;
                JsonParser jsonParser = new JsonParser();
                <fim_suffix>
                return input;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        protected void returnJson(HttpExchange he, int code, Object o) {

            try {

                //
                String output = JsonSerializer.create().deep(true).serialize(o);
                byte[] outputBytes = output.getBytes(StandardCharsets.UTF_8);
                //
                Headers hdrs = he.getResponseHeaders();
                hdrs.add("Content-Type", "application/json");
                he.sendResponseHeaders(code, outputBytes.length);
                //
                OutputStream os = he.getResponseBody();
                os.write(outputBytes);
                os.close();

            } catch (Throwable t) {

                System.err.format("REST: Nested error while returning JSON message:%n");
                t.printStackTrace(System.err);
                throw new RuntimeException(t);

            }

        }

        protected void returnError(HttpExchange he, int code, String str) {

            try {

                JsonError err = new JsonError();
                err.error = str;
                returnJson(he, code, err);

            } catch (Throwable t) {

                System.err.format("REST: Nested error while returning an error message:%n");
                t.printStackTrace(System.err);
                throw new RuntimeException(t);

            }

        }

        protected Map<String, String> splitQuery(URI url) throws UnsupportedEncodingException {
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            String query = url.getQuery();
            if (query == null || query.isEmpty())
                return query_pairs;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
            return query_pairs;
        }

        private class ThreadHelper extends Thread {
            private HttpExchange he;
            public ThreadHelper(HttpExchange he) {
                this.he = he;
            }

            @Override
            public void run() {
                try {
                    handleThreaded(he);
                } catch (Throwable t) {
                    System.err.format("Error in REST server thread:%n");
                    t.printStackTrace(System.err);
                    try {
                        returnError(he, 500, "Internal REST service error");
                    } catch (Throwable t2) {
                        System.err.format("Nested error in REST server thread while returning an error:%n");
                        t2.printStackTrace(System.err);
                    }
                }
            }
        }

        public void handle(HttpExchange he) throws IOException {

            ThreadHelper th = new ThreadHelper(he);
            th.start();

        }

        public void handleThreaded(HttpExchange he) throws IOException {
            // default implementation
            he.close();
        }

    }

    private static BlocksReader blocksService;

    private static class BlocksReader extends Thread {

        FabricContext ctx;
        long currentHeight;
        long lowestHeight;
        final List<JsonBlock> blocks = new LinkedList<JsonBlock>();

        public static class JsonTransactionAction {

            //
            public String ChaincodeID;
            public String ChaincodeVersion;
            public String ChaincodeFunction;
            public String[] ChaincodeArgs;
            public String ChaincodeProposal;

        }

        public static class JsonIdemixIdentity {

            //
            public String NymX;
            public String NymY;
            public Object OU;
            public Object Proof;
            public Object Role;

        }

        public static class JsonTransactionCreator {

            //
            public String MspID;
            public Object Identity;

        }

        public static class JsonTransaction {

            //
            public String ID;
            public String Signature;
            public String Date;
            public long BlockNumber;
            public String BlockDataHash;
            public String BlockHash;
            public JsonTransactionCreator Creator;
            public List<JsonTransactionAction> Actions;

        }

        public static class JsonBlock {

            public long BlockNumber;
            public String BlockDataHash;
            public String BlockPreviousHash;
            public String BlockHash;

            public List<JsonTransaction> Transactions;

        }

        private String makeHash(byte[] hash) {

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        }

        // returns map for proto
        private Object getProtoAsJSON(Message msg) {
            try {
                return JsonParser.create().parse(JsonFormat.printer().print(msg));
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        }

        // this is not entirely correct, but works for now
        private String stringOrBinary(byte[] bytes) {
            for (byte b : bytes) {
                if (b < 0x20 || b > 0x7f) {
                    return String.format("[blob:%s]", Base64.getEncoder().encodeToString(bytes));
                }
            }

            return new String(bytes, StandardCharsets.UTF_8);
        }

        private void addBlock(long height, Block block) {

            try {

                BlockHeader hdr = block.getHeader();
                JsonBlock jblock = new JsonBlock();
                jblock.BlockNumber = hdr.getNumber();
                jblock.BlockHash = makeHash(SDKUtils.calculateBlockHash(ctx.getClient(), height, hdr.getPreviousHash().toByteArray(), hdr.getDataHash().toByteArray()));
                jblock.BlockDataHash = makeHash(hdr.getDataHash().toByteArray());
                jblock.BlockPreviousHash = makeHash(hdr.getPreviousHash().toByteArray());

                // get proposal
                BlockData bdata = block.getData();
                int bdataCount = bdata.getDataCount();
                //System.out.format("REST: Blocks reader: New block (%d, %s)%n", jblock.BlockNumber, jblock.BlockHash);

                // 
                jblock.Transactions = new ArrayList<JsonTransaction>();
                for (int i = 0; i < bdataCount; i++) {

                    // 
                    Envelope blockEnvelope = Envelope.parseFrom(bdata.getData(i));
                    byte[] sig = blockEnvelope.getSignature().toByteArray();
                    JsonTransaction tx = new JsonTransaction();
                    tx.Signature = Base64.getEncoder().encodeToString(sig);
                    Payload payload = Payload.parseFrom(blockEnvelope.getPayload());
                    Header payloadHeader = payload.getHeader();
                    SerializedIdentity creatorId = SerializedIdentity.parseFrom(SignatureHeader.parseFrom(payloadHeader.getSignatureHeader()).getCreator());

                    String mspId = creatorId.getMspid();
                    // ...are we expected to know the identity type by mspid here?..
                    JsonTransactionCreator creator = new JsonTransactionCreator();
                    creator.MspID = mspId;
                    // hack: if there is 'idemix' in MSP ID, use Idemix identity
                    if (mspId.toLowerCase().contains("idemix")) {
                        SerializedIdemixIdentity sIdentity = SerializedIdemixIdentity.parseFrom(creatorId.getIdBytes());
                        JsonIdemixIdentity niceId = new JsonIdemixIdentity();
                        niceId.NymX = Base64.getEncoder().encodeToString(sIdentity.getNymX().toByteArray());
                        niceId.NymY = Base64.getEncoder().encodeToString(sIdentity.getNymY().toByteArray());
                        niceId.Role = getProtoAsJSON(MspPrincipal.MSPRole.parseFrom(sIdentity.getRole()));
                        niceId.OU = getProtoAsJSON(MspPrincipal.OrganizationUnit.parseFrom(sIdentity.getRole()));
                        niceId.Proof = getProtoAsJSON(Idemix.Signature.parseFrom(sIdentity.getProof()));
                        creator.Identity = niceId;
                    } else {
                        creator.Identity = new String(creatorId.getIdBytes().toByteArray(), StandardCharsets.UTF_8);
                    }

                    ChannelHeader channelHeader = ChannelHeader.parseFrom(payload.getHeader().getChannelHeader());
                    tx.ID = channelHeader.getTxId();
                    tx.Creator = creator;
                    tx.BlockNumber = jblock.BlockNumber;
                    tx.BlockDataHash = jblock.BlockDataHash;
                    tx.BlockHash = jblock.BlockHash;
                    Timestamp protoTimestamp = channelHeader.getTimestamp();
                    tx.Date = Instant.ofEpochSecond(protoTimestamp.getSeconds(), protoTimestamp.getNanos()).atZone(ZoneId.of("UTC")).toString().replace("Z[UTC]", "+0000");
                    // get proposal(s)
                    // this is from Fabric SDK source
                    byte[] txDataBytes = payload.getData().toByteArray();
                    Transaction txData = Transaction.parseFrom(payload.getData());
                    List<FabricTransaction.TransactionAction> al = txData.getActionsList();
                    tx.Actions = new ArrayList<JsonTransactionAction>();
                    for (FabricTransaction.TransactionAction ta : al) {

                        ChaincodeActionPayload tap = ChaincodeActionPayload.parseFrom(ta.getPayload());//<<<
                        FabricProposal.ChaincodeProposalPayload ccpp = FabricProposal.ChaincodeProposalPayload.parseFrom(tap.getChaincodeProposalPayload());
                        Chaincode.ChaincodeInput cinputRaw = Chaincode.ChaincodeInput.parseFrom(ccpp.getInput());

                        JsonTransactionAction txAc = new JsonTransactionAction();
                        if (cinputRaw.getArgsCount() < 1)
                            continue; // some weird block

                        ChaincodeSpec cspec = ChaincodeSpec.parseFrom(cinputRaw.getArgs(0));
                        //System.out.format("spec input argsCount = %d%n", cspec.getInput().getArgsCount());
                        Chaincode.ChaincodeInput cinput = cspec.getInput();
                        int argsCount = cinput.getArgsCount();

                        txAc.ChaincodeID = cspec.getChaincodeId().getName();
                        txAc.ChaincodeVersion = cspec.getChaincodeId().getVersion();
                        txAc.ChaincodeArgs = new String[argsCount-1];
                        for (int j = 0; j < argsCount; j++) {
                            String arg = stringOrBinary(cinput.getArgs(j).toByteArray());
                            if (j < 1) {
                                txAc.ChaincodeFunction = arg;
                            } else {
                                txAc.ChaincodeArgs[j-1] = arg;
                            }
                        }
                        tx.Actions.add(txAc);

                    }

                    jblock.Transactions.add(tx);
                }

                synchronized (blocks) {
                    blocks.add(jblock);
                    blocks.sort(new Comparator<JsonBlock>() {
                        @Override
                        public int compare(JsonBlock o1, JsonBlock o2) {

                            if (o1.BlockNumber > o2.BlockNumber)
                                return -1;
                            if (o1.BlockNumber < o2.BlockNumber)
                                return 1;
                            return 0;

                        }
                    });
                }

            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

        }

        public JsonBlock getBlockByHeight(long height) {

            synchronized(blocks) {
                for (JsonBlock b : blocks) {
                    if (b.BlockNumber == height)
                        return b;
                }
            }

            return null;

        }

        public JsonBlock getBlockByHash(String hash) {

            synchronized(blocks) {
                for (JsonBlock b : blocks) {
                    if (b.BlockHash.compareToIgnoreCase(hash) == 0)
                        return b;
                }
            }

            return null;

        }

        public List<JsonBlock> getAllBlocks() {

            //
            synchronized(blocks) {
                return new LinkedList<JsonBlock>(blocks);
            }

        }

        public List<JsonTransaction> getAllTransactions() {

            //
            List<JsonTransaction> txs = new LinkedList<JsonTransaction>();
            synchronized(blocks) {
                for (JsonBlock blk : blocks) {
                    for (JsonTransaction tx : blk.Transactions) {
                        txs.add(tx);
                    }
                }
            }

            return txs;

        }

        public List<JsonBlock> getBlocks(int count) {

            //
            synchronized (blocks) {
                return blocks.subList(0, (blocks.size() > count ? count : blocks.size()));
            }

        }

        public List<JsonTransaction> getTransactions(int count) {

            //
            List<JsonTransaction> txs = new LinkedList<JsonTransaction>();
            synchronized (blocks) {
                for (JsonBlock blk : blocks) {

                    if (txs.size() >= count) {
                        break;
                    }

                    for (JsonTransaction tx : blk.Transactions) {
                        txs.add(tx);
                        if (txs.size() >= count) {
                            break;
                        }
                    }

                }
            }

            return txs;

        }

        public JsonTransaction getTransactionByHash(String hash) {

            //
            synchronized(blocks) {
                for (JsonBlock blk : blocks) {
                    for (JsonTransaction tx : blk.Transactions) {
                        if (tx.ID.compareToIgnoreCase(hash) == 0)
                            return tx;
                    }
                }
            }

            return null;

        }

        @Override
        public void run() {

            //
            System.out.format("REST: Blocks reader: Starting...%n");
            ctx = new FabricContext(false);
            System.out.format("REST: Blocks reader: Started.%n");
            //
            while (true) {

                try {

                    Thread.sleep(5000);

                    byte[] data = ctx.querySystemChaincode("qscc", "GetChainInfo", ctx.getChannel().getName());
                    BlockchainInfo info = BlockchainInfo.parseFrom(data);
                    long channelHeight = info.getHeight();
                    if (channelHeight < currentHeight) { // chain reset
                        currentHeight = 0;
                        blocks.clear();
                    }
                    boolean isJustStarted = blocks.isEmpty();
                    //System.out.format("Channel height = %d; previous = %d%n", channelHeight, currentHeight);
                    for (long i = channelHeight-1; i > currentHeight; i--) {
                        //System.out.format("Trying block at %d%n", i);
                        byte[] blockData = ctx.querySystemChaincode("qscc", "GetBlockByNumber", ctx.getChannel().getName(), Long.toString(i));
                        Block block = Block.parseFrom(blockData);
                        addBlock(i, block);
                        // after restart, don't read ALL blocks from the ledger
                        // we only need to show last 100 through REST
                        if (blocks.size() >= 100 && isJustStarted) {
                            break;
                        }
                    }

                    currentHeight = channelHeight-1;

                } catch (Throwable t) {
                    System.err.format("REST: Blocks reader: Caught Exception:%n");
                    t.printStackTrace(System.err);
                    continue;
                }

            }

        }

    }
}

<fim_middle>