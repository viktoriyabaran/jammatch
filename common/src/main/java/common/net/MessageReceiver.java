package common.net;

import java.io.IOException;

public interface MessageReceiver {
    byte[] receive() throws InterruptedException, IOException;
}
