package com.tailf.packages.ned.tdre.parser;

public class AttrList extends Level {
    public AttrList(String name) {
        this.name = name;
    }

    public void add(String index) {

    }

    public String toString() {
        return this.name;
    }

    public void printElem(int indent) {
        this.print(indent, String.format("%s: {", this.name));

        this.printMembers(indent+1);

        this.print(indent, "}");
    }
}
