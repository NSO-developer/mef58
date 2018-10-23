package com.tailf.packages.ned.tdre.parser;

public class ListEntry extends Cont {
    public String listname;

    public ListEntry(String listname, String name) {
        super(name);
        this.listname = listname;
    }

    public String toString() {
//        return String.format("%s{%s}", this.listname, this.name);
        return this.name;
    }

    public void printElem(int indent) {
        this.print(indent, String.format("%s: {", this.toString()));

        printMembers(indent+1);

        this.print(indent, "}");
    }
}

