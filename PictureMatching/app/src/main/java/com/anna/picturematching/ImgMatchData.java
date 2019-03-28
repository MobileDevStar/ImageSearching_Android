package com.anna.picturematching;

import org.opencv.core.Mat;

import java.util.LinkedList;

public class ImgMatchData {
    public LinkedList<Mat> desc2List = null;
    public LinkedList<String> prevNames = null;

    public ImgMatchData() {
    }

    public void initialize() {
        desc2List = new LinkedList<Mat>();
        prevNames = new LinkedList<String>();
    }

}
