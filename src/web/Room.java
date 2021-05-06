package web;

import org.java_websocket.WebSocket;

import java.util.ArrayList;

public class Room {

    private String room_id;
    private String owner_id;
    private String curStatus;
    private String curTime;
    private String curSource;

    ArrayList<WebUser> users = new ArrayList<>();

    public Room(String id) {
        room_id = id;
        curStatus = "PLAY";
        curTime = "0";
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
            u.getUserSocket().send("[ROOM_CMD]['src:  " + curSource + "']");
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
        String arg = null;

        if (cmd.contains(":")) {
            arg = cmd.substring(cmd.indexOf(":") + 1).trim();
        }

        if (cmd.equalsIgnoreCase("play")) {
            curStatus = "PLAY";
        } else if (cmd.equalsIgnoreCase("pause")) {
            curStatus = "PAUSE";
        } else if (cmd.contains("ROOM_CUR_TIME")) {
            curTime = arg;
        } else if (cmd.contains("src")) {
            setCurSource(arg);
            curStatus = "PLAY";
            curTime = "0";
        }

        for (WebUser u: users) {
            WebSocket ws = u.getUserSocket();
            String chat = "[ROOM_CMD]['" + cmd + "']";
            ws.send(chat);
        }
    }

    public String getCurStatus() {
        return curStatus;
    }

    public void setCurStatus(String curStatus) {
        this.curStatus = curStatus;
    }

    public String getCurSource() {
        return curSource;
    }

    public void setCurSource(String src) {
        System.out.println("Changing src... " + src);
        this.curSource = src;
    }
}
