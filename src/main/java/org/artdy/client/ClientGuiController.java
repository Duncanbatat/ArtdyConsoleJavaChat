package org.artdy.client;

public class ClientGuiController extends Client {
    private ClientGuiModel model = new ClientGuiModel();
    private ClientGuiView view = new ClientGuiView(this);

    public static void main(String[] args) {
        ClientGuiController controller = new ClientGuiController();
        controller.run();
    }

    @Override
    public void run() {
        SocketThread guiSocketThread = getSocketThread();
        guiSocketThread.run();
    }

    @Override
    protected String getServerAddress() {
        return view.getServerAddress();
    }

    @Override
    protected int getServerPort() {
        return view.getServerPort();
    }

    @Override
    protected String getUserName() {
        return view.getUserName();
    }

    public ClientGuiModel getModel() {
        return model;
    }

    @Override
    protected SocketThread getSocketThread() {
        return new GuiSocketThread();
    }

    public class GuiSocketThread extends SocketThread {
        @Override
        protected void processIncomingMessage(String message) {
            model.setNewMessage(message);
            view.refreshMessages();
        }

        @Override
        protected void informAboutAddingNewUser(String userName) {
            model.addUser(userName);
            view.refreshUsers();
        }

        @Override
        protected void informAboutDeletingNewUser(String userName) {
            model.deleteUser(userName);
            view.refreshUsers();
        }

        @Override
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
