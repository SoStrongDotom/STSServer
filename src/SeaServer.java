import java.awt.*;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Vector;
import javax.swing.*;

import java.sql.*;
public class SeaServer {

   public static void main(String[] args) {
      // TODO Auto-generated method stub
      ChatServer3 mainFrame = new ChatServer3("STS Server");
      mainFrame.initForm();
      mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      mainFrame.pack();
      mainFrame.setVisible(true);

   }

}
class ChatServer3 extends JFrame {
   private JPanel pan1;
   private JPanel pan11;
   private JPanel pan12;
   private JPanel pan2;
   private JTextField serverAddr;
   private JTextField portNo;
   private JTextField talkName;
   private JTextField sendMessageBox;
   private JButton startButton;
   private JButton stopButton;
   private JButton forcedExitButton;
   private JButton sendButton;
   private sendMessageHandlerS3 sendMsgHandler;
   
   
   
   private ServerSocket serverSocket;
   private JTextArea showConnectorBox;
   
   JTextArea showMessageBox;
   Socket dataSocket;
   Vector <ServerReceiveThread> vClient;
   boolean listening;
   
   public ChatServer3() {}
   public ChatServer3(String str) {
      super(str);
      vClient = new Vector<ServerReceiveThread>();
   }
   public void initForm(){
	   
	   
	   
	   
	   
	   
      pan1 = new JPanel();
      pan2 = new JPanel();
      pan11 = new JPanel();
      pan12 = new JPanel();
      
      InetAddress inet = null;
      try {
         inet = InetAddress.getLocalHost();
      } catch(UnknownHostException e) { e.printStackTrace(); }
      
      serverAddr = new JTextField(inet.getHostAddress(), 10);
      portNo = new JTextField("1234", 10);
      portNo.setEditable(true);
      portNo.selectAll();
      talkName = new JTextField("Server", 10);
      sendMessageBox = new JTextField(30);
      
      
      startButton = new JButton("Server Start");
      stopButton = new JButton("Server Stop");
      sendButton = new JButton("Send");
      forcedExitButton = new JButton("    ");
      
      showMessageBox = new JTextArea(20, 40);
      showConnectorBox= new JTextArea(10, 10);
      showMessageBox.setLineWrap(true);
      JScrollPane scrollPane1 = new JScrollPane(showMessageBox);
      
      pan2.setLayout(new BorderLayout());
      pan2.add("Center", scrollPane1);
      pan2.add("South", sendMessageBox);
      
      pan1.setLayout(new BorderLayout());
      pan11.setLayout(new GridLayout(4, 2, 0, 10));
      pan11.add(new JLabel(" Server Ip"));
      pan11.add(serverAddr);
      pan11.add(new JLabel(" Port No"));
      pan11.add(portNo);
      pan11.add(new JLabel(" Name"));
      pan11.add(talkName);
      pan11.add(startButton);
      pan11.add(stopButton);
      pan12.setLayout(new BorderLayout());
      pan12.add("North", new JLabel("      "));
      pan12.add("Center", showConnectorBox);
      pan12.add("South", forcedExitButton);
      pan1.add("North", pan11);
      pan1.add("Center", pan12);
      pan1.add("South", sendButton);
      
      showMessageBox.setEditable(false);
      showConnectorBox.setEditable(false);
      startButton.setEnabled(true);
      stopButton.setEnabled(false);
      sendButton.setEnabled(false);
      forcedExitButton.setEnabled(false);
      
      Container cpane;
      cpane = getContentPane();
      cpane.add("East", pan1);
      cpane.add("Center", pan2);
      pack();
      setVisible(true);
      
      startButton.addActionListener(new SjChat3ActionHandler());
      stopButton.addActionListener(new SjChat3ActionHandler());
      sendMsgHandler = new sendMessageHandlerS3();
      sendButton.addActionListener(sendMsgHandler);
      sendMessageBox.addActionListener(sendMsgHandler);
      
   }
   public void broadcast(String msg) throws IOException {
      for(int i=0; i<vClient.size(); i++) {
    	  
         ServerReceiveThread trd = ((ServerReceiveThread)vClient.elementAt(i));
         trd.socketOut.println(msg);
      }
      showMessageBox.append(msg + "\n");
      showMessageBox.setCaretPosition(showMessageBox.getDocument().getLength());
   }
   
