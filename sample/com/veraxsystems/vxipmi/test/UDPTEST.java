package com.veraxsystems.vxipmi.test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPTEST {
    public static void main(String[] args) throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(1000);
        ds.connect(InetAddress.getByName("127.0.0.1"), 623); // 连接指定服务器和端口
// 发送:
        byte[] data = "Hello".getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length);
        //ds.send(packet);
        //ds.disconnect();
// 接收:
        byte[] buffer = new byte[1024];
        DatagramSocket ds1 = new DatagramSocket(623);
        DatagramPacket packet1 = null;
        packet1 = new DatagramPacket(buffer, buffer.length);
        ds1.setSoTimeout(0);
        ds1.receive(packet);
        String resp = new String(packet1.getData(), packet1.getOffset(), packet1.getLength());
        System.out.println(resp);
        System.out.println(packet.getAddress().getAddress());
        System.out.println(packet.getPort());

    }
}
