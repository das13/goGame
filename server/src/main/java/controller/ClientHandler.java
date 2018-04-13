package controller;

import model.XMLGenerator;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Handler class for create new thread when user connect
 * on server part
 *
 * @author Eugene Lobin
 * @version 1.0 09 Mar 2018
 */
public class ClientHandler extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class);
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket clientSocket;
    private XMLGenerator xmlGenerator;

    ClientHandler(Socket client) {
        xmlGenerator = new XMLGenerator();
        this.clientSocket = client;
        this.setDaemon(true);
    }

    /**
     * Starts when user connecting to server.
     * Reads xml from user and send in main method
     * createXML()
     */
    @Override
    public void run() {
        LOGGER.info("User: " + clientSocket.getInetAddress().toString().replace("/", "") + " connected;");
        String input;
        try {
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            xmlGenerator.setWriter(writer);
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while ((input = reader.readLine()) != null) {
                if (input.equals("stop") || input.equals("restart")) {
                    ServerCommand serverCommand = Server.getCommand(input);
                    LOGGER.info("User disconnected");
                    Server.handleCommand(serverCommand);
                } else {
                    xmlGenerator.readInput(input);
                }
            }
        } catch (SocketException e) {
            LOGGER.info("User disconnected");
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            xmlGenerator.checkAfterPlayerExit();
            writer.close();
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error("IOException", e);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.error("IOException", e);
            }
        }
    }
}
