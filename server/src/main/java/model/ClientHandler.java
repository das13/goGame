package model;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import controller.Server;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;

public class ClientHandler extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class);
    private BufferedReader reader;
    private PrintWriter writer;

    private Socket clientSocket;

    private DocumentBuilder builder;
    private Transformer transformer;
    private Player currentPlayer;
    private GameRoom currentRoom;


    public ClientHandler(Socket client) {
        this.clientSocket = client;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            TransformerFactory tf = TransformerFactory.newInstance();
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
        } catch (ParserConfigurationException e) {
            LOGGER.error("ParserConfigurationException", e);
        } catch (TransformerConfigurationException e) {
            LOGGER.error("TransformerConfigurationException", e);
        }
        this.setDaemon(true);
    }

    @Override
    public void run() {
        LOGGER.info("User: " + clientSocket.getInetAddress().toString().replace("/", "") + " connected;");
        String input;
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);//send to java.client
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));//receive from java.client
            while ((input = reader.readLine()) != null) {
                Document document = builder.parse(new InputSource(new StringReader(input)));
                Node user = document.getElementsByTagName("body").item(0);

                document = builder.newDocument();
                document = createXML((Element) user, document);

                StringWriter stringWriter = new StringWriter();
                transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

                if (!document.getElementsByTagName("meta-info").item(0).getTextContent().equals("")) {
                    writer.println(stringWriter.toString());
                }

                if (document.getElementsByTagName("meta-info").item(0).getTextContent().equals("connect")) {
                    Server.writers.add(writer);
                    for (PrintWriter writer : Server.writers) {
                        for (Map.Entry<String, Player> entry : Server.userOnline.entrySet()) {
                            if (!writer.equals(this.writer)) {
                                writer.println(createXMLForUserList("online", entry.getValue()));
                            } else if (!entry.getValue().getUserName().equals(currentPlayer.getUserName())) {
                                writer.println(createXMLForUserList("online", entry.getValue()));
                            }
                        }
                        for (Map.Entry<String, GameRoom> entry : Server.gameRooms.entrySet()) {
                            writer.println(createXMLForRoomList("newGameRoom", entry.getValue()));
                        }
                    }
                } else if (document.getElementsByTagName("meta-info").item(0).getTextContent().equals("roomCreated")) {
                    for (PrintWriter writer : Server.writers) {
                        writer.println(createXMLForRoomList("newGameRoom", currentRoom));
                    }
                }
            }
        }
        catch (SocketException e){
            LOGGER.info("User disconnected");
        } catch (IOException | SAXException | TransformerException e) {
            LOGGER.error(e);
        } finally {
            try {
                Server.writers.remove(writer);
                Server.userOnline.remove(currentPlayer.getUserName());
                writer.close();
                reader.close();
                clientSocket.close();
                if (currentRoom.getGameStatus().equals("in game") && clientSocket.isConnected()) {
                    if (currentRoom.getPlayerHost().getUserName().equals(currentPlayer.getUserName())) {
                        int white = 10;
                        int black = 0;
                        changerXMLAfterGameEnd(white, black);
                        currentRoom.getPrintWriter().println(createXMLGameOver(white, black,
                                currentRoom.getPlayer().getUserName()));
                        for (PrintWriter writer : Server.writers) {
                            if (currentRoom != null) {
                                writer.println(createXMLForRoomList("closeRoom", currentRoom));
                            }
                            writer.println(createXMLForUserList("offline", currentPlayer));
                        }
                        Server.gameRooms.remove(Integer.toString(currentRoom.getRoomId()));
                    } else {
                        int white = 0;
                        int black = 10;
                        changerXMLAfterGameEnd(white, black);
                        currentRoom.getPrintWriterHost().println(createXMLGameOver(white, black,
                                currentRoom.getPlayerHost().getUserName()));
                        for (PrintWriter writer : Server.writers) {
                            writer.println(createXMLForUserList("offline", currentPlayer));
                        }
                    }
                } else if (currentRoom.getPlayerHost() == currentPlayer) {
                    Server.gameRooms.remove(Integer.toString(currentRoom.getRoomId()));
                    if (currentRoom.getRoomOnline() == 2) {
                        for (PrintWriter writer : Server.writers) {
                            if (currentRoom != null) {
                                writer.println(createXMLForRoomList("closeRoom", currentRoom));
                            }
                            writer.println(createXMLForUserList("offline", currentPlayer));
                        }
                        currentRoom.getPrintWriter().println(createXmlForHostCloseRoom());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("IOException", e);
            }
        }
    }

    private Document createXML(Element inputElement, Document outputDocument) throws IOException, SAXException {
        Element root = outputDocument.createElement("body");
        outputDocument.appendChild(root);

        Element metaElement = outputDocument.createElement("meta-info");
        root.appendChild(metaElement);

        String meta = inputElement.getElementsByTagName("meta-info").item(0).getTextContent();
        String newMeta;
        switch (meta) {
            case "login":
                newMeta = testLogin(inputElement.getElementsByTagName("login").item(0).getTextContent(),
                        inputElement.getElementsByTagName("password").item(0).getTextContent());

                metaElement.appendChild(outputDocument.createTextNode(newMeta));

                Element admin = outputDocument.createElement("admin");
                admin.appendChild(outputDocument.createTextNode(Boolean.toString(currentPlayer.isAdmin())));
                root.appendChild(admin);

                if (!newMeta.equals("incorrect") && !newMeta.equals("currentUserOnline") && !newMeta.equals("banned")) {
                    root = createUserXML(outputDocument, currentPlayer, root);
                }
                break;
            case "createRoom":
                String roomDescription = inputElement.getElementsByTagName("roomDescription").item(0).getTextContent();
                currentRoom = new GameRoom(roomDescription, currentPlayer, writer);
                currentRoom.setFieldSizeId(inputElement.getElementsByTagName("fieldSize").item(0).getTextContent());

                Server.gameRooms.put(Integer.toString(currentRoom.getRoomId()), currentRoom);
                metaElement.appendChild(outputDocument.createTextNode("roomCreated"));

                Element roomIdElement = outputDocument.createElement("roomId");
                roomIdElement.appendChild(outputDocument.createTextNode(Integer.toString(currentRoom.getRoomId())));
                root.appendChild(roomIdElement);

                Element roomDescriptionElement = outputDocument.createElement("roomDescription");
                roomDescriptionElement.appendChild(outputDocument.createTextNode(roomDescription));
                root.appendChild(roomDescriptionElement);
                break;
            case "closeRoom":
                String idRoom = inputElement.getElementsByTagName("roomId").item(0).getTextContent();
                GameRoom closeRoom = Server.gameRooms.get(idRoom);
                if (closeRoom.getGameStatus().equals("in game")) {
                    int white = 10;
                    int black = 0;
                    changerXMLAfterGameEnd(white, black);
                    for (PrintWriter writer : closeRoom.getWriters()) {
                        writer.println(createXMLGameOver(white, black, closeRoom.getPlayer().getUserName()));
                    }
                }
                Server.gameRooms.remove(idRoom);
                for (PrintWriter writer : Server.writers) {
                    writer.println(createXMLForRoomList("closeRoom", closeRoom));
                }
                if (closeRoom.getPrintWriter() != null) {
                    closeRoom.getPrintWriter().println(createXmlForHostCloseRoom());
                }
                break;
            case "changeStatus":
                String status = inputElement.getElementsByTagName("status").item(0).getTextContent();
                String roomId = inputElement.getElementsByTagName("idRoom").item(0).getTextContent();
                String playerType = inputElement.getElementsByTagName("playerType").item(0).getTextContent();
                if (playerType.equals("host")) {
                    Server.gameRooms.get(roomId).setHostStatus(status);
                } else {
                    Server.gameRooms.get(roomId).setPlayerStatus(status);
                }
                for (PrintWriter writers : Server.gameRooms.get(roomId).getWriters()) {
                    writers.println(createXMLChangeStatus(status, playerType));
                }
                break;
            case "connectToRoom":
                String id = inputElement.getElementsByTagName("idRoom").item(0).getTextContent();
                if (Server.gameRooms.get(id).getRoomOnline() != 2) {
                    metaElement.appendChild(outputDocument.createTextNode("connectionAllowed"));

                    Element fieldSizeId = outputDocument.createElement("fieldSizeId");
                    fieldSizeId.appendChild(outputDocument.createTextNode(Server.gameRooms.get(id).getFieldSizeId()));
                    root.appendChild(fieldSizeId);

                    root = createConnectAcceptXML(outputDocument, root, Server.gameRooms.get(id));

                    PrintWriter hostWriter = Server.gameRooms.get(
                            inputElement.getElementsByTagName("idRoom").item(0).getTextContent()).getPrintWriterHost();
                    hostWriter.println(createXMLForHostAfterPlayerConnect(currentPlayer.getUserName()));
                    Server.gameRooms.get(id).setPrintWriter(writer);
                    Server.gameRooms.get(id).setPlayer(currentPlayer);
                    Server.gameRooms.get(id).setRoomOnline(2);
                    for (PrintWriter writer : Server.writers) {
                        writer.println(createXMLForChangeOnlineGameRoom(Server.gameRooms.get(id).getRoomOnline(), Server.gameRooms.get(id).getRoomId()));
                    }
                    currentRoom = Server.gameRooms.get(id);
                } else {
                    metaElement.appendChild(outputDocument.createTextNode("roomFull"));
                }
                break;
            case "disconnectingFromRoom":
                GameRoom gameRoomDisconnect = Server.gameRooms.get(inputElement.getElementsByTagName("roomId").item(0).getTextContent());
                if (!gameRoomDisconnect.getGameStatus().equals("in game")) {
                    gameRoomDisconnect.setPlayer(null);
                    gameRoomDisconnect.setPrintWriter(null);
                } else {
                    int white = 0;
                    int black = 10;
                    changerXMLAfterGameEnd(white, black);
                    for (PrintWriter writer : currentRoom.getWriters()) {
                        writer.println(createXMLGameOver(white, black, currentRoom.getPlayerHost().getUserName()));
                    }
                }
                gameRoomDisconnect.setPlayerStatus(null);
                gameRoomDisconnect.setRoomOnline(1);
                PrintWriter hostWriter = Server.gameRooms.get(
                        inputElement.getElementsByTagName("roomId").item(0).getTextContent()).getPrintWriterHost();
                hostWriter.println(createXMLForHostAfterPlayerDisconnect());
                for (PrintWriter writer : Server.writers) {
                    writer.println(createXMLForChangeOnlineGameRoom(gameRoomDisconnect.getRoomOnline(), gameRoomDisconnect.getRoomId()));
                }
                break;

            case "startGame":
                Server.gameRooms.get(Integer.toString(currentRoom.getRoomId())).setGameStatus("in game");
                currentRoom.setGameStatus("in game");
                GameField gameField = currentRoom.getGameField();
                int fieldSize = Integer.parseInt(inputElement.getElementsByTagName("fieldSize").item(0).getTextContent());
                gameField.initGameField(fieldSize);
//                gameField.setStepSize(Integer.parseInt(inputElement.getElementsByTagName("stepSize").item(0).getTextContent()));
                for (PrintWriter writer : currentRoom.getWriters()) {
                    writer.println(createGameStartXML(fieldSize));
                }
                for (PrintWriter writer : Server.writers) {
                    writer.println(createXMLForChangeStatusGameRoom(currentRoom.getGameStatus(), currentRoom.getRoomId()));
                }
                break;
            case "playerMove":
                gameField = currentRoom.getGameField();
                double x = Double.parseDouble(inputElement.getElementsByTagName("xCoordinate").item(0).getTextContent());
                double y = Double.parseDouble(inputElement.getElementsByTagName("yCoordinate").item(0).getTextContent());
                String color = inputElement.getElementsByTagName("playerColor").item(0).getTextContent();
                String userName = inputElement.getElementsByTagName("userName").item(0).getTextContent();
                String secondUserName;
                if (currentRoom.getPlayerHost().getUserName().equals(userName)) {
                    secondUserName = currentRoom.getPlayer().getUserName();
                    if (currentRoom.isHostPassed()) {
                        currentRoom.setHostPassed(false);
                    }
                } else {
                    secondUserName = currentRoom.getPlayerHost().getUserName();
                    if (currentRoom.isPlayerPassed()) {
                        currentRoom.setPlayerPassed(false);
                    }
                }
                boolean result;
                if (color.equals("BLACK")) {
                    result = gameField.isAllowedToPlace(x, y, PointState.STONE_BLACK);
                } else {
                    result = gameField.isAllowedToPlace(x, y, PointState.STONE_WHITE);
                }
                if (result) {
                    for (PrintWriter writer : currentRoom.getWriters()) {
                        if (gameField.getPointsToRemove().size() > 0) {
                            writer.println(createXMLForRemoveSet(gameField.getPointsToRemove()));
                        }
                        writer.println(createXMLForSendResultToPlayer(result, x, y, color, userName, secondUserName));
                    }
                    gameField.setPointsToRemoveClear();
                }
                break;
            case "changeFieldSize":
                String buttonId = inputElement.getElementsByTagName("buttonId").item(0).getTextContent();
                if (currentRoom.getPrintWriter() != null) {
                    currentRoom.getPrintWriter().println(createXMLChangeFieldSize(buttonId));
                }
                currentRoom.setFieldSizeId(buttonId);
                break;
            case "playerPassed":
                String user = inputElement.getElementsByTagName("userName").item(0).getTextContent();
                if (currentRoom.getPlayerHost().getUserName().equals(user)) {
                    currentRoom.setHostPassed(true);
                    currentRoom.getPrintWriter().println(createXMLPlayerPass(user));
                } else {
                    currentRoom.setPlayerPassed(true);
                    currentRoom.getPrintWriterHost().println(createXMLPlayerPass(user));
                }
                if (currentRoom.isHostPassed() && currentRoom.isPlayerPassed()) {
                    Server.gameRooms.remove(Integer.toString(currentRoom.getRoomId()));
                    currentRoom.getGameField().countPlayersScore();
                    int white = currentRoom.getGameField().getWhiteCount();
                    int black = currentRoom.getGameField().getBlackCount();
                    String winName = "";
                    if (white > black) {
                        winName = currentRoom.getPlayer().getUserName();
                    } else if (black > white) {
                        winName = currentRoom.getPlayerHost().getUserName();
                    }
                    changerXMLAfterGameEnd(white, black);
                    for (PrintWriter writer : currentRoom.getWriters()) {
                        writer.println(createXMLGameOver(white, black, winName));
                    }
                    for (PrintWriter writer : Server.writers) {
                        writer.println(createXMLForRoomList("closeRoom", currentRoom));
                    }
                }
                break;
            case "banUser":
                String banUserName = inputElement.getElementsByTagName("userName").item(0).getTextContent();
                if (!banUserName.equals(currentPlayer.getUserName())) {
                    Player banUser = Server.userOnline.get(banUserName);

                    File file = new File("users" + File.separator + banUserName + ".xml");
                    Document document = builder.parse(file);

                    document.getElementsByTagName("banned").item(0).setTextContent("true");

                    DOMSource domSource = new DOMSource(document);
                    StreamResult streamResult = new StreamResult(file);
                    try {
                        transformer.transform(domSource, streamResult);
                    } catch (TransformerException e) {
                        e.printStackTrace();
                    }
                    Server.banList.add(banUserName);
                    banUser.getWriter().println(createXMLBan());
                }
                break;
            default:
                break;
        }
        return outputDocument;

    }

    private Element createUserXML(Document document, Player player, Element root) {
        Element userName = document.createElement("userName");
        userName.appendChild(document.createTextNode(player.getUserName()));
        root.appendChild(userName);

        Element userGameCount = document.createElement("userGameCount");
        userGameCount.appendChild(document.createTextNode(player.getUserGameCount()));
        root.appendChild(userGameCount);

        Element userPercentWins = document.createElement("userPercentWins");
        userPercentWins.appendChild(document.createTextNode(player.getUserPercentWins()));
        root.appendChild(userPercentWins);

        Element userRating = document.createElement("userRating");
        userRating.appendChild(document.createTextNode(player.getUserRating()));
        root.appendChild(userRating);
        return root;
    }

    private String testLogin(String login, String password) {
        String action = "connect";
        if (!Server.userList.containsKey(login)) {
            currentPlayer = createNewUser(login, password);
        } else {
            for (String name : Server.banList) {
                if (name.equals(login)) {
                    currentPlayer = Server.userList.get(login);
                    return "banned";
                }
            }
            if (!Server.userList.get(login).getUserPassword().equals(password)) {
                action = "incorrect";
            } else if (Server.userOnline.containsKey(login)) {
                action = "currentUserOnline";
            } else {
                currentPlayer = Server.userList.get(login);
                currentPlayer.setWriter(writer);
            }
        }
        if (currentPlayer != null) {
            Server.userOnline.put(currentPlayer.getUserName(), currentPlayer);
        }
        return action;
    }

    private Player createNewUser(String login, String password) {
        Player newPlayer = new Player(password, login);
        newPlayer.setWriter(writer);
        Server.userList.put(login, newPlayer);
        File file = new File("users" + File.separator + login + ".xml");
        Document doc = builder.newDocument();

        Element root = doc.createElement("body");
        doc.appendChild(root);

        Element log = doc.createElement("login");
        log.appendChild(doc.createTextNode(login));
        root.appendChild(log);

        Element pass = doc.createElement("password");
        pass.appendChild(doc.createTextNode(password));
        root.appendChild(pass);

        Element gameCount = doc.createElement("gameCount");
        gameCount.appendChild(doc.createTextNode("0"));
        root.appendChild(gameCount);

        Element rating = doc.createElement("rating");
        rating.appendChild(doc.createTextNode("100"));
        root.appendChild(rating);

        Element percentWins = doc.createElement("percentWins");
        percentWins.appendChild(doc.createTextNode("0"));
        root.appendChild(percentWins);

        Element winGames = doc.createElement("winGames");
        winGames.appendChild(doc.createTextNode("0"));
        root.appendChild(winGames);

        Element admin = doc.createElement("admin");
        admin.appendChild(doc.createTextNode("false"));
        root.appendChild(admin);

        Element banned = doc.createElement("banned");
        banned.appendChild(doc.createTextNode("false"));
        root.appendChild(banned);
        try {
            transformer.transform(new DOMSource(doc), new StreamResult(file));
        } catch (TransformerException e) {
            LOGGER.error("TransformerException", e);
        }
        return newPlayer;
    }

    private String createXMLForUserList(String action, Player player) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode(action));
        root.appendChild(meta);

        if (action.equals("online")) {
            root = createUserXML(document, player, root);
        } else if (action.equals("offline")) {
            Element userName = document.createElement("userName");
            userName.appendChild(document.createTextNode(player.getUserName()));
            root.appendChild(userName);
        }
        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLForRoomList(String action, GameRoom room) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode(action));
        root.appendChild(meta);

        Element roomHost = document.createElement("roomHost");
        roomHost.appendChild(document.createTextNode(room.getPlayerHost().getUserName()));
        root.appendChild(roomHost);

        Element roomDescription = document.createElement("roomDescription");
        roomDescription.appendChild(document.createTextNode(room.getRoomDescription()));
        root.appendChild(roomDescription);

        Element roomId = document.createElement("roomId");
        roomId.appendChild(document.createTextNode(Integer.toString(room.getRoomId())));
        root.appendChild(roomId);

        Element roomOnline = document.createElement("roomOnline");
        roomOnline.appendChild(document.createTextNode(Integer.toString(room.getRoomOnline())));
        root.appendChild(roomOnline);

        Element gameStatus = document.createElement("gameStatus");
        gameStatus.appendChild(document.createTextNode(room.getGameStatus()));
        root.appendChild(gameStatus);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLChangeStatus(String status, String playerType) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("changeStatus"));
        root.appendChild(meta);

        Element statusElement = document.createElement("status");
        statusElement.appendChild(document.createTextNode(status));
        root.appendChild(statusElement);

        Element playerTypeElement = document.createElement("playerType");
        playerTypeElement.appendChild(document.createTextNode(playerType));
        root.appendChild(playerTypeElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private Element createConnectAcceptXML(Document document, Element root, GameRoom gameRoom) {
        Element hostName = document.createElement("hostName");
        hostName.appendChild(document.createTextNode(gameRoom.getPlayerHost().getUserName()));
        root.appendChild(hostName);

        Element hostStatus = document.createElement("hostStatus");
        hostStatus.appendChild(document.createTextNode(gameRoom.getHostStatus()));
        root.appendChild(hostStatus);

        Element roomDescription = document.createElement("roomDescription");
        roomDescription.appendChild(document.createTextNode(gameRoom.getRoomDescription()));
        root.appendChild(roomDescription);

        Element roomId = document.createElement("roomId");
        roomId.appendChild(document.createTextNode(Integer.toString(gameRoom.getRoomId())));
        root.appendChild(roomId);
        return root;
    }

    private String createXMLForHostAfterPlayerConnect(String playerName) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("playerConnectToRoom"));
        root.appendChild(meta);

        Element playerNameElement = document.createElement("playerName");
        playerNameElement.appendChild(document.createTextNode(playerName));
        root.appendChild(playerNameElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLForChangeOnlineGameRoom(int online, int roomId) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("changeOnline"));
        root.appendChild(meta);

        Element playerOnline = document.createElement("playerOnline");
        playerOnline.appendChild(document.createTextNode(Integer.toString(online)));
        root.appendChild(playerOnline);

        Element roomIdElement = document.createElement("roomId");
        roomIdElement.appendChild(document.createTextNode(Integer.toString(roomId)));
        root.appendChild(roomIdElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLForChangeStatusGameRoom(String status, int roomId) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("changeStatusGameRoom"));
        root.appendChild(meta);

        Element statusElement = document.createElement("status");
        statusElement.appendChild(document.createTextNode(status));
        root.appendChild(statusElement);

        Element roomIdElement = document.createElement("roomId");
        roomIdElement.appendChild(document.createTextNode(Integer.toString(roomId)));
        root.appendChild(roomIdElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLForHostAfterPlayerDisconnect() {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("playerDisconnect"));
        root.appendChild(meta);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXmlForHostCloseRoom() {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("hostCloseRoom"));
        root.appendChild(meta);

        currentRoom = null;

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createGameStartXML(int side) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("startGame"));
        root.appendChild(meta);

        Element sideElement = document.createElement("side");
        sideElement.appendChild(document.createTextNode(Integer.toString(side)));
        root.appendChild(sideElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLForSendResultToPlayer(boolean res, double x, double y, String color,
                                                  String userName, String unblockUserName) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("resultMove"));
        root.appendChild(meta);

        Element result = document.createElement("result");
        result.appendChild(document.createTextNode(Boolean.toString(res)));
        root.appendChild(result);

        Element xCoordinate = document.createElement("xCoordinate");
        xCoordinate.appendChild(document.createTextNode(Double.toString(x)));
        root.appendChild(xCoordinate);

        Element yCoordinate = document.createElement("yCoordinate");
        yCoordinate.appendChild(document.createTextNode(Double.toString(y)));
        root.appendChild(yCoordinate);

        Element playerColor = document.createElement("playerColor");
        playerColor.appendChild(document.createTextNode(color));
        root.appendChild(playerColor);

        Element blockUser = document.createElement("blockUser");
        blockUser.appendChild(document.createTextNode(userName));
        root.appendChild(blockUser);

        Element unblockUser = document.createElement("unblockUser");
        unblockUser.appendChild(document.createTextNode(unblockUserName));
        root.appendChild(unblockUser);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLForRemoveSet(Set<Point> set) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("removePoint"));
        root.appendChild(meta);

        Element pointSet = document.createElement("pointSet");
        root.appendChild(pointSet);

        int id = 1;
        for (Point temp : set) {
            Element coordinate = document.createElement("coordinate");
            coordinate.setAttribute("id", Integer.toString(id++));
            coordinate.setAttribute("xCoordinate", Double.toString(temp.getX()));
            coordinate.setAttribute("yCoordinate", Double.toString(temp.getY()));
            pointSet.appendChild(coordinate);
        }

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        System.out.println(stringWriter.toString());
        return stringWriter.toString();
    }

    private String createXMLChangeFieldSize(String id) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("changeFieldSize"));
        root.appendChild(meta);

        Element radioButtonId = document.createElement("radioButtonId");
        radioButtonId.appendChild(document.createTextNode(id));
        root.appendChild(radioButtonId);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLPlayerPass(String userName) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("playerPassed"));
        root.appendChild(meta);

        Element userNameElement = document.createElement("userName");
        userNameElement.appendChild(document.createTextNode(userName));
        root.appendChild(userNameElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLGameOver(int white, int black, String userName) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("gameOver"));
        root.appendChild(meta);

        Element whiteElement = document.createElement("white");
        whiteElement.appendChild(document.createTextNode(Integer.toString(white)));
        root.appendChild(whiteElement);

        Element blackElement = document.createElement("black");
        blackElement.appendChild(document.createTextNode(Integer.toString(black)));
        root.appendChild(blackElement);

        Element userNameElement = document.createElement("userName");
        userNameElement.appendChild(document.createTextNode(userName));
        root.appendChild(userNameElement);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private String createXMLBan() {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("ban"));
        root.appendChild(meta);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    private void changerXMLAfterGameEnd(int white, int black) {
        int res;
        String hostName = currentRoom.getPlayerHost().getUserName();
        String userName = currentRoom.getPlayer().getUserName();
        if (white > black) {
            res = white - black;
            setNewInfoAboutUser(userName, res);
            setNewInfoAboutUser(hostName, -res);
        } else if (black > white) {
            res = black - white;
            setNewInfoAboutUser(hostName, res);
            setNewInfoAboutUser(userName, -res);
        }
    }

    private void setNewInfoAboutUser(String name, int res) {
        try {
            File file = new File("users" + File.separator + name + ".xml");
            Document document = builder.parse(file);
            int gameCount = Integer.parseInt(document.getElementsByTagName("gameCount").item(0).getTextContent());
            document.getElementsByTagName("gameCount").item(0).setTextContent(Integer.toString(++gameCount));

            int rating = Integer.parseInt(document.getElementsByTagName("rating").item(0).getTextContent());
            rating += res;
            document.getElementsByTagName("rating").item(0).setTextContent(Integer.toString(rating));

            int winGames = Integer.parseInt(document.getElementsByTagName("winGames").item(0).getTextContent());
            if (res > 0) {
                document.getElementsByTagName("winGames").item(0).setTextContent(Integer.toString(++winGames));
            }
            double percentWins = winGames * 100 / gameCount;
            document.getElementsByTagName("percentWins").item(0).setTextContent(Double.toString(percentWins));

            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(file);
            try {
                transformer.transform(domSource, streamResult);
            } catch (TransformerException e) {
                e.printStackTrace();
            }
            for (PrintWriter writer : Server.writers) {
                Player player = Server.userList.get(name);
                player.setUserGameCount(Integer.toString(gameCount));
                player.setUserRating(Integer.toString(rating));
                player.setUserWinGames(Integer.toString(winGames));
                player.setUserPercentWins(Double.toString(percentWins));
                writer.println(createXMLForNewUserInfo(player));
            }
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private String createXMLForNewUserInfo(Player player) {
        Document document = builder.newDocument();

        Element root = document.createElement("body");
        document.appendChild(root);

        Element meta = document.createElement("meta-info");
        meta.appendChild(document.createTextNode("newUserInfo"));
        root.appendChild(meta);

        Element name = document.createElement("userName");
        name.appendChild(document.createTextNode(player.getUserName()));
        root.appendChild(name);

        Element gameCount = document.createElement("gameCount");
        gameCount.appendChild(document.createTextNode(player.getUserGameCount()));
        root.appendChild(gameCount);

        Element rating = document.createElement("rating");
        rating.appendChild(document.createTextNode(player.getUserRating()));
        root.appendChild(rating);

        Element percentWins = document.createElement("percentWins");
        percentWins.appendChild(document.createTextNode(player.getUserPercentWins()));
        root.appendChild(percentWins);

        StringWriter stringWriter = new StringWriter();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }
}

