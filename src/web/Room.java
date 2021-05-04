package web;

import org.java_websocket.WebSocket;

import java.util.ArrayList;

public class Room {

    private String room_id;
    ArrayList<WebUser> users = new ArrayList<>();

    public Room(String id) {
        room_id = id;
    }

    public boolean addUser(WebUser u) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i) != u) {
                users.add(u);
                return true;
            }
        }
        return false;
    }

    public void removeUser(WebUser u) {
        users.remove(u);
    }

    public String getRoomID() {
        return this.room_id;
    }

    public void sendChat(String msg, String author) {
        for (WebUser u: users) {
            WebSocket ws = u.getUserSocket();
            String chat = "[CHAT_MSG]['" + author + "', '" + msg + "']";
        }
    }

}
