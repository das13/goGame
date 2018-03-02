package model;

public class GameRoom {
    private String host;
    private String description;
    private String online;
    private String statusGame;
    private String idRoom;
    private String statusHost;
    private String statusPlayer;

    public GameRoom() {
    }

    public GameRoom(String host, String description,String idRoom) {
        this.host = host;
        this.description = description;
        online = "1/2";
        statusGame = "In lobby";
        this.idRoom = idRoom;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online + "/2";
    }

    public String getStatusGame() {
        return statusGame;
    }

    public void setStatusGame(String statusGame) {
        this.statusGame = statusGame;
    }


    public String getIdRoom() {
        return idRoom;
    }

    public String getStatusHost() {
        return statusHost;
    }

    public void setStatusHost(String statusHost) {
        this.statusHost = statusHost;
    }

    public String getStatusPlayer() {
        return statusPlayer;
    }

    public void setStatusPlayer(String statusPlayer) {
        this.statusPlayer = statusPlayer;
    }
}