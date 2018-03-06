package model;

import java.io.PrintWriter;

public class Player {
    private String userName;
    private String userPassword;
    private String userGameCount;
    private String userRating;
    private String userPercentWins;
    private boolean admin = false;
    private PrintWriter writer;

    public Player() {
    }

    public Player(String userPassword, String userName) {
        this.userName = userName;
        this.userPassword = userPassword;
        userGameCount = "0";
        userRating = "0";
        userPercentWins = "0";
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserGameCount() {
        return userGameCount;
    }

    public void setUserGameCount(String userGameCount) {
        this.userGameCount = userGameCount;
    }

    public String getUserRating() {
        return userRating;
    }

    public void setUserRating(String userRating) {
        this.userRating = userRating;
    }

    public String getUserPercentWins() {
        return userPercentWins;
    }

    public void setUserPercentWins(String userPercentWins) {
        this.userPercentWins = userPercentWins;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public PrintWriter getWriter() {
        return writer;
    }
}
