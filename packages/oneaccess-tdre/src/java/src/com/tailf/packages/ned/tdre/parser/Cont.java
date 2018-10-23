package com.tailf.packages.ned.tdre.parser;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;

public class Cont extends Level {
    public Cont(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public void printElem(int indent) {
        this.print(indent, String.format("%s: {", this.toString()));

        printMembers(indent+1);

        this.print(indent, "}");
    }
}

