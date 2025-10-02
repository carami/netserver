package networkexam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Scanner; // Scanner 추가

public class ChatServer2 {
    // 포트 번호를 상수로 고정하지 않고, 실행 시 입력받도록 수정
    private static ConcurrentHashMap<String, ClientHandler> clientsMap = new ConcurrentHashMap<>();

    // 맵에서 Set으로 값을 얻어와 브로드캐스트를 처리 (기존과 동일)
    private static Set<ClientHandler> getAllClients() {
        return ConcurrentHashMap.newKeySet(clientsMap.values().size(), clientsMap.values());
    }

    // 닉네임을 키로 ClientHandler를 찾을 수 있도록 함 (기존과 동일)
    private static ClientHandler findClient(String nickname) {
        return clientsMap.get(nickname);
    }

    // 브로드캐스트 로직 (기존과 동일)
    private static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clientsMap.values()) {
            if (client.equals(sender)) {
                client.sendMessage("내가 보낸메시지 ::" + message);
            } else {
                client.sendMessage(sender.nickname + ":::" + message);
            }
        }
    }

    public static void main(String[] args) {
        int port = 0;
        // 1. 실행 시 포트 번호 입력받기
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("채팅 서버가 사용할 포트 번호를 입력하세요 (예: 12345): ");
            if (scanner.hasNextInt()) {
                port = scanner.nextInt();
            } else {
                System.out.println("유효한 포트 번호를 입력해야 합니다. 서버를 종료합니다.");
                return;
            }
        } catch (Exception e) {
            System.out.println("포트 번호 입력 중 오류 발생: " + e.getMessage());
            return;
        }

        System.out.println("채팅 서버 시작! 포트: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) { // 입력받은 port 사용
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                System.out.println("새로운 연결 수락: " + socket.getInetAddress().getHostAddress()); // 연결 수락 시 IP 출력

                new Thread(clientHandler).start();
            }
        } catch (Exception e) {
            System.out.println("서버 오류: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    // ClientHandler 클래스
    // ----------------------------------------------------------------------
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String nickname;
        private String clientIp; // 클라이언트 IP 주소 저장을 위한 필드 추가

        ClientHandler(Socket socket) {
            this.socket = socket;
            // 2. 클라이언트 IP 주소 추출
            this.clientIp = socket.getInetAddress().getHostAddress();
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private boolean sendPrivateMessage(String targetNickname, String content) {
            // 귓속말 처리 로직 (기존과 동일)
            ClientHandler targetClient = findClient(targetNickname);

            if (targetClient != null) {
                targetClient.sendMessage("[귓속말 from " + this.nickname + "(" + this.clientIp + ")] " + content); // IP 포함
                this.sendMessage("[귓속말 to " + targetNickname + "] " + content);
                return true;
            } else {
                this.sendMessage("[시스템] 사용자 (" + targetNickname + ") 님을 찾을 수 없습니다.");
                return false;
            }
        }

        private void sendHelpMessage() {
            // 도움말 메시지 (기존과 동일)
            this.sendMessage("==================== 채팅 사용법 ====================");
            this.sendMessage("1. 일반 메시지: 내용을 입력 후 엔터");
            this.sendMessage("2. 귓속말: /to [대상닉네임] [내용] 형식으로 입력");
            this.sendMessage("   예) /to userA 안녕하세요.");
            this.sendMessage("3. 종료: bye 를 입력 후 엔터");
            this.sendMessage("4. 도움말: /help 를 입력 후 엔터");
            this.sendMessage("====================================================");
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("닉네임을 입력하세요.");
                String tempNickname = in.readLine();

                if (tempNickname == null) {
                    return;
                }

                if (clientsMap.containsKey(tempNickname)) {
                    out.println("[시스템] 이미 사용 중인 닉네임입니다. 연결을 종료합니다.");
                    return;
                }

                this.nickname = tempNickname;
                clientsMap.put(this.nickname, this);

                // 3. 서버 콘솔 출력에 닉네임 + IP 표시
                System.out.println(nickname + "(" + clientIp + ")님 입장. 현재 접속자 수: " + clientsMap.size());

                sendHelpMessage();

                // 4. 입장/퇴장 메시지에 닉네임 + IP 표시
                broadcast(nickname + "(" + clientIp + ") 님 입장", this);

                String message = null;
                while ((message = in.readLine()) != null) {
                    System.out.println(nickname + "(" + clientIp + "):::" + message); // 서버 콘솔 출력 시 IP 표시

                    if (message.startsWith("/to ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length < 3) {
                            this.sendMessage("[시스템] 귓속말 사용법: /to [대상닉네임] [내용]");
                        } else {
                            String targetNickname = parts[1];
                            String content = parts[2];
                            sendPrivateMessage(targetNickname, content);
                        }
                    } else if ("/help".equalsIgnoreCase(message)) {
                        sendHelpMessage();
                    } else if ("bye".equalsIgnoreCase(message)) {
                        break;
                    } else {
                        broadcast(message, this);
                    }
                }
            } catch (Exception e) {
                System.out.println(this.nickname + "(" + this.clientIp + ") 연결 오류: " + e.getMessage());
            } finally {
                if (nickname != null) {
                    clientsMap.remove(nickname);
                    System.out.println(nickname + "(" + this.clientIp + ")님 퇴장. 현재 접속자 수: " + clientsMap.size());
                    // 5. 퇴장 메시지에 닉네임 + IP 표시
                    broadcast(nickname + "(" + this.clientIp + ") 님 퇴장", this);
                }
                try {
                    socket.close();
                } catch (Exception ignored) {}
            }
        }
    }
}