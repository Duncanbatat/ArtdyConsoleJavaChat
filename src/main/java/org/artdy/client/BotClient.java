package org.artdy.client;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BotClient extends Client {
    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random()*100);
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            super.processIncomingMessage(message);
            String userNameDelimiter = ": ";
            String[] split = message.split(userNameDelimiter);
            if (split.length != 2) {
                return;
            }
            String userName = split[0];
            String text = split[1];
            String datePattern = null;
            switch (text) {
                case "дата":
                    datePattern = "d.MM.YYYY";
                    break;
                case "день":
                    datePattern = "d";
                    break;
                case "месяц":
                    datePattern = "MMMM";
                    break;
                case "год":
                    datePattern = "YYYY";
                    break;
                case "время":
                    datePattern = "H:mm:ss";
                    break;
                case "час":
                    datePattern = "H";
                    break;
                case "минуты":
                    datePattern = "m";
                    break;
                case "секунды":
                    datePattern = "s";
                    break;
            }
            if (datePattern != null) {
                SimpleDateFormat format = new SimpleDateFormat(datePattern);
                Date date = Calendar.getInstance().getTime();
                BotClient.this.sendTextMessage(String.format("Информация для %s: %s", userName, format.format(date)));
            }
        }
    }
}
