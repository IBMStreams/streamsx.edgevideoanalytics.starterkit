/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Mat;
  
public class MyPanel extends JPanel{  
    private static final long serialVersionUID = 1L;  
    private BufferedImage image;  
    
    public static MyPanel newFramedPanel(String title) {
      return newFramedPanel(title, 0, 0);
    }

    public static MyPanel newFramedPanel(String title, int x, int y) {
      return newFramedPanel(title, x, y, 500, 400);
    }
    
    public static MyPanel newFramedPanel(String title, int x, int y, int width, int height) {
      JFrame frame = new JFrame(title);  
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
      frame.setSize(width,height);
      frame.setLocation(x, y);
      MyPanel panel = new MyPanel();  
      frame.setContentPane(panel);       
      frame.setVisible(true);        
      
      return panel;
    }

    public MyPanel(){  
        super();   
    }
    
    public void clear() {
      if (image != null) {
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Arrays.fill(targetPixels, (byte)0);
      }
    }
    
    public boolean matToBufferedImage(Mat matRGB) {
        return matToBufferedImage(matRGB, BufferedImage.TYPE_3BYTE_BGR);
    }
    
    public boolean grayMatToBufferedImage(Mat matGray) {
      return matToBufferedImage(matGray, BufferedImage.TYPE_BYTE_GRAY);
    }
    
    public boolean matToBufferedImage(Mat mat, int bufferedImageType) {  
        int width = mat.width(), height = mat.height(), channels = mat.channels() ;  
        byte[] sourcePixels = new byte[width * height * channels];  
        mat.get(0, 0, sourcePixels);  
        // create new image and get reference to backing data  
        image = new BufferedImage(width, height, bufferedImageType);  
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();  
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);  
        return true;  
    }
    
    @Override
    public void paintComponent(Graphics g) {  
        super.paintComponent(g);   
        if (this.image==null) return;  
        g.drawImage(this.image,10,10,2*this.image.getWidth(),2*this.image.getHeight(), null);  
        //g.drawString("This is my custom Panel!",10,20);  
    }  
}  


