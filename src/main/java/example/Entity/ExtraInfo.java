package example.Entity;

public class ExtraInfo {
    public int port;
    public String host = null;
    public String data = null;
    public SendWindow sendWin = null;
    public ReceiveWindow recvWin = null;
    public int newSendWinSize;
    public int newRecvWinSize;

    public ExtraInfo(SendWindow sendWin) {
        this.sendWin = sendWin;
    }

    public ExtraInfo(ReceiveWindow recWin) {
        this.recvWin = recWin;
    }

    public ExtraInfo(String data) {
        this.data = data;
    }

    public ExtraInfo(String data, ReceiveWindow recWin) {
        this.data = data;
        this.recvWin = recWin;
    }
}
