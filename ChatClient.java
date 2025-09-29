package com.example.net;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private static final String SERVER_HOST = "15.164.228.241";
    private static final int SERVER_PORT = 8020;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
             Scanner scanner = new Scanner(System.in, "UTF-8")) {

            // 서버 메시지를 읽는 스레드
            Thread readerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("서버 연결이 끊어졌습니다.");
                }
            });

            readerThread.start();

            // 사용자 입력 처리
            String userInput;
            while (!(userInput = scanner.nextLine()).equalsIgnoreCase("bye")) {
                out.println(userInput);
            }

            out.println("bye");

        } catch (IOException e) {
            System.err.println("서버 연결 실패: " + e.getMessage());
        }
    }
}