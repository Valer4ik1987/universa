/*
 * Copyright (c) 2017 Sergey Chernov, iCodici S.n.C, All Rights Reserved
 *
 * Written by Sergey Chernov <real.sergeych@gmail.com>
 *
 */

package com.icodici.universa.node2.network;

import com.icodici.crypto.EncryptionError;
import com.icodici.crypto.PrivateKey;
import com.icodici.crypto.SymmetricKey;
import com.icodici.universa.node2.NodeInfo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Adapter to the Universa Node UDP protocol v2.
 *
 * Protocol description: https://docs.google.com/document/d/1vlRQu5pcqWlOUT3yzBEKTq95jM8XlRutP-tR-aoUUiI/edit?usp=sharing
 *
 *
 */

public abstract class DatagramAdapter {

    /**
     * the queue where to put incoming data
     */
    BlockingQueue<byte[]> inputQueue = new LinkedBlockingQueue<>();

    /**
     * Maximum packet size in bytes. Adapter should try to send several blocks together as long as the overall encoded
     * packet sie is no more than MAX_PACKET_SIZE with all extra data attached.
     */
    static public final int MAX_PACKET_SIZE = 512;

    /**
     * Max number of attempts to retransmit a block, defaults to 10
     */
    static public final int RETRANSMIT_MAX_ATTEMPTS = 10;

    /**
     * Time between attempts to retransmit a DATA block, in milliseconds
     */
    static public final int RETRANSMIT_TIME = 10000;

    protected NodeInfo myNodeInfo;
    protected Consumer<byte[]> receiver = null;
    protected final SymmetricKey sessionKey;
    protected final PrivateKey ownPrivateKey;

    protected int testMode = TestModes.NONE;

    /**
     * Create an instance that listens for the incoming datagrams using the specified configurations. The adapter should
     * start serving incoming datagrams immediately upon creation.
     *
     * @param myNodeInfo
     */
    public DatagramAdapter(PrivateKey ownPrivateKey, SymmetricKey sessionKey, NodeInfo myNodeInfo) {
        this.myNodeInfo = myNodeInfo;
        this.sessionKey = sessionKey;
        this.ownPrivateKey = ownPrivateKey;
    }

    public abstract void send(NodeInfo destination, byte[] payload) throws EncryptionError, InterruptedException;


    /**
     * Close socket and stop threads/
     */
    public abstract void shutdown();

    public void receive(Consumer<byte[]> receiver) {
        byte[] payload;
        // first set the receiver so the queue won't be grow
        // the order does not matter anyway
        this.receiver = receiver;
        // now let's drain the buffer
        while((payload = inputQueue.poll()) != null ) {
            receiver.accept(payload);
        }
    }

    public void seTestMode(int testMode) {
        this.testMode = testMode;
    }


    public class TestModes
    {
        static public final int NONE =              0;
        static public final int LOST_PACKETS =      1;
        static public final int SHUFFLE_PACKETS =   2;
    }
}
