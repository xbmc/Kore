package org.xbmc.kore.testutils.tcpserver.handlers;

import org.xbmc.kore.testutils.tcpserver.handlers.jsonrpc.response.methods.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaylistHolder {
    private int id;
    private List<Player.GetItem> items = new ArrayList<>();
    private int currentIndex;

    PlaylistHolder(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void clear() {
        id = 0;
        currentIndex = 0;
        items.clear();
    }

    public void add(Player.GetItem item) {
        items.add(item);
    }

    public List<Player.GetItem> getItems() {
        return items;
    }

    public int getIndexOf(Player.GetItem item) {
        return items.indexOf(item);
    }

    public Player.GetItem getCurrentItem() {
        return items.get(currentIndex);
    }

    public int getPlaylistSize() {
        return items.size();
    }

    public void setPlaylistIndex(int index) {
        currentIndex = index;

        if (currentIndex < 0)
            currentIndex = 0;
        else if (currentIndex >= items.size())
            currentIndex = getPlaylistSize() - 1;
    }
}
