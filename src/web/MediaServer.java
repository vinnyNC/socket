package web;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import web.database.db;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MediaServer extends WebSocketServer {

     static final int LOG_LEVEL = 3;
     private db myDB = new db();
     private ArrayList<WebUser> userList = new ArrayList<>();
     private ArrayList<Room> roomList = new ArrayList<>();
     private static WebSocketServer server = null;

    public MediaServer(InetSocketAddress address) {
        super(address);
    }

    public static void main(String[] args) {
        String host = "::";
        int port = 8887;

        server = new MediaServer(new InetSocketAddress(host, port));
        server.run();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        myLog("New Connection: " + webSocket.getRemoteSocketAddress(), 2);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        for (int j = 0; j < userList.size(); j++) {
            WebUser ws = userList.get(j);
            if (webSocket.getRemoteSocketAddress().toString().equalsIgnoreCase(ws.getUserSocket().getRemoteSocketAddress().toString())) {
                userList.remove(j);
                j = userList.size() + 1;
            }
        }
        myLog("Connection Closed: " + webSocket.getRemoteSocketAddress() + " - Code: " + i + " - Reason: " + s, 2);

        for (int j = 0; j < roomList.size(); j++) {
            roomList.get(j).removeUser(webSocket.getRemoteSocketAddress().toString());
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        String command = s.substring(0, s.indexOf(":"));
        String args = s.substring(s.indexOf(":") + 2);
        myLog(("[CMD RECEIVED][" + webSocket.getRemoteSocketAddress().toString() + "] MSG: " + command + " || ARGS: " + args), 3);
        if (command.equalsIgnoreCase("UUID")) {
            if (args.equalsIgnoreCase("generate")) {
                String uuid = genUserUUIDString();
                WebUser newUser = new WebUser(uuid, webSocket);
                userList.add(newUser);
                myDB.addUserUUID(newUser.getUUID(), newUser.getUserSocket().getRemoteSocketAddress().toString());
                myLog("[NEW USER] " + newUser.getUUID() + " - " + newUser.getUserSocket().getRemoteSocketAddress(), 1);
                webSocket.send("ASSIGN_USER_UUID: " + newUser.getUUID());
            } else {
                String uuid = args;
                WebUser newUser = new WebUser(uuid, webSocket);
                userList.add(newUser);
                myLog("[OLD USER] " + newUser.getUUID() + " - " + newUser.getUserSocket().getRemoteSocketAddress(), 1);
            }
        } else if (command.equalsIgnoreCase("SET_USERNAME")) {
            //SET_USERNAME: (UUID), USERNAME
            String pUUID = args.substring(0, args.indexOf(","));
            String pUser = args.substring(args.indexOf(",") + 2);
            myLog("[USERNAME SET] UUID: " + pUUID + " || WebSocket: " + webSocket.getRemoteSocketAddress().toString() + " || Username: " + pUser, 2);
            for (int i = 0; i < userList.size(); i++) {
                String temp_uuid = userList.get(i).getUUID();
                if (temp_uuid.equalsIgnoreCase(pUUID)) {
                    userList.get(i).setUsername(pUser);
                }
            }
        } else if (command.equalsIgnoreCase("ROOMS")) {
            if (args.equalsIgnoreCase("GET_LIST")) {
                myLog("Sending room list to " + webSocket.getRemoteSocketAddress().toString(), 3);
                webSocket.send(myDB.getRoomList());
            }
        } else if (command.equalsIgnoreCase("JOIN_ROOM")) {
            checkRoomExists(args);
            WebUser tUser = getUser(webSocket.getRemoteSocketAddress());
            userJoinRoom(tUser, args);
        } else if (command.equalsIgnoreCase("ROOM_OWNER_ACTION")) {
            String roomID = args.substring(1, args.indexOf("]"));
            String cmd = args.substring(args.indexOf("]") + 1);
            if (cmd.equalsIgnoreCase("PAUSE")) {
                roomAction(roomID, "PAUSE");
            } else if (cmd.equalsIgnoreCase("PLAY")) {
                roomAction(roomID, "PLAY");
            } else if (cmd.contains("TIME")) {
                String curTime = cmd.substring(cmd.indexOf("=") + 1);
                roomAction(roomID, "ROOM_CUR_TIME: " + curTime);
            }
        } else if (command.equalsIgnoreCase("CHAT_MSG")) {
            String roomID = args.substring(args.indexOf("[") + 2, args.indexOf("]") - 1);
            String msg = args.substring(args.indexOf("]") + 1);
            WebUser author = getUser(webSocket.getRemoteSocketAddress());
            if (msg.trim().startsWith("/source")) {
                if (author.getUUID().equalsIgnoreCase(myDB.getRoomOwner(roomID))) {
                    String url = msg.substring(8);
                    roomAction(roomID, "src: " + url);
                }
            } else {
                roomSendChat(roomID, msg, author.getUUID());
            }
        } else if (command.equalsIgnoreCase("JOIN_REQ")) {
            //webSocket.send("JOIN_REQ_RESP: " + checkRoomExists(args));
            boolean foundRoom =  false;
            for (int i = 0; i < roomList.size(); i++) {
                if (roomList.get(i).getRoomID().equalsIgnoreCase(args.trim())) {
                    webSocket.send("JOIN_REQ_RESP: true");
                    foundRoom = true;
                }
            }
            if (!foundRoom) {
                webSocket.send("JOIN_REQ_RESP: false");
            }
        }else if (command.equalsIgnoreCase("CREATE_ROOM")) {
            String room_uuid = genRoomUUIDString().trim();
            myDB.createRoom(room_uuid, getUser(webSocket.getRemoteSocketAddress()).getUUID(), args);
            System.out.println("TRY: " + room_uuid);
            webSocket.send("CREATE_ROOM_SUCCESS: " + room_uuid);
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        myLog("ERROR: " + e.getMessage(), 1);
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        myLog("Server Started (" + server.getAddress().toString() + ")", 1);
        myLog("DB Status: " + myDB.connect(), 1);
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);

        ArrayList<String> res = myDB.getRoomListInternal();
        for (String r: res) {
            checkRoomExists(r);
        }
    }

    public void roomAction(String roomID, String msg) {
        Room temp;
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomID().equalsIgnoreCase(roomID)) {
                roomList.get(i).sendCMD(msg);
            }
        }
    }

    public void roomSendChat(String roomID, String msg, String author) {
        Room temp;
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomID().equalsIgnoreCase(roomID)) {
                roomList.get(i).sendChat(msg, author);
            }
        }
    }

    public static void myLog(String msg, int level) {
        SimpleDateFormat newDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        String iDate = newDate.format(new Date());
        if (LOG_LEVEL == 3) {
            System.out.println("[" + iDate + "] " + msg);
        } else if (LOG_LEVEL == 2 && level < 3) {
            System.out.println("[" + iDate + "] " + msg);
        } else if (LOG_LEVEL == 1 && level < 2) {
            System.out.println("[" + iDate + "] " + msg);
        }
    }

    public String genUserUUIDString() {
        boolean status = false;
        String saltStr = "";
        while (!status) {
            saltStr = "";
            String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
            StringBuilder salt = new StringBuilder();
            Random rnd = new Random();
            while (salt.length() < 18) { // length of the random string.
                int index = (int) (rnd.nextFloat() * SALTCHARS.length());
                salt.append(SALTCHARS.charAt(index));
            }
            saltStr = salt.toString();
            if (!myDB.checkUserUUID(saltStr)) {
                status = true;
            }
        }
        return saltStr;
    }

    public String genRoomUUIDString() {
        boolean status = false;
        String saltStr = "";
        while (!status) {
            saltStr = "";
            String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
            StringBuilder salt = new StringBuilder();
            Random rnd = new Random();
            while (salt.length() < 18) { // length of the random string.
                int index = (int) (rnd.nextFloat() * SALTCHARS.length());
                salt.append(SALTCHARS.charAt(index));
            }
            saltStr = salt.toString();
            if (!myDB.checkRoomUUID(saltStr)) {
                status = true;
            }
        }
        return saltStr;
    }

    public WebUser getUser(InetSocketAddress addr) {
        myLog("Searching for user matching address: " + addr.toString(), 3);
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUserSocket().getRemoteSocketAddress() == addr) {
                return userList.get(i);
            }
        }
        return null;
    }

    public void userJoinRoom(WebUser wu, String roomID) {
        myLog("Adding user (" + wu.getUUID() + ") to room (" + roomID + ")", 2);
        String owner = myDB.getRoomOwner(roomID);
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomID().equalsIgnoreCase(roomID)) {
                roomList.get(i).addUser(wu);
            }
        }

        if (wu.getUUID().equalsIgnoreCase(owner)) {
            myLog("Sending owner message", 3);
            wu.getUserSocket().send("isOwner");
        }
    }

    public boolean checkRoomExists(String roomID) {
        myLog("Checking if room exists (" + roomID + ")", 2);
        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).getRoomID().equalsIgnoreCase(roomID)) {
                return true;
            }
        }
        roomList.add(new Room(roomID));
        return false;
    }
}
