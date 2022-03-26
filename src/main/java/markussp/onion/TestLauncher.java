package markussp.onion;

import markussp.onion.router.OnionNode;

import java.util.Scanner;

/**
 * The NodeLauncher class is mainly for testing purposes. Launches a specified number of
 * {@link OnionNode} instances in seperate threads on localport with portnumbers from 3000
 * and rising.
 */
public class TestLauncher {
    public static void main(String[] args) throws Exception{
        //Get number of nodes from argument
        int n = Integer.parseInt(args[0]);

        //Create threads
        Thread[] threads = new Thread[n];
        OnionNode[] nodes = new OnionNode[n];

        for(int i=0; i<n; i++){
            nodes[i] = new OnionNode(3000 + i);
            threads[i] = new Thread(new LauncherThread(nodes[i]));
        }

        //Launch threads
        for(Thread thread : threads){
            thread.start();
        }

        //Wait for user to type exit command
        System.out.println("Onion nodes launched, type 'x' to exit");
        Scanner scanner = new Scanner(System.in);
        String line;
        while(!(line = scanner.nextLine()).equals("x"));

        //Close all nodes
        for(OnionNode node : nodes){
            node.close();
        }

        //Wait for all threads to finish
        for(Thread thread : threads){
            thread.join();
        }
    }
}

class LauncherThread implements Runnable{
    private final OnionNode node;
    private boolean running = true;

    LauncherThread(OnionNode node){
        this.node = node;
    }

    @Override
    public void run() {
        try{
            //Launch OnionNode server
            node.launch();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
