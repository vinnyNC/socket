package web.database;

import java.sql.*;
import java.util.ArrayList;

public class db {
    Connection con = null;
    private boolean conStatus = false;
    public db() {

    }

    public boolean connect() {
        String url = "jdbc:sqlite:myDB.db";
        try {
            con = DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean disconnect() {
        try {
            con.close();
        } catch (Exception throwables) {
            throwables.printStackTrace();
            try {
                if (con.isClosed()) {
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean checkRoomUUID(String uuid) {
        String sql = "SELECT * FROM rooms WHERE room_uuid='" + uuid + "'";
        try {
            Statement sm = con.createStatement();
            ResultSet rs = sm.executeQuery(sql);
            while (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean checkUserUUID(String uuid) {
        String sql = "SELECT * FROM users WHERE uuid='" + uuid + "'";
        try {
            Statement sm = con.createStatement();
            ResultSet rs = sm.executeQuery(sql);
            while (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean addUserUUID(String uuid, String remoteAddress) {
        String sql = "INSERT INTO users(uuid, address) VALUES(?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, uuid);
            ps.setString(2, remoteAddress);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean createRoom(String room_uuid, String owner_uuid, String name) {
        String sql = "INSERT INTO rooms(room_uuid, owner_uuid, name) VALUES(?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, room_uuid);
            ps.setString(2, owner_uuid);
            ps.setString(3, name);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getRoomList() {
        String sql = "SELECT * FROM rooms";
        String results = "[ROOM_LIST]";

        try {
            Statement sm = con.createStatement();
            ResultSet rs = sm.executeQuery(sql);

            while (rs.next()) {
                String roomName = rs.getString("name").replace(" ", "_");
                String temp = " ['" + roomName + "','" + rs.getString("room_uuid") + "']";

                results += temp;
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return "error-EXCEPTION";
        }
    }

    public ArrayList<String> getRoomListInternal() {
        String sql = "SELECT * FROM rooms";
        ArrayList<String> results = new ArrayList<>();

        try {
            Statement sm = con.createStatement();
            ResultSet rs = sm.executeQuery(sql);

            while (rs.next()) {
                results.add(rs.getString("room_uuid"));
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getRoomOwner(String roomID) {
        String sql = "SELECT owner_uuid FROM rooms WHERE room_uuid='" + roomID + "'";

        try {
            Statement sm = con.createStatement();
            ResultSet rs = sm.executeQuery(sql);

            while (rs.next()) {
                return rs.getString("owner_uuid");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
        return "null";
    }
}
