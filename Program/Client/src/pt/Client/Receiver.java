package pt.Client;

import pt.Common.Command;
import pt.Common.Constants;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Receiver extends Thread {

    private ObjectInputStream oIS;
    private List<Waiter> waiters;


    public Receiver(ObjectInputStream oIS) {
        waiters = Collections.synchronizedList(new ArrayList<>());
        this.oIS = oIS;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Command command = (Command) oIS.readObject();

                setAll(command);

                for (var waiter : waiters) {
                    synchronized (waiter) {
                        waiter.notifyAll();
                    }
                }
            } catch (Exception e) {
                ClientMain instance = ClientMain.getInstance();
                System.out.println("Trying to connect");
                try {
                    instance.connectToServer();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void setAll(Object obj) {
        for (var waiter : waiters) {
            waiter.setResult(obj);
        }
    }

    public Object waitForCommand() throws InterruptedException {
        Waiter waiter = new Waiter();
        synchronized (waiter) {
            waiters.add(waiter);
            waiter.wait();
            waiters.remove(waiter);
        }
        return waiter.getResult();
    }

}
