package com.revolgenx.weaverx.core.torrent.common;

import androidx.annotation.Nullable;

import java.util.List;


public class TorrentMetadataImpl implements TorrentMetadata {

    private final byte[] myInfoHash;
    @Nullable
    private final List<List<String>> myAnnounceList;
    private final String myMainAnnounce;
    private final long myCreationDate;
    private final String myComment;
    private final String myCreatedBy;
    private final String myName;
    private final List<TorrentFile> myFiles;
    private final int myPieceCount;
    private final int myPieceLength;
    private final byte[] myPiecesHashes;
    private final String myHexString;
    private final String path;
    private final byte[] blobs;

    TorrentMetadataImpl(byte[] infoHash,
                        @Nullable List<List<String>> announceList,
                        String mainAnnounce,
                        long creationDate,
                        String comment,
                        String createdBy,
                        String name,
                        List<TorrentFile> files,
                        int pieceCount,
                        int pieceLength,
                        byte[] piecesHashes,
                        String path,
                        byte[] meta) {
        myInfoHash = infoHash;
        myAnnounceList = announceList;
        myMainAnnounce = mainAnnounce;
        myCreationDate = creationDate;
        myComment = comment;
        myCreatedBy = createdBy;
        myName = name;
        myFiles = files;
        myPieceCount = pieceCount;
        myPieceLength = pieceLength;
        myPiecesHashes = piecesHashes;
        myHexString = TorrentUtils.byteArrayToHexString(myInfoHash);
        this.path = path;
        blobs = meta;
    }

    @Override
    public String getDirectoryName() {
        return myName;
    }

    @Override
    public List<TorrentFile> getFiles() {
        return myFiles;
    }

    @Nullable
    @Override
    public List<List<String>> getAnnounceList() {
        return myAnnounceList;
    }

    @Nullable
    @Override
    public String getAnnounce() {
        return myMainAnnounce;
    }

    @Override
    public Long getCreationDate() {
        return myCreationDate == -1 ? null : myCreationDate;
    }

    @Override
    public String getComment() {
        return myComment;
    }

    @Override
    public String getCreatedBy() {
        return myCreatedBy;
    }

    @Override
    public int getPieceLength() {
        return myPieceLength;
    }

    @Override
    public byte[] getPiecesHashes() {
        return myPiecesHashes;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public int getPiecesCount() {
        return myPieceCount;
    }

    @Override
    public byte[] getInfoHash() {
        return myInfoHash;
    }

    @Override
    public String getHexInfoHash() {
        return myHexString;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public byte[] getBlobs() {
        return blobs;
    }
}