   public class SjChat3ActionHandler implements ActionListener {
      public void actionPerformed(ActionEvent e) {
         if(e.getSource() == startButton) {
            showMessageBox.append("Server Start : ");
            SjChatAcceptThread acceptThread = new SjChatAcceptThread();
            acceptThread.start();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            sendButton.setEnabled(true);
            sendMessageBox.requestFocus();
         }
         else {
            showMessageBox.append("Server Stop : ");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            sendButton.setEnabled(false);
            listening = false;
            try {
               serverSocket.close();
            }catch(IOException e1) { e1.printStackTrace(); }
         }
      }
   }
   class sendMessageHandlerS3 implements ActionListener {
      public void actionPerformed(ActionEvent evt) {
         String strMsg;
         try {
            strMsg = sendMessageBox.getText();
            if(!strMsg.isEmpty()) {
               broadcast("[Server] " + strMsg);
               System.out.println(strMsg);
               sendMessageBox.setText("");
               sendMessageBox.requestFocus();
            }
         }
         catch(Exception e) {showMessageBox.append("전송오류\n"); }
      }
   }
   public class SjChatAcceptThread extends Thread {
      ServerReceiveThread chatTrd;
      SjChatAcceptThread() {}
      public void run() {
         int port = Integer.parseInt(portNo.getText());
         listening = true;
         try {
            serverSocket = new ServerSocket(port);
         }
         catch(IOException e) {
            showMessageBox.append("Server Socket 초기화 오류\n");
            return;
         }
         startButton.setEnabled(false);;
         stopButton.setEnabled(true);
         showMessageBox.append(port + " Port에서 기다립니다.\n");
         try {
            while(listening) {
               dataSocket = serverSocket.accept();
               chatTrd = new ServerReceiveThread(ChatServer3.this);
               chatTrd.start();
               vClient.addElement(chatTrd);
            }
            serverSocket.close();
         } catch(IOException e) {}
         showMessageBox.append("서버를 종료합니다.\n");
      }
   }
}
class ServerReceiveThread extends Thread {
   Socket clientSocket = null;
   PrintWriter socketOut;
   BufferedReader socketIn;
   String strInput, strName = "NoName", strSub= "";
   ChatServer3 cServer;
   
   Connection con=null; // 커넥터
   Statement stmt=null;
   ResultSet rs=null;
   String dbFileUrl="jdbc:sqlite:test.db";
   
   
   public ServerReceiveThread() {}
   public ServerReceiveThread(ChatServer3 cs) {
      clientSocket = cs.dataSocket;
      this.cServer = cs;
      
      try {
          
          // DB파일 최초생성과 동시에 연결(최초 생성 후엔 생성 없이 사용만 함)
          Class.forName("org.sqlite.JDBC"); // sqlite 사용하겠다는 문장
          System.out.println("첫번째명령");
          con=DriverManager.getConnection(dbFileUrl); // 커넥터에 파일 링크로 DB파일 드라이브 연결
          System.out.println("SQLite DB connected"); // DB연결 확인하려고 출력
          
          stmt=con.createStatement();
          rs=stmt.executeQuery("select * from Table1");
          System.out.println("stmt,rs ok");
          
          
          //con.close();
       }catch(Exception e) {
          System.out.println("연결오류");
       }
    
      
   }
   
   public void removeClient() throws IOException {
      cServer.vClient.removeElement(this);
      cServer.broadcast("[" + strName + "]님이 퇴장하셨습니다.");
   }
   public void sendUserList() throws IOException {
      int cnt = cServer.vClient.size() + 1;
      socketOut.println("<현재 접속자 " + cnt + "명 명단>");
      socketOut.println("Server ");
      for(int i=0; i<cServer.vClient.size(); i++) {
         ServerReceiveThread trd = ((ServerReceiveThread)cServer.vClient.elementAt(i));
         socketOut.println(trd.strName);
      }
   }
   public void run() {
      try {
         cServer.showMessageBox.append("Client:" + clientSocket.toString() + "에서 접속하였습니다.");
         socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
         socketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         
         socketOut.println("SjChatServer");
         strInput = socketIn.readLine();
         
         if(strInput.equals("SjChatClient")) {
            socketOut.println("<단축키> : /h(도움말), /u(접속자목록), /r 이름 (이름변경), /s(주제보기),\n /ss 주제 (주제수정)");
            strName = socketIn.readLine();
            cServer.broadcast("[" + strName + "]님이 입장하였습니다.");
            
            
           
            
            
            while((strInput = socketIn.readLine()) != null) {
               if(strInput.equals("/h")) {
                  socketOut.println("<단축키> : /h(도움말), /u(접속자목록), /r 이름 (이름변경)");
               } else if(strInput.equals("/u")) {
                  sendUserList();
               } else if(strInput.regionMatches(0, "/r", 0, 2)) {
                  String new_name = strInput.substring(2).trim();
                  cServer.broadcast("접속자" + strName + " 님의 이름이 [" + new_name + "](으)로 바뀌었습니다.");
                  
                  strName = new_name;
               } else if(strInput.regionMatches(0, "/ss", 0, 3)) {
            	   strSub = strInput.substring(3).trim();
            	   strSub += "   <출제자: " + strName + ">";
            	   cServer.broadcast("주제: " + strSub);
               } else if(strInput.equals(("/s"))) {
            	   cServer.broadcast("주제: " + strSub);
               } else {
                  cServer.broadcast("[" + strName + "] " + strInput);
                  try {
                      
                      stmt.executeUpdate("insert into Table1(NAME,CHAT) values ('" + strName + "','" + strInput + "');");
                      System.out.println("값삽입완료");

                      //con.close();
                   }catch(Exception e) {
                      System.out.println("연결오류");
                   }
               }
               
         }
      } else {
         socketOut.println("잘못된 Client입니다.");
      }
         socketOut.close();
         socketIn.close();
         clientSocket.close();
         removeClient();
   }catch(IOException e) {
      try {
         removeClient();
      } catch(IOException e1) {}
      cServer.showMessageBox.append(" " + strName + "의 접속이 끊겼습니다.");
      }
   }
}