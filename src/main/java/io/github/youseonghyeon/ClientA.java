package io.github.youseonghyeon;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientA {

    public static void main(String[] args) throws IOException, InterruptedException {
        Socket socket = new Socket("localhost", 9999);
        Thread.startVirtualThread(new RequestHandler(socket));
        Thread.startVirtualThread(new ResponseHandler(socket));
        Thread.sleep(Long.MAX_VALUE);
    }

    private static class RequestHandler implements Runnable {
        private final Socket socket;
        private final Scanner sc = new Scanner(System.in);

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                OutputStream out = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                while (true) {
                    String message = sc.nextLine();
                    dos.writeUTF(message);
                    dos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ResponseHandler implements Runnable {
        private final Socket socket;

        public ResponseHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                DataInputStream dis = new DataInputStream(in);
                while (true) {
                    String message = dis.readUTF();
                    System.out.println("Received: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
