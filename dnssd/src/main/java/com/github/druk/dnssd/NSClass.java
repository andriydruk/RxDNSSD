package com.github.druk.dnssd;

// Copied from nameser.h
@SuppressWarnings(value = "")
public class NSClass {

    public final static int INVALID = 0;	    /* Cookie. */
    public final static int IN = 1;		    /* Internet. */
    public final static int CLASS2 = 2;		/* unallocated/unsupported. */
    public final static int CHAOS = 3;		/* MIT Chaos-net. */
    public final static int HS = 4;		    /* MIT Hesiod. */
                                        /* Query class values which do not appear in resource records */
    public final static int NONE = 254;	    /* for prereq. sections in update requests */
    public final static int ANY = 255;		/* Wildcard match. */
    public final static int MAX = 65536;
}
