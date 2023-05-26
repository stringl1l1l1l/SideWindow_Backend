package example.Entity;

public class ExtraInfo {
    public int port;
    public String host;
    public String data;
    public boolean startSuccess;

    public ExtraInfo(boolean startSuccess) {
        this.startSuccess = startSuccess;
    }
}
