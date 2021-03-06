/**
 * Port forwarding client. Forward data
 * between two TCP ports. Based on Nakov TCP Socket Forward Server 
 * and adapted for IK2206.
 *
 * See original copyright notice below.
 * (c) 2018 Peter Sjodin, KTH
 */

/**
 * Nakov TCP Socket Forward Server - freeware
 * Version 1.0 - March, 2002
 * (c) 2001 by Svetlin Nakov - http://www.nakov.com
 */


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ForwardClient
{
    private static final boolean ENABLE_LOGGING = true;
    public static final int DEFAULTSERVERPORT = 2206;
    public static final String DEFAULTSERVERHOST = "localhost";
    public static final String PROGRAMNAME = "ForwardClient";

    private static Arguments arguments;
    private static int serverPort;
    private static String serverHost;
    private SessionEncrypter encrypter = null;



    /**
     * Program entry point. Reads arguments and run
     * the forward server
     */
    public static void main(String[] args) throws Exception {
        try {
            arguments = new Arguments();
            arguments.setDefault("handshakeport", Integer.toString(DEFAULTSERVERPORT));
            arguments.setDefault("handshakehost", DEFAULTSERVERHOST);
            arguments.loadArguments(args);
            if (arguments.get("targetport") == null || arguments.get("targethost") == null) {
                throw new IllegalArgumentException("Target not specified");
            }
        } catch(IllegalArgumentException ex) {
            System.out.println(ex);
            usage();
            System.exit(1);
        }
        ForwardClient fc = new ForwardClient();
        try {
            fc.startForwardClient();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Set up client forwarder.
     * Run handshake negotiation, then set up a listening socket and wait for user.
     * When user has connected, start port forwarder thread.
     */
     public void startForwardClient() throws Exception {

        doHandshake();

        try {
            ForwardServerClientThread forwardThread;
            ServerSocket bootstrapSocket = new ServerSocket();
            bootstrapSocket.bind(null);
            /* Tell the user, so the user knows where to connect */
            tellUser(bootstrapSocket);

            Socket clientSocket = bootstrapSocket.accept();
            String clientHostPort = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
            log("Accepted client from " + clientHostPort);

            forwardThread = new ForwardServerClientThread(clientSocket, serverHost, serverPort, encrypter);
            forwardThread.start();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e);
            throw e;
        }
    }

    private void doHandshake() throws Exception {

        /* Connect to forward server server */
        System.out.println("Connect to " +  arguments.get("handshakehost") + ":" + Integer.parseInt(arguments.get("handshakeport")));
        Socket socket = new Socket(arguments.get("handshakehost"), Integer.parseInt(arguments.get("handshakeport")));

        /* This is where the handshake should take place */
        Handshake handshake = new Handshake();
        handshake.clientHello(arguments.get("usercert"), socket);
        handshake.forwardMessage(arguments.get("targethost"), arguments.get("targetport"), arguments.get("cacert"), socket);
        handshake.finishHandshake(socket, arguments.get("key"));
        socket.close();

        encrypter = handshake.getEncrypter();
        serverHost = handshake.getServerHost();
        serverPort = handshake.getServerPort();
    }

    /*
     * Let user know that we are waiting
     */
    private static void tellUser(ServerSocket listensocket) throws UnknownHostException {
        System.out.println("Client forwarder to target " + arguments.get("targethost") + ":" + arguments.get("targetport"));
        System.out.println("Waiting for incoming connections at " +
                InetAddress.getLocalHost().getHostAddress() + ":" + listensocket.getLocalPort());
    }


    /**
     * Prints given log message on the standard output if logging is enabled,
     * otherwise ignores it
     */
    public static void log(String aMessage)
    {
        if (ENABLE_LOGGING)
            System.out.println(aMessage);
    }

    static void usage() {
        String indent = "";
        System.err.println(indent + "Usage: " + PROGRAMNAME + " options");
        System.err.println(indent + "Where options are:");
        indent += "    ";
        System.err.println(indent + "--targethost=<hostname>");
        System.err.println(indent + "--targetport=<portnumber>");
        System.err.println(indent + "--handshakehost=<hostname>");
        System.err.println(indent + "--handshakeport=<portnumber>");
        System.err.println(indent + "--usercert=<filename>");
        System.err.println(indent + "--cacert=<filename>");
        System.err.println(indent + "--key=<filename>");
    }
}