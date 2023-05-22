package example.Service;

import example.Entity.SendWindow;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 超时重传扫描器
 */
public class Scanner extends Thread{

    private Timer timer;
    private SendWindow sendWindow;

    public Scanner() {

    }
}
