package com.example.udp_test3;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UdpClientThread extends Thread{

    private final String TAG = "[UDP]";
    final static String MOS_RPC_CMD_GET_SYS_INFO = "{\"id\":1999,\"method\":\"Sys.GetInfo\"}";
    final static String MOS_RPC_CMD_SET_WIFI_CONNECTION = "{\"id\": 1998, \"method\": \"AMT.ConnectToAP\", \"params\": {\"ssid\": \"VXA\", \"pass\": \"12345678\"}}";
    final static String MOS_RPC_CMD_GET_WIFI_IP = "{\"id\":1996,\"method\":\"Joe.GetDeviceIP\"}";
    final static int RPC_CMD_EMPTY = -1;
    final static int RPC_SEND_AND_RESPONSE = 0;
    //final static int PRC_CMD_SEND = 0;
    //final static int PRC_CMD_RESPONSE = 1;

    private ArrayList<String> cmd_list = new ArrayList<String>();
    private int cmd_list_size = 0;
    private String dstAddress = "";
    private int dstPort = 0;
    private boolean running = false;
    private final int TimeOutCount = 5;
    private final int TimeOutConfig = 1000;

    private MainActivity.UdpClientHandler handler = null;

    private DatagramSocket socket = null;
    private DatagramPacket packet = null;

    private InetAddress address = null;
    private String rpcData = "";
    private int flow = 0;
    public UdpClientThread(String addr, int port, MainActivity.UdpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        this.handler = handler;
        cmd_list.add(MOS_RPC_CMD_GET_SYS_INFO);
        cmd_list.add(MOS_RPC_CMD_SET_WIFI_CONNECTION);
        cmd_list_size = cmd_list.size();
    }

    public UdpClientThread(String addr, int port, String data, MainActivity.UdpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        this.handler = handler;
        rpcData = data;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private void sendState(String state){
        handler.sendMessage(
                Message.obtain(handler,
                        MainActivity.UdpClientHandler.UPDATE_STATE, state));
    }
    /*
    @Override
    public void run() {
        sendState("connecting...");

        running = true;
        String line = "";
        String TimeOutMsg = null;
        int timeOut = TimeOutCount;

        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(dstAddress);
            //DatagramPacket packet = null;

            // send request
            byte[] buf = new byte[1024];

            do{
                //UDP Send
                packet = new DatagramPacket(rpcData.getBytes(), rpcData.length(), address, dstPort);
                socket.send(packet);
                sendState("connected");
                // set the timeout in millisecounds.
                socket.setSoTimeout(TimeOutConfig);

                //UDP Response
                try{
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    System.out.println("[Joe]Finish Receive!!");
                    //String rcvd = "rcvd from " + packet.getAddress() + ", " + packet.getPort() + ": "+ new String(packet.getData(), 0, packet.getLength());
                    //System.out.println(rcvd);
                    line = new String(packet.getData(), 0, packet.getLength());
                    break;
                }catch (SocketTimeoutException e) {
                    // timeout exception.
                    //socket.close();
                    //line = "";
                    System.out.println("[Joe]TimeOut!!");
                    timeOut --;
                }
            }while(timeOut > 0);

            handler.sendMessage(
                    Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                handler.sendEmptyMessage(MainActivity.UdpClientHandler.UPDATE_END);
            }
        }

    }

     */
    @Override
    public void run() {
        sendState("connecting...");
        String line = "";
        int timeOut = TimeOutCount;
        int index = 0;

        do{
            switch(flow){
                case RPC_SEND_AND_RESPONSE:
                    Log.d(TAG,"RPC_SEND_AND_RESPONSE");
                    if(index >= cmd_list_size) {
                        flow = RPC_CMD_EMPTY;
                        break;
                    }
                    rpcData = cmd_list.get(index);
                    try {
                        socket = new DatagramSocket();
                        address = InetAddress.getByName(dstAddress);
                        // send request
                        byte[] buf = new byte[1024];

                        do{
                            //UDP Send
                            packet = new DatagramPacket(rpcData.getBytes(), rpcData.length(), address, dstPort);
                            socket.send(packet);
                            sendState("connected");
                            // set the timeout in millisecounds.
                            socket.setSoTimeout(TimeOutConfig);

                            //UDP Response
                            try{
                                packet = new DatagramPacket(buf, buf.length);
                                socket.receive(packet);
                                System.out.println("[Joe]Finish Receive!!");

                                line = new String(packet.getData(), 0, packet.getLength());
                                break;
                            }catch (SocketTimeoutException e) {
                                // timeout exception.
                                System.out.println("[Joe]TimeOut!!");
                                timeOut --;
                            }
                        }while(timeOut > 0);
                        if(rpcData == MOS_RPC_CMD_GET_SYS_INFO){
                            handler.sendMessage(
                                    Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_SYSINFO, line));
                        }
                        handler.sendMessage(
                                Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));

                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(socket != null){
                            socket.close();
                            handler.sendEmptyMessage(MainActivity.UdpClientHandler.UPDATE_END);
                            index++;
                        }
                    }
                    break;
                case RPC_CMD_EMPTY:
                    Log.d(TAG,"RPC_CMD_EMPTY");
                    break;
                default:
                    break;
            }
            if(flow == RPC_CMD_EMPTY) break;
        }while(true);
        System.out.println("[Joe]"+"Thread finish!!!");
    }
}
