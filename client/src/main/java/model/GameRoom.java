package model;

/**
 * Class with all info about
 * game room for game room list
 *
 * @author Eugene Lobin
 * @version 1.0 09 Mar 2018
 */
public class GameRoom {
    private String host;
    private String player;
    private String description;
    private String online;
    private String statusGame;
    private String idRoom;
    private String statusHost;
    private String statusPlayer;
    private static final String HOST_COLOR = "BLACK";
    private static final String PLAYER_COLOR = "WHITE";

    public GameRoom() {
    }

    public GameRoom(String host, String description, String idRoom) {
        this.host = host;
        this.description = description;
        online = "1/2";
        statusGame = "in lobby";
        statusPlayer = "not ready";
        this.idRoom = idRoom;
    }

    /**
     * @return host name
     */
    public String getHost() {
        return host;
    }

    /**
     * @return game description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets new game description
     *
     * @param description the new info
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return game online
     */
    public String getOnline() {
        return online;
    }

    /**
     * Sets new game online
     *
     * @param online the new info
     */
    public void setOnline(String online) {
        this.online = online + "/2";
    }

    /**
     * @return game status
     */
    public String getStatusGame() {
        return statusGame;
    }

    /**
     * Sets game status
     *
     * @param statusGame the new info
     */
    public void setStatusGame(String statusGame) {
        this.statusGame = statusGame;
    }

    /**
     * @return game id
     */
    public String getIdRoom() {
        return idRoom;
    }

    /**
     * @return host status
     */
    public String getStatusHost() {
        return statusHost;
    }

    /**
     * Sets host status
     *
     * @param statusHost the new info
     */
    public void setStatusHost(String statusHost) {
        this.statusHost = statusHost;
    }

    /**
     * @return player status
     */
    public String getStatusPlayer() {
        return statusPlayer;
    }

    /**
     * Sets player status
     *
     * @param statusPlayer the new info
     */
    public void setStatusPlayer(String statusPlayer) {
        this.statusPlayer = statusPlayer;
    }

    /**
     * @return host color
     */
    public String getHostColor() {
        return HOST_COLOR;
    }

    /**
     * @return player color
     */
    public String getPlayerColor() {
        return PLAYER_COLOR;
    }

    /**
     * Sets new player name
     *
     * @param player the new info
     */
    public void setPlayer(String player) {
        this.player = player;
    }

    /**
     * @return player name
     */
    public String getPlayer() {
        return player;
    }
}
