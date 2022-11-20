package org.artdy.client;

import org.artdy.Connection;
import org.artdy.ConsoleHelper;
import org.artdy.message.Message;
import org.artdy.message.MessageType;

import java.io.IOException;
import java.net.Socket;

@SuppressWarnings("InfiniteLoopStatement")
public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException ignored) {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
            clientConnected = false;
            return;
        }
        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }

        while(clientConnected) {
            String text = ConsoleHelper.readString();
            if (text.equals("exit")) {
                clientConnected = false;
            } else {
                if (shouldSendTextFromConsole()) {
                    sendTextMessage(text);
                }
            }
        }
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите своё имя пользователя:");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException ignored) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Во время отправки сообщения произошла ошибка!");
        }
    }

    public class SocketThread extends Thread {
        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try {
                connection = new Connection(new Socket(serverAddress, serverPort));
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException ignored) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник '" + userName + "' присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник '" + userName + "' покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while(true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    String userName = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType.");
                }
            }
        }
    }
}
