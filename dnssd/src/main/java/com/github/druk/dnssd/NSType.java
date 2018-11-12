package com.github.druk.dnssd;

// Copied from nameser.h
public class NSType {

    public final static int INVALID = 0;	    /* Cookie. */
    public final static int A = 1;		    /* Host address. */
    public final static int NS = 2;		    /* Authoritative server. */
    public final static int MD = 3;		    /* Mail destination. */
    public final static int MF = 4;		    /* Mail forwarder. */
    public final static int CNAME = 5;		/* Canonical name. */
    public final static int SOA = 6;		    /* Start of authority zone. */
    public final static int MB = 7;		    /* Mailbox domain name. */
    public final static int MG = 8;		    /* Mail group member. */
    public final static int MR = 9;		    /* Mail rename name. */
    public final static int NULL = 10;		/* Null resource record. */
    public final static int WKS = 11;		    /* Well known service. */
    public final static int PTR = 12;		    /* Domain name pointer. */
    public final static int HINFO = 13;	    /* Host information. */
    public final static int MINFO = 14;	    /* Mailbox information. */
    public final static int MX = 15;		    /* Mail routing information. */
    public final static int TXT = 16;		    /* Text strings. */
    public final static int RP = 17;		    /* Responsible person. */
    public final static int AFSDB = 18;	    /* AFS cell database. */
    public final static int X25 = 19;		    /* X_25 calling address. */
    public final static int ISDN = 20;		/* ISDN calling address. */
    public final static int RT = 21;		    /* Router. */
    public final static int NSAP = 22;		/* NSAP address. */
    public final static int NSAP_PTR = 23;	/* Reverse NSAP lookup  = deprecated). */
    public final static int SIG = 24;		    /* Security signature. */
    public final static int KEY = 25;		    /* Security key. */
    public final static int PX = 26;		    /* X.400 mail mapping. */
    public final static int GPOS = 27;		/* Geographical position  = withdrawn). */
    public final static int AAAA = 28;		/* Ip6 Address. */
    public final static int LOC = 29;		    /* Location Information. */
    public final static int NXT = 30;		    /* Next domain  (security). */
    public final static int EID = 31;		    /* Endpoint identifier. */
    public final static int NIMLOC = 32;	    /* Nimrod Locator. */
    public final static int SRV = 33;		    /* Server Selection. */
    public final static int ATMA = 34;		/* ATM Address */
    public final static int NAPTR = 35;	    /* Naming Authority PoinTeR */
    public final static int KX = 36;		    /* Key Exchange */
    public final static int CERT = 37;		/* Certification record */
    public final static int A6 = 38;		    /* IPv6 address  = deprecates AAAA) */
    public final static int DNAME = 39;	    /* Non-terminal DNAME  (for IPv6) */
    public final static int SINK = 40;		/* Kitchen sink  (experimentatl) */
    public final static int OPT = 41;		    /* EDNS0 option  (meta-RR) */
    public final static int TKEY = 249;	    /* Transaction key */
    public final static int TSIG = 250;	    /* Transaction signature. */
    public final static int IXFR = 251;	    /* Incremental zone transfer. */
    public final static int AXFR = 252;	    /* Transfer zone of authority. */
    public final static int MAILB = 253;	    /* Transfer mailbox records. */
    public final static int MAILA = 254;	    /* Transfer mail agent records. */
    public final static int ANY = 255;		/* Wildcard match. */
    public final static int ZXFR = 256;	    /* BIND-specific, nonstandard. */
    public final static int MAX = 65536;

}
