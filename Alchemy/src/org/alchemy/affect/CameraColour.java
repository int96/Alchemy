/*
 * This file is part of the Alchemy project - http://al.chemy.org
 * 
 * Copyright (c) 2007-2008 Karl D.D. Willis
 * 
 * Alchemy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alchemy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Alchemy.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.alchemy.affect;

import JMyron.JMyron;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import org.alchemy.core.*;

/**
 * CameraColour
 * @author Karl D.D. Willis
 */
public class CameraColour extends AlcModule implements AlcConstants {

    static {
        if (Alchemy.PLATFORM == WINDOWS) {
            System.loadLibrary("dsvl");
            System.loadLibrary("myron_ezcam");
        }
    }
    private JMyron cam;
    //private AlcCamera cam;
    private int width = 640;
    private int height = 480;
    private int refreshRate = 100;
    private BufferedImage cameraImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    private boolean cameraDisplay = false;
    private AlcSubToolBarSection subToolBarSection;
    private Thread camThread;
    private boolean threadPaused = true;

    public CameraColour() {

    }

    @Override
    public void setup() {
        createSubToolBarSection();
        toolBar.addSubToolBarSection(subToolBarSection);
        setupCamera();
    }

    @Override
    public void deselect() {
        if (cameraDisplay) {
            setCameraDisplay(false);
        }
        synchronized (camThread) {
            threadPaused = true;
        }
        cam.stop();
        cam = null;
        camThread = null;

    }

    @Override
    public void reselect() {
        toolBar.addSubToolBarSection(subToolBarSection);
        setupCamera();
    }

    public void createSubToolBarSection() {
        subToolBarSection = new AlcSubToolBarSection(this);

        // Show Camera
        AlcSubToggleButton cameraButton = new AlcSubToggleButton("Display Image", AlcUtil.getUrlPath("imagedisplay.png", getClassLoader()));
        cameraButton.setToolTipText("Display the camera image");

        cameraButton.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        setCameraDisplay(!cameraDisplay);
                    }
                });
        subToolBarSection.add(cameraButton);
    }

    private void setupCamera() {
        cam = new JMyron();
        cam.start(width, height);
        cam.findGlobs(0);

        camThread = new Thread() {

            @Override
            public void run() {

                try {
                    while (true) {
                        //if (cam != null) {
                        cam.update();
                        cameraImage.setRGB(0, 0, width, height, cam.image(), 0, width);
                        if (cameraDisplay) {
                            canvas.setImage(cameraImage);
                            canvas.redraw();
                        }
                        //}

                        // Now the thread checks to see if it should suspend itself
                        if (threadPaused) {
                            synchronized (this) {
                                while (threadPaused) {
                                    wait();
                                }
                            }
                        }
                        Thread.sleep(refreshRate);  // interval given in milliseconds
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        threadPaused = false;
        camThread.start();
    }

    private void setCameraDisplay(boolean b) {
        cameraDisplay = b;
        if (b) {
            int x = (canvas.getWidth() - width) >> 1;
            int y = (canvas.getHeight() - height) >> 1;
            canvas.setImageLocation(x, y);
            canvas.setImageDisplay(true);
        } else {
            canvas.setImageDisplay(false);
            canvas.resetImageLocation();
            canvas.redraw();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (camThread) {
            threadPaused = true;
        }

        Point p = e.getPoint();

        // Centre the image
        int x = (canvas.getWidth() - width) >> 1;
        int y = (canvas.getHeight() - height) >> 1;
        Rectangle camBounds = new Rectangle(x, y, width, height);

        // If the mouse point is inside the centred image then set the colour
        if (camBounds.contains(p)) {
            int colour = cameraImage.getRGB(p.x - x, p.y - y);
            for (int i = 0; i < canvas.createShapes.size(); i++) {
                canvas.createShapes.get(i).setColour(new Color(colour));
            }
            for (int j = 0; j < canvas.affectShapes.size(); j++) {
                canvas.affectShapes.get(j).setColour(new Color(colour));
            }
            canvas.setColour(new Color(colour));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        synchronized (camThread) {
            threadPaused = false;
            camThread.notify();
        }
    }
    }
