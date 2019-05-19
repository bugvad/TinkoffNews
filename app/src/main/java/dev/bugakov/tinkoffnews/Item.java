package dev.bugakov.tinkoffnews;

public class Item {

    public Integer getId() {
        return id;
    }

    public Integer id;

    public String getText() {
        return text;
    }

    public String text;

    public long milliseconds;

    public Item(Integer id, String text, long milliseconds){
        this.id=id;
        this.text=text;
        this.milliseconds=milliseconds;
    }
}