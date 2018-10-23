package com.tailf.packages.ned.tdre.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;


/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link r1645Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public class r1645MyVisitor extends r1645BaseVisitor<Level>{
	/**
	 * Visit a parse tree produced by {@link r1645Parser#data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */

    Filter current;
	Stack<Filter> stack;

	public int indent;
    public int INDENT = 2;

    public r1645MyVisitor(Filter filter) {
        this.indent = 0;
        this.current = filter;
        this.stack = new Stack<Filter>();
    }

    /*********************************************************************
     * Debug print functions                                             *
     *********************************************************************/

    public void print(String msg) {
        String id = "";
        for (int i=0;i<this.indent; i++) {
            id += " ";
        }
        //System.out.println(id+msg);
	}

    public void print(String msg, String msg2) {
        this.print(msg+ " " + msg2);
    }


    /*********************************************************************
     * Filter functions                                                  *
     *********************************************************************/

    public Filter filter(String name) {
	    return this.current.match(name);
	}

	public void enter(Filter match) {
        this.stack.push(this.current);
        this.current = match;
	}

	public void exit() {
        this.current = this.stack.pop();
    }

    /*********************************************************************
     * Helper functions                                                  *
     *********************************************************************/

    public String[] split_name(String entry_name) {
        String[] parts = entry_name.split("\\[");
        return new String[]{parts[0], parts[1].substring(0, parts[1].length()-1)};
    }



    /*********************************************************************
     * Visitors                                                          *
     *********************************************************************/

    @Override
    public Level visit(ParseTree tree) {
        this.indent += INDENT;
        Level lvl = super.visit(tree);
        this.indent -= INDENT;
        return lvl;
    }


    @Override
	public Level visitData(r1645Parser.DataContext ctx) {
		this.print("DATA");
		Cont root = new Cont("root");
		
		for (r1645Parser.SelectContext s_ctx: ctx.select()) {
            root.add(this.visit(s_ctx));
        }

        return root;
	}


	/**
	 * Visit a parse tree produced by {@link r1645Parser#select}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	@Override
	public Level visitSelect(r1645Parser.SelectContext ctx) {
        String name = ctx.NAME().toString();

        /**********
         * Filter *
         **********/

        Filter f = this.filter(name);
        if (f == null)
            return null;
        this.enter(f);

        this.print("SELECT", name);

        /*******************
         * Process members *
         *******************/

        Cont sel = new Cont(name);

        for (r1645Parser.SelectContext s_ctx: ctx.select()) {
            sel.add(this.visit(s_ctx));
        }
        for (r1645Parser.Select_listContext sl_ctx: ctx.select_list()) {
            sel.add(this.visit(sl_ctx));
        }
        for (r1645Parser.Select_list_nameContext sln_ctx: ctx.select_list_name()) {
            sel.add_list_entry((ListEntry)this.visit(sln_ctx));
        }

        this.exit();
        // TODO: Return empty object?
        return sel;
	}

	/**
	 * Visit a parse tree produced by {@link r1645Parser#select_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	@Override
	public Level visitSelect_list(r1645Parser.Select_listContext ctx) {
        String name = ctx.NAME().toString();

        /**********
         * Filter *
         **********/

        Filter f = this.filter(name);
        if (f == null)
            return null;
        this.enter(f);

        this.print("SELECT-LIST", name);

        /*******************
         * Process members *
         *******************/

        Cont sel_list = new Cont(name);

        for (r1645Parser.SelectContext s_ctx: ctx.select()) {
            sel_list.add(this.visit(s_ctx));
        }

        this.exit();
        // TODO: Return empty object?
        return sel_list;
	}

	/**
	 * Visit a parse tree produced by {@link r1645Parser#select_list_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	@Override
	public Level visitSelect_list_name(r1645Parser.Select_list_nameContext ctx) {
        String[] parts = this.split_name(ctx.ENTRY().toString());
        String listname = parts[0];
        String index = parts[1];

        /**********
         * Filter *
         **********/

        Filter f = this.filter(listname);
        if (f == null)
            return null;
        this.enter(f);

        this.print("SELECT-LIST-NAME", listname);

        f = this.filter(index);
        if (f == null)
            return null;
        this.enter(f);

        this.print("SELECT-LIST-NAME", index);

        /************************************
         * Process members (bypassing liste)*
         ***********************************/

        Cont sln = new ListEntry(listname, index);

        for (r1645Parser.AttrContext a_ctx : ctx.liste().attr()) {
            sln.add(this.visit(a_ctx));
        }
        for (r1645Parser.Attr_listContext al_ctx : ctx.liste().attr_list()) {
            sln.add(this.visit(al_ctx));
        }

        this.exit();
        this.exit();
        // TODO: Return empty object?
        return sln;
	}


	/**
	 * Visit a parse tree produced by {@link r1645Parser#attr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	@Override
	public Level visitAttr_list(r1645Parser.Attr_listContext ctx) {
        String name = ctx.NAME().toString();

        /**********
         * Filter *
         **********/

        Filter f = this.filter(name);
        if (f == null)
            return null;
        this.enter(f);

        this.print("ATTR-LIST", ctx.NAME().toString());

        /*******************
         * Process members *
         *******************/

        Cont attr_list = new Cont(ctx.NAME().toString());

        if (ctx.index() != null) {
            for (r1645Parser.IndexContext i_ctx : ctx.index()) {
                attr_list.add(this.visit(i_ctx));
            }
        } else {
            for (r1645Parser.AttrContext a_ctx : ctx.attr()) {
                attr_list.add(this.visit(a_ctx));
            }
            for (r1645Parser.Attr_listContext al_ctx : ctx.attr_list()) {
                attr_list.add(this.visit(al_ctx));
            }
        }

        this.exit();
        // TODO: Return empty object?
        return attr_list;
	}

	/**
	 * Visit a parse tree produced by {@link r1645Parser#index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	@Override
	public Level visitIndex(r1645Parser.IndexContext ctx) {
        String full_index = ctx.INDEX().toString();
        String index = full_index.substring(1, full_index.length()-1);

        /**********
         * Filter *
         **********/

        Filter f = this.filter(index);
        if (f == null)
            return null;
        this.enter(f);

        this.print("INDEX", ctx.INDEX().toString());

        /*******************
         * Process members *
         *******************/

        Cont indexes = new Cont(full_index.substring(1, full_index.length()-1));

        for (r1645Parser.AttrContext a_ctx : ctx.attr()) {
            indexes.add(this.visit(a_ctx));
        }
        for (r1645Parser.Attr_listContext al_ctx : ctx.attr_list()) {
            indexes.add(this.visit(al_ctx));
        }

        this.exit();
        // TODO: Return empty object?
        return indexes;
	}


	/**
	 * Visit a parse tree produced by {@link r1645Parser#attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	@Override
	public Level visitAttr(r1645Parser.AttrContext ctx) {
	    String name = ctx.NAME(0).toString();

        Filter f = this.filter(name);
        if (f == null)
            return null;

		String value;
		if (ctx.NAME(1) != null ) {
		    value = ctx.NAME(1).toString();
        } else if(ctx.VALUE() != null) {
            value = ctx.VALUE().toString();
        } else {
            value = ctx.STR().toString();
        }
//        this.print("ATTR",ctx.NAME(0).toString() + " " + value);
		return new Attr(ctx.NAME(0).toString(), value);
	}
}
