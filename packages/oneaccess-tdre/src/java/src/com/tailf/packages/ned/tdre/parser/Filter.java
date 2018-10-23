package com.tailf.packages.ned.tdre.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Filter {

    public static Filter ALL = new Filter("*");
    public String name;
    public HashMap<String, Filter> members;

    public Filter() {
        this.name = "root";
        this.members = new HashMap<String, Filter>();
    }
    public Filter(Filter filter)
    {
        this();
        this.add(filter);
    }

    public Filter(Filter[] filters) {
        this();
        this.add(filters);
    }

    public Filter(String name) {
        this();
        this.name = name;
    }

    public Filter(String name, Filter filter) {
        this(name);
        add(filter);
    }

    public Filter(String name, Filter[] filters) {
        this(name);
        add(filters);
    }

    public void add(Filter filter) {
        this.members.put(filter.name, filter);
    }

    public void add(Filter[] filters) {
        for (Filter filter: filters) {
            this.add(filter);
        }
    }

    public Filter get(String name) {
        Filter next = this.members.get(name);
        if (this.members.size()>0 && next != null) {
            return next;
        }
        return ALL;
    }

    public Filter matchn(String name) {
        if (this.name.equals("*")) {
            return this;
        }

        return this.members.get(name);
    }

    public Filter match(String name) {
//        String s = String.join(",", this.members.keySet());
//        System.out.println(String.format("FILTER? %s in [%s]", name, s));

        if (this.members.size() == 0) {
            return ALL;
        }

        if (this.members.containsKey(name))
            return this.members.get(name);
        if (this.members.containsKey("*"))
            return this.members.get("*");

        return null; // No match
    }

    public Filter add_path(String path)
    {
        Filter f = null;

        int pos = path.indexOf('/');
        if (pos != -1) {
            f = new Filter(path.substring(0, pos));
            this.add(f);
            f = f.add_path(path.substring(pos+1));
        } else {
            f = new Filter(path);
            this.add(f);
        }

        return f;
    }

    public void add_path(String[] paths)
    {
        for(String path: paths) {
            add_path(path);
        }
    }
}
