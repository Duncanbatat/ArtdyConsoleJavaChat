package org.artdy.server;

import org.artdy.Connection;
import org.artdy.ConsoleHelper;
import org.artdy.message.Message;
import org.artdy.message.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("InfiniteLoopStatement")
public class Server
{
    private static final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Сервер запущен");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (IOException ignored) {
            ConsoleHelper.writeMessage("Произошла ошибка при запуске или работе сервера.");
        }
    }

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Connection connection : connectionMap.values()) {
                connection.send(message);
            }
        } catch (IOException ignored) {
            System.out.println("Не удалось отправить сообщение.");
        }
    }

    private static class Handler extends Thread {
        private final Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage(String.format("Установлено новое соединение с адресатом %s.", socket.getRemoteSocketAddress()));
            try(Connection connection = new Connection(socket)) {
                String username = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, username));
                notifyUsers(connection, username);
                serverMainLoop(connection, username);
                connectionMap.remove(username);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, username));
            } catch (IOException | ClassNotFoundException ignored) {
                ConsoleHelper.writeMessage(String.format("Произошла ошибка при обмене данными с удаленным адресом %s.", socket.getRemoteSocketAddress()));
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message responseMessage = connection.receive();
                if (responseMessage.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("Получено сообщение от " + connection.getRemoteSocketAddress() + ". Тип сообщения не соответствует протоколу.");
                    continue;
                }
                String username = responseMessage.getData();
                if (username.isEmpty()) {
                    ConsoleHelper.writeMessage("Имя пользователя не может быть пустой строкой!");
                    continue;
                }
                if (connectionMap.containsKey(username)) {
                    ConsoleHelper.writeMessage("Такое имя пользователя уже зарегистрировано!");
                    continue;
                }
                connectionMap.put(username, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return username;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String addedUserName : connectionMap.keySet()) {
                if (!addedUserName.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, addedUserName));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws  IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    Message broadcastMessage = new Message(MessageType.TEXT, String.format("%s: %s", userName, message.getData()));
                    sendBroadcastMessage(broadcastMessage);
                } else {
                    ConsoleHelper.writeMessage("Переданное сообщение не является текстом!");
                }
            }
        }
    }
}
