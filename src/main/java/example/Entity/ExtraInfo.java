package example.Entity;

public class ExtraInfo {
    public int port;
    public String host;
    public String data;
    public boolean startSuccess;
    public int[][] sendWin;
    public int[] recWin;

    public ExtraInfo(int[][] sendWin) {
        this.sendWin = sendWin;
    }

    public ExtraInfo(int[] recWin) {
        this.recWin = recWin;
    }

    public ExtraInfo(boolean startSuccess) {
        this.startSuccess = startSuccess;
    }

    public ExtraInfo(String data) {
        this.data = data;
    }
}
