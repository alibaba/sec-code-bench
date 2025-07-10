<filename>src/com/kevinychen/chess/main/util/NetworkHandler.java<fim_prefix>

package com.kevinychen.chess.main.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkHandler implements Runnable {

    private GameModel gameModel;
    private Preferences preferences;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private boolean dispatchEnabled;

    public NetworkHandler(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    @Override
    public void run() {
        initialize();
        if (handshake()) {
            gameModel.onHandshake();
            new Thread(new MessageDispatch()).start();
        }
    }

    public synchronized boolean sendMoveMessage(Move move) {
        dispatchEnabled = false;
        NetworkMessage moveMessage = new NetworkMessage(NetworkMessage.Type.MOVE);
        moveMessage.setMove(move.getOriginFile(), move.getOriginRank(), move.getDestinationFile(), move.getDestinationRank());
        try {
            outputStream.writeObject(moveMessage.toString());
            outputStream.flush();
        } catch (IOException e) {
            // notify GameModel
            // end connection
            dispatchEnabled = true;
            return false;
        }

        NetworkMessage responseMoveMessage = null;
        try {
            responseMoveMessage = new NetworkMessage((String) inputStream.readObject());
        } catch (IOException | ClassNotFoundException e) {
            // notify GameModel
            // end connection
            dispatchEnabled = true;
            return false;
        }
        if (NetworkMessage.Type.MOVE_RESPONSE.equals(responseMoveMessage.getType())) {
            dispatchEnabled = true;
            return responseMoveMessage.isMoveValid();
        } else {
            dispatchEnabled = true;
            return false;
        }
    }

    private synchronized void onMoveMessage(NetworkMessage moveMessage) {
        dispatchEnabled = false;
        System.out.println("MoveMessage - inbound: received");
        NetworkMessage responseMoveMessage = new NetworkMessage(NetworkMessage.Type.MOVE_RESPONSE);
        if (gameModel.onInboundRemoteMoveRequest(moveMessage.getOriginFile(), moveMessage.getOriginRank(), moveMessage.getDestinationFile(), moveMessage.getDestinationRank())) {
            responseMoveMessage.setMoveValid(true);
        } else {
            responseMoveMessage.setMoveValid(false);
        }

        try {
            outputStream.writeObject(responseMoveMessage.toString());
            outputStream.flush();
            dispatchEnabled = true;
        } catch (IOException e) {
            // notify GameModel
            // end connection
        }
    }

    public boolean onUndoMessage(NetworkMessage undoMessage) {
        return true;
    }

    private void initialize() {
        preferences = Core.getPreferences();
        switch (preferences.getNetworkMode()) {
            case HOST:
                initializeInbound();
                break;
            case CLIENT:
                initializeOutbound();
                break;
        }
    }

    private void initializeInbound() {
        try {
            serverSocket = new ServerSocket(preferences.getPort(), 10);
            clientSocket = serverSocket.accept();
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            outputStream.flush();
        } catch (IOException e) {
            gameModel.getWaitingDialog().loadErrorInterface();
        }
    }

    private void initializeOutbound() {
        try {
            clientSocket = new Socket(preferences.getHostIP(), preferences.getPort());
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean handshake() {
        switch (preferences.getNetworkMode()) {
            case HOST:
                return handshakeInbound();
            case CLIENT:
                return handshakeOutbound();
        }
        return false;
    }

    private synchronized boolean handshakeInbound() {
        NetworkMessage handshakeMessage = null;
        try {
            handshakeMessage = (NetworkMessage) inputStream.readObject();
        } catch (ClassNotFoundException | IOException e) {
            // notify GameModel
            // end connection
            return false;
        }
        if (NetworkMessage.Type.HANDSHAKE.equals(handshakeMessage.getType())) {
            gameModel.setOpponentName(handshakeMessage.getPlayerName());
        } else {
            // notify GameModel
            // end connection
            return false;
        }

        NetworkMessage responseHandShakeMessage = new NetworkMessage(NetworkMessage.Type.HANDSHAKE);
        responseHandShakeMessage.setPlayerName(preferences.getPlayerName());
        responseHandShakeMessage.setTimerMode(preferences.getTimerMode());
        if (preferences.getTimerMode().equals(Preferences.TimerMode.COUNTDOWN)) {
            responseHandShakeMessage.setTimeLimit(preferences.getTimeLimit());
        }
        responseHandShakeMessage.setUsingCustomPieces(preferences.isUsingCustomPieces());
        try {
            outputStream.writeObject(responseHandShakeMessage);
            outputStream.flush();
            // notify GameModel
        } catch (IOException e) {
            // notify GameModel
            // end connection
            return false;
        }
        System.out.println("Handshake - inbound: success");
        return true;
    }

    private synchronized boolean handshakeOutbound() {
        NetworkMessage handshakeMessage = new NetworkMessage(NetworkMessage.Type.HANDSHAKE);
        handshakeMessage.setPlayerName(preferences.getPlayerName());

        try {
            outputStream.writeObject(handshakeMessage);
            outputStream.flush();
        } catch (IOException e) {
            // notify GameModel
            // end connection
            return false;
        }

        NetworkMessage responseHandshakeMessage = null;
        <fim_suffix>
        if (NetworkMessage.Type.HANDSHAKE.equals(responseHandshakeMessage.getType())) {
            gameModel.setOpponentName(responseHandshakeMessage.getPlayerName());
            preferences.setTimerMode(responseHandshakeMessage.getTimerMode());
            if (responseHandshakeMessage.getTimerMode().equals(Preferences.TimerMode.COUNTDOWN)) {
                preferences.setTimeLimit(responseHandshakeMessage.getTimeLimit());
            }
            preferences.setUsingCustomPieces(responseHandshakeMessage.isUsingCustomPieces());
            // notify GameModel
        } else {
            // notify GameModel
            // end connection
            return false;
        }
        System.out.println("Handshake - outbound: success");
        return true;
    }

    private class MessageDispatch implements Runnable {
        NetworkMessage receivedMessage = null;
        @Override
        public void run() {
            do {
                try {
                    receivedMessage = new NetworkMessage((String) inputStream.readObject());
                    System.out.println("MessageDispatch: message received with type [" + receivedMessage.getType().toString() + "]");
                    switch (receivedMessage.getType()) {
                        case MOVE:
                            onMoveMessage(receivedMessage);
                            break;
                        case UNDO:
                            onUndoMessage(receivedMessage);
                            break;
                        case DISCONNECT:
                            // disconnect
                            break;
                        default:
                            break;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    //e.printStackTrace();
                }
                //} while (!receivedMessage.getType().equals(NetworkMessage.Type.DISCONNECT));
            } while (true);
        }
    }




}
<fim_middle>