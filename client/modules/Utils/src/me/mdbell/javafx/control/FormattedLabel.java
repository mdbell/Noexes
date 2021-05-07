package me.mdbell.javafx.control;

import javafx.scene.control.Label;

import java.text.MessageFormat;

public class FormattedLabel extends Label {

    private MessageFormat format;

    public FormattedLabel(){
        this("");
    }

    public FormattedLabel(String format){
        this(new MessageFormat(format));
    }

    public FormattedLabel(MessageFormat format){
        this.format = format;
    }

    public void setFormat(String format){
        setFormat(new MessageFormat(format));
    }

    private void setFormat(MessageFormat format){
        this.format = format;
    }

    public String  getFormat(){
        return format.toPattern();
    }

    public void setFormattedText(Object... args){
        setText(format.format(args));
    }
}
