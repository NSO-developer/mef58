package com.tailf.packages.ned.tdre.parser;

public class Attr extends Level {
    public String value;

    public Attr(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String toString() {
        return String.format("%s = %s", this.name, this.value);
    }

    public void printElem(int indent) {
        this.print(indent, this.toString());
    }
}
