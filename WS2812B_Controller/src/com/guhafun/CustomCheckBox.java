package com.guhafun;

import javax.swing.*;

public class CustomCheckBox extends JCheckBox {
    private int index;
    CustomCheckBox(int index, String text){
        super(text);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
