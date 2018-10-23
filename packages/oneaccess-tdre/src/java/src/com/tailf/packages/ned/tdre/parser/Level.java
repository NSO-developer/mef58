package com.tailf.packages.ned.tdre.parser;

import com.tailf.conf.ConfException;
import com.tailf.maapi.Maapi;
import org.antlr.v4.runtime.ParserRuleContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public abstract class Level {
    public String name;
    public HashMap<String, Level> members;

    public Level() {
        this.members = new LinkedHashMap<String, Level>();
    }

    public Level(String value) {
//        System.out.println(String.format("value: %s ", value));
    }

    public void add(Level level) {
        if (level != null) {
            this.members.put(level.name, level);
        }
    }

    public void add_list_entry(ListEntry le) {
        if (le != null) {
            Level list = this.get(le.listname);
            if (list == null) {
                list = new Cont(le.listname);
                this.add(list);
            }
            list.add(le);
        }
    }

    public boolean exist(String name) {
        return members.containsKey(name);
    }

    public Level get(String name) {
        return this.members.get(name);
    }

    public void print(int indent, String msg) {
        System.out.println(ids(indent) + msg);
    }

    public void print(int indent, String msg, String msg2) {
        this.print(indent, msg + " " + msg2);
    }

    public String ids(int indent) {
        String ids = "";
        while (indent-- > 0) {
            ids += " ";
        }
        return ids;
    }

    public abstract void printElem(int indent);

    public void printMembers(int indent) {
        for (String key: this.members.keySet()) {
            this.members.get(key).printElem(indent);
        }
    }

    public void print_structure() {
        this.printElem(0);
    }


    public void create_structure(Maapi m, int th, String device) {
        process_elem(m, th, "", String.format("/devices/device{%s}/config", device));
    }

    public void process_elem(Maapi m, int th, String pl, String kp) {
        if (this.members.size() > 0) {
            for (String key : this.members.keySet()) {
                Level lvl = this.members.get(key);
                String nkp = kp;
                if (pl.equals("priorityPolicy") || pl.equals("bandwidth")) {
                    nkp += "{" + lvl.name + "}";
                    create(m, th, nkp);
                } else {
                    nkp += "/" + lvl.name;
                }
                lvl.process_elem(m, th, lvl.name, nkp);
            }
        } else {
            System.out.println(kp);
            set_elem(m, th, kp, (Attr)this);
        }
    }

    public void create(Maapi m, int th, String kp) {
        System.out.println(kp);
        try {
            m.create(th, kp);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfException e) {
            e.printStackTrace();
        }
    }

    public void set_elem(Maapi m, int th, String kp, Attr value) {
        try {
            m.setElem(th, value.value, kp);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfException e) {
            e.printStackTrace();
        }
    }
}
