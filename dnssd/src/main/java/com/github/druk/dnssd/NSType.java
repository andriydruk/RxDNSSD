package com.github.druk.dnssd;

// Copied from nameser.h
public class NSType {

    public static int INVALID = 0;	    /* Cookie. */
    public static int A = 1;		    /* Host address. */
    public static int NS = 2;		    /* Authoritative server. */
    public static int MD = 3;		    /* Mail destination. */
    public static int MF = 4;		    /* Mail forwarder. */
    public static int CNAME = 5;		/* Canonical name. */
    public static int SOA = 6;		    /* Start of authority zone. */
    public static int MB = 7;		    /* Mailbox domain name. */
    public static int MG = 8;		    /* Mail group member. */
    public static int MR = 9;		    /* Mail rename name. */
    public static int NULL = 10;		/* Null resource record. */
    public static int WKS = 11;		    /* Well known service. */
    public static int PTR = 12;		    /* Domain name pointer. */
    public static int HINFO = 13;	    /* Host information. */
    public static int MINFO = 14;	    /* Mailbox information. */
    public static int MX = 15;		    /* Mail routing information. */
    public static int TXT = 16;		    /* Text strings. */
    public static int RP = 17;		    /* Responsible person. */
    public static int AFSDB = 18;	    /* AFS cell database. */
    public static int X25 = 19;		    /* X_25 calling address. */
    public static int ISDN = 20;		/* ISDN calling address. */
    public static int RT = 21;		    /* Router. */
    public static int NSAP = 22;		/* NSAP address. */
    public static int NSAP_PTR = 23;	/* Reverse NSAP lookup  = deprecated). */
    public static int SIG = 24;		    /* Security signature. */
    public static int KEY = 25;		    /* Security key. */
    public static int PX = 26;		    /* X.400 mail mapping. */
    public static int GPOS = 27;		/* Geographical position  = withdrawn). */
    public static int AAAA = 28;		/* Ip6 Address. */
    public static int LOC = 29;		    /* Location Information. */
    public static int NXT = 30;		    /* Next domain  (security). */
    public static int EID = 31;		    /* Endpoint identifier. */
    public static int NIMLOC = 32;	    /* Nimrod Locator. */
    public static int SRV = 33;		    /* Server Selection. */
    public static int ATMA = 34;		/* ATM Address */
    public static int NAPTR = 35;	    /* Naming Authority PoinTeR */
    public static int KX = 36;		    /* Key Exchange */
    public static int CERT = 37;		/* Certification record */
    public static int A6 = 38;		    /* IPv6 address  = deprecates AAAA) */
    public static int DNAME = 39;	    /* Non-terminal DNAME  (for IPv6) */
    public static int SINK = 40;		/* Kitchen sink  (experimentatl) */
    public static int OPT = 41;		    /* EDNS0 option  (meta-RR) */
    public static int TKEY = 249;	    /* Transaction key */
    public static int TSIG = 250;	    /* Transaction signature. */
    public static int IXFR = 251;	    /* Incremental zone transfer. */
    public static int AXFR = 252;	    /* Transfer zone of authority. */
    public static int MAILB = 253;	    /* Transfer mailbox records. */
    public static int MAILA = 254;	    /* Transfer mail agent records. */
    public static int ANY = 255;		/* Wildcard match. */
    public static int ZXFR = 256;	    /* BIND-specific, nonstandard. */
    public static int MAX = 65536;

}
