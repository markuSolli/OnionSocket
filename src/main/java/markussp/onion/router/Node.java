package markussp.onion.router;

import markussp.onion.util.Standards;

import java.io.IOException;
import java.util.Scanner;

public class Node {
    private static int portnr = Standards.PORTNR;
    private static String distAddress = Standards.DIST;
    private static int distPort = Standards.DISTPORT;

    public static void main(String[] args) throws Exception{
        //Override standard values if given
        if(args.length > 0){
            portnr = Integer.parseInt(args[0]);
            if(args.length > 1){
                distAddress = args[1];
                if(args.length > 2){
                    distPort = Integer.parseInt(args[2]);
                }
            }
        }

        //Start OnionNode in own thread
        OnionNode node = new OnionNode(portnr, distAddress, distPort);
        Thread thread = new Thread(() -> {
            try {
                node.launch();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        //Wait for user to type exit command
        System.out.println("OnionNode is running, type 'x' to exit");
        Scanner scanner = new Scanner(System.in);
        String line;
        while(!(line = scanner.nextLine()).equals("x"));

        //Close node
        node.close();
        thread.join();
    }
}
