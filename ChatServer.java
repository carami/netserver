package com.example.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int DEFAULT_PORT = 8020;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) {
                    System.err.println("포트 번호는 1-65535 사이여야 합니다. 기본 포트 " + DEFAULT_PORT + " 사용");
                    port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.err.println("잘못된 포트 번호입니다. 기본 포트 " + DEFAULT_PORT + " 사용");
                port = DEFAULT_PORT;
            }
        }

        System.out.println("채팅 서버가 포트 " + port + "에서 시작되었습니다.");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String nickname;
        private String clientIP;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            // 클라이언트 IP 주소 가져오기
            InetAddress inetAddress = socket.getInetAddress();
            this.clientIP = inetAddress.getHostAddress();
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                out = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), "UTF-8"), true);

                // 닉네임 설정
                out.println("닉네임을 입력하세요:");
                nickname = in.readLine();

                if (nickname == null || nickname.trim().isEmpty()) {
                    return;
                }

                System.out.println(nickname + "[" + clientIP + "]님이 채팅방에 입장했습니다.");
                broadcast(nickname + "[" + clientIP + "]님이 채팅방에 입장했습니다.", this);

                String message;
                while ((message = in.readLine()) != null) {
                    if ("bye".equalsIgnoreCase(message)) {
                        break;
                    }
                    broadcast(nickname + "[" + clientIP + "]: " + message, this);
                }

            } catch (IOException e) {
                System.err.println("클라이언트 처리 오류: " + e.getMessage());
            } finally {
                if (nickname != null) {
                    System.out.println(nickname + "[" + clientIP + "]님이 채팅방을 나갔습니다.");
                    broadcast(nickname + "[" + clientIP + "]님이 채팅방을 나갔습니다.", this);
                }
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("소켓 종료 오류: " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }

    private static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
}