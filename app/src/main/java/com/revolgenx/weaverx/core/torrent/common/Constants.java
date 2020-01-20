package com.revolgenx.weaverx.core.torrent.common;

import java.nio.ByteBuffer;

public class Constants {
    public static final int DEFAULT_ANNOUNCE_INTERVAL_SEC = 15;

    public final static int DEFAULT_SOCKET_CONNECTION_TIMEOUT_MILLIS = 100000;
    public static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10000;

    public static final int DEFAULT_MAX_CONNECTION_COUNT = 100;

    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    public static final int DEFAULT_SELECTOR_SELECT_TIMEOUT_MILLIS = 10000;
    public static final int DEFAULT_CLEANUP_RUN_TIMEOUT_MILLIS = 120000;

    public static final String BYTE_ENCODING = "ISO-8859-1";

    public static final int PIECE_HASH_SIZE = 20;

}