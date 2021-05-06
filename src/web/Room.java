package web;

import org.java_websocket.WebSocket;

import java.util.ArrayList;

public class Room {

    private String room_id;
    private String owner_id;
    private String curStatus;
    private String curTime;
    ArrayList<WebUser> users = new ArrayList<>();

    public Room(String id) {
        room_id = id;
    }

    public void addUser(WebUser u) {
        boolean status = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i) == u) {
                status = true;
            }
        }
        if (!status) {
            users.add(u);
            u.getUserSocket().send("[ROOM_CMD]['" + curStatus + "']");
            u.getUserSocket().send("[ROOM_CMD]['ROOM_CUR_TIME: " + curTime + "']");

        }
    }

    public void removeUser(String addr) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserSocket().getRemoteSocketAddress().toString().equalsIgnoreCase(addr)) {
                users.remove(i);
            }
        }
    }

    public String getRoomID() {
        return this.room_id;
    }

    public void sendChat(String msg, String author) {
        for (WebUser u: users) {
            WebSocket ws = u.getUserSocket();
            String chat = "[CHAT_MSG]['" + author + "'] " + msg;
            ws.send(chat);
        }
    }

    public void sendCMD(String cmd) {
        System.out.println(users.size() + " number of users in room " + this.room_id);
        for (WebUser u: users) {
            WebSocket ws = u.getUserSocket();
            String chat = "[ROOM_CMD]['" + cmd + "']";
            System.out.println("Sending msg to " + ws.getRemoteSocketAddress().toString());
            ws.send(chat);

            if (cmd.equalsIgnoreCase("play")) {
                curStatus = "PLAY";
            } else if (cmd.equalsIgnoreCase("pause")) {
                curStatus = "PAUSE";
            } else if (cmd.contains("ROOM_CUR_TIME")) {
                curTime = cmd.substring(cmd.indexOf(":") + 1).trim();
            }
        }
    }

    public String getCurStatus() {
        return curStatus;
    }

    public void setCurStatus(String curStatus) {
        this.curStatus = curStatus;
    }
}
