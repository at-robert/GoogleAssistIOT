package com.example.udp_test3;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class UdpClientThread extends Thread{
    String dstAddress;
    int dstPort;
    private boolean running;
    MainActivity.UdpClientHandler handler;

    DatagramSocket socket;
    InetAddress address;
    String rpcData;
    static final int TimeOutCount = 5;
    static final int TimeOutConfig = 1000;

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
            DatagramPacket packet = null;

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


            //socket.receive(packet);

            /*
            while(true){        // recieve data until timeout
                try {
                    socket.receive(packet);
                    System.out.println("[Joe]Receive Finish!!");
                    String rcvd = "rcvd from " + packet.getAddress() + ", " + packet.getPort() + ": "+ new String(packet.getData(), 0, packet.getLength());
                    System.out.println(rcvd);
                    line = new String(packet.getData(), 0, packet.getLength());
                    handler.sendMessage(
                            Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));
                }
                catch (SocketTimeoutException e) {
                    // timeout exception.
                    socket.close();
                    break;
                }
            }
            if(line == null){
                handler.sendMessage(
                        Message.obtain(handler, MainActivity.UdpClientHandler.TRANSFER_MESSAGE, "[Joe]NoData Receive!!!"));
                throw new IOException();
            }
            else{
            }
             */
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

}
