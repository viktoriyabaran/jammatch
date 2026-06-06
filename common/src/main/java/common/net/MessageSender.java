package common.net;

public interface MessageSender {
    void send(byte[] message) throws InterruptedException;
}
