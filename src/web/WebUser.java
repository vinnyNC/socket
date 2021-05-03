package web;

import org.java_websocket.WebSocket;

public class WebUser {

    private String userUUID = null;
    private WebSocket userSocket = null;
    private String curRoom = null;
    private String username = null;

    public WebUser() {

    }

    public WebUser(String UUID, WebSocket socket) {
        this.userUUID = UUID;
        this.userSocket = socket;
    }

    public String getCurRoom() {
        return curRoom;
    }

    public void setCurRoom(String curRoom) {
        this.curRoom = curRoom;
    }

    public WebSocket getUserSocket() {
        return userSocket;
    }

    public void setUserSocket(WebSocket userSocket) {
        this.userSocket = userSocket;
    }

    public String getUUID() {
        return userUUID;
    }

    public void setUUID(String UUID) {
        this.userUUID = UUID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
