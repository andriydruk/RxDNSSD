package com.github.druk.dnssd;

// Copied from nameser.h
public class NSClass {

    public static int INVALID = 0;	    /* Cookie. */
    public static int IN = 1;		    /* Internet. */
    public static int CLASS2 = 2;		/* unallocated/unsupported. */
    public static int CHAOS = 3;		/* MIT Chaos-net. */
    public static int HS = 4;		    /* MIT Hesiod. */
                                        /* Query class values which do not appear in resource records */
    public static int NONE = 254;	    /* for prereq. sections in update requests */
    public static int ANY = 255;		/* Wildcard match. */
    public static int MAX = 65536;
}
