import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudServer {

    public CloudServer() {
        ExecutorService run = Executors.newFixedThreadPool(4);
        try(ServerSocket server = new ServerSocket(8189)){
            while (true){
                Socket socket = server.accept();
                System.out.println("Client accepted");
                run.execute(new ClientHandler(socket));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new CloudServer();
    }
}
