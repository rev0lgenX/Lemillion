package com.revolgenx.weaverx.core.torrent.common;

import androidx.annotation.Nullable;

import java.util.List;

public interface TorrentMetadata extends TorrentHash {

    /**
     * @return all tracker for announce
     * @see <a href="http://bittorrent.org/beps/bep_0012.html"></a>
     */
    @Nullable
    List<List<String>> getAnnounceList();

    /**
     * @return main announce url for tracker or <code>null</code> if main announce is not specified
     */
    @Nullable
    String getAnnounce();

    /**
     * @return creation date of the torrent in unix format
     */
    Long getCreationDate();

    /**
     * @return free-form text comment of the author
     */
    String getComment();

    /**
     * @return name and version of the program used to create .torrent
     */
    String getCreatedBy();

    /**
     * @return number of bytes in each piece
     */
    int getPieceLength();

    /**
     * @return concatenation of all 20-byte SHA1 hash values, one per piece.
     * So the length of this array must be a multiple of 20
     */
    byte[] getPiecesHashes();

    /**
     * @return true if it's private torrent. In this case client must get peers only from tracker and
     * must initiate connections to peers returned from the tracker.
     * @see <a href="http://bittorrent.org/beps/bep_0027.html"></a>
     */
    boolean isPrivate();

    /**
     * @return count of pieces in torrent
     */
    int getPiecesCount();

    /**
     * @return The filename of the directory in which to store all the files
     */
    String getDirectoryName();

    /**
     * @return list of files, stored in this torrent
     */
    List<TorrentFile> getFiles();

    String path();

    byte[] getBlobs();
}
