package org.apache.geronimo.samples.daytrader.dacapo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.apache.geronimo.samples.daytrader.*;
import org.apache.geronimo.samples.daytrader.direct.TradeDirect;
import org.apache.geronimo.samples.daytrader.soap.TradeWebSoapProxy;
import org.apache.geronimo.samples.daytrader.util.Log;

public class DaCapoTrader extends Thread {

  private static final boolean VERBOSE = false;

  static final int MAX_INITIALIZATION_WAIT_CYCLES = 120;
  static final int PAUSE_MS = 1000;
  static final int MIN_WORK_PARTITIONING_FACTOR = 4;  // higher -> better load balancing but more contention on session table

  private int[] consumed;
  private static String[] tradeSessions;
  private int threads;
  private String size;
  private TradeServices trade = null;
  private static final int MAX_TRANSACTION_RETRIES = 5;
  private static final int OP_H = 0, OP_P = 1, OP_Q = 2, OP_B = 3, OP_S = 4, OP_U = 5, OP_R = 6, OP_L = 7, OP_O = 8, OP_NQ = 9;
  private static final String[]OP_NAMES      = {"Home", "Portfolio", "Quote", "Buy", "Sell", "Update", "Register", "Login", "Logout", "Nested Quote"};
  private static final int[] MAX_OP_ATTEMPTS = {     5,           5,       5,     5,      5,        5,          5,       5,        5,             5};
  private static int[] opCount = new int[OP_NAMES.length];
  private int[] localOpCount = new int[OP_NAMES.length];
  private boolean soap;
  private static int sessionStride = 1;
  private int sessionIndex = -1;
  private int sessionBound = -1;
  private int threadID = -1;
  
  public DaCapoTrader() {}
  
  public DaCapoTrader(boolean soap, int[] completed, int ordinal, int threads, String size) {
    try {
      this.consumed = completed;
      this.threads = threads;
      this.size = size;
      this.soap = soap;
      this.threadID = ordinal;
      try {
        if (VERBOSE) System.err.println("["+threadID+"] Creating Trade Action");
        if (soap) 
          trade = new TradeWebSoapProxy();
        else
          trade = new TradeDirect();
        if (VERBOSE) System.err.println("["+threadID+"] Created Trade Action");
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("Caught exception while creating trader "+ e.toString());  
      e.printStackTrace();
    }
  }
  
  private void reset() {
    try {
      trade.resetDaCapo(size, threads);
    } catch (Exception e) {
      System.err.println("Caught exception while resetting DaCapo workload: "+e.toString());
      e.printStackTrace();
    }
    int ordinal;
    synchronized(consumed) {
      ordinal = consumed[1]++;
      for (int i = 0; i < OP_NAMES.length; i++) localOpCount[i] = 0;  // reset local count
      
      /* last to finish does some cleanup and announcements */
      if (consumed[1] == threads) { 
        for (int i = 0; i < OP_NAMES.length; i++) opCount[i] = 0;     // reset global count
        consumed[1] = 0;
        System.out.println("Finished repopulating database");
        System.out.println("Running "+tradeSessions.length+" trade sessions "+(soap ? "from client via soap" : "directly on server"));
      }
      
      if (VERBOSE) System.err.println("["+threadID+"] reached barrier: "+ordinal);
      consumed.notify();
    }
    synchronized(consumed) {
      while(consumed[1] != 0) {
        try {
          consumed.wait();
          if (VERBOSE) System.err.println("["+threadID+"] Completed: "+consumed[1]);
        } catch (InterruptedException e) {
          System.err.println("Caught exception while waiting: "+e.toString());
          e.printStackTrace();
        }
      }
    }
    if (VERBOSE) System.err.println("["+threadID+"] completed reset(), thread "+ordinal);
  }
  
  public void run() {
    reset();
    if (VERBOSE) System.err.println("["+threadID+"] starting trading");
    String tradeSession;
    
    while((tradeSession = getNextTradeSession()) != null) {
      runTradeSession(tradeSession);
    }
  }
  
  
  private boolean increaseSessionBound() {
    boolean available = false;
    int alreadyConsumed = 0;
    
    synchronized(consumed) {
      for (int i = 0; i < OP_NAMES.length; i++) opCount[i] += localOpCount[i];
      alreadyConsumed = consumed[0];
      int remaining = tradeSessions.length - consumed[0];
      if (remaining > 0) {  // more work to do
        sessionIndex = consumed[0];
        consumed[0] += (remaining >= sessionStride) ? sessionStride : remaining;
        sessionBound = consumed[0];
        available = true;
      } else {              // we're done
        consumed[0]++;
        if (alreadyConsumed  == tradeSessions.length + threads - 1)
        printReport();
      }
      consumed.notify();
    }

    for (int i = 0; i < OP_NAMES.length; i++) localOpCount[i] = 0;
    
//    if (available && sessionBound % (50 * sessionStride) == 0)
//      System.out.println(sessionBound+" trade sessions...");
    return available;
  }
  
  private String getNextTradeSession() {
    if (sessionIndex < sessionBound || increaseSessionBound()) {
      return tradeSessions[sessionIndex++];
    } else {
      return null;
    }
  }
      

  private void printReport() {
    int total = 0;
    int nested = opCount[OP_NQ];
    for (int i = 0; i < OP_NAMES.length; i++) { 
      total += opCount[i];
    }
    String dots = "........................";
    System.out.println("Completed "+tradeSessions.length+" trade sessions comprising "+(total-nested)+" trader actions");
    for (int i = 0; i < OP_NAMES.length; i++) {
      if (i != OP_NQ)
        System.out.format("\t"+OP_NAMES[i]+" "+dots.substring(OP_NAMES[i].length())+"%6d (%4.1f%%)%n",  opCount[i], 100*((float) opCount[i]/(total-nested)));
    }
    System.out.flush();
  }
  
  public static void initializeTrade(final String size) {
    Thread initializer = new Thread(new Runnable() {
      public void run() {
        TradeServices initTrade = new TradeWebSoapProxy();
        if (VERBOSE) System.err.println("Initializing...");
        for (int i = 0; i < MAX_INITIALIZATION_WAIT_CYCLES; i++) {
          try {
            initTrade.initializeDaCapo(size);
             return;
          } catch (Exception e) {
            if (i == MAX_INITIALIZATION_WAIT_CYCLES - 1) {
              System.err.println("Error initializing DaCapo: " + e.toString());
              e.printStackTrace();
              System.exit(0);
            } else {
              if (VERBOSE) System.err.println("Attempt " + i + " failed...");
              try {
                Thread.sleep(PAUSE_MS);
              } catch (InterruptedException ie) {}
            }
          }
        }
      }});
    initializer.start();
    try {
      initializer.join();
    } catch (InterruptedException e) {
      System.err.println("Error initializing DaCapo: " + e.toString());
      e.printStackTrace();
      System.exit(0);
    }
  }
  
  public static void setSessionStride(int numSessions, int numThreads) {
    int idealStride = numSessions / (MIN_WORK_PARTITIONING_FACTOR * numThreads);
    if (idealStride > sessionStride)
      sessionStride = idealStride;
  }
  
  public int loadWorkload(String size) {
    try {
      String fileName = "workload.txt";
      URL workloadFile = getURL(fileName);
      if (workloadFile == null) {
        String msg = "DaCapoTrader: workload input file does not exist at path "+fileName+" , please provide the file and retry";
        Log.error(msg);
        System.err.println(msg);
        return -1;
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(workloadFile.openStream()));
      
      int numSessions = parseHeader(br.readLine().trim(), size);
      
      if (VERBOSE) System.err.println("["+threadID+"] Successfully read header.  Will load "+numSessions+" sessions");
      tradeSessions = new String[numSessions];
      
      String s;
      int i = 0;
      while (i < numSessions && (s = br.readLine().trim()) != null) {
        if ((s.length() != 0) && (s.charAt(0) != '#')) { // Empty lines or lines starting with "#" are ignored
          tradeSessions[i++] = s;
          if (VERBOSE) System.err.println("["+threadID+"] Loaded session: "+s);
        }
      }
      if (i != numSessions) {
        String msg = "DaCapoDBBuilder: could only read "+i+" of "+numSessions+" users from input file "+ fileName +" , please correct the file and retry";
        Log.error(msg);
        System.err.println(msg);
        return -1;
      }
      return numSessions;
    } catch (Exception e) {
      String msg = "Unable to load workload: "+e;
      Log.error(msg);
      System.err.println(msg);
      return -1;
    }
  }
  
  private static int parseHeader(String s, String size) {
    int users = 0;
    String[] header = s.split("\t");
    for (int h = 0; h < header.length; h++) {
      if (header[h].indexOf(size) != -1) {
        try {
          users = Integer.parseInt(header[h].substring(header[h].indexOf(": ")+2));
        } catch (NumberFormatException e) {
          String msg = "DaCapoDBTrader: garbled size field in user header: "+header[h];
          Log.error(msg);
          System.err.println(msg);
          return 0;            
        }
      }
    }
    return users;
  }

  
  private void runTradeSession(String session) {
    String[] entries = session.split("\t");
    if (VERBOSE) System.err.println("["+threadID+"] Session: "+entries.length+" --> "+session);
    String uid = entries[0];
    String passwd = entries[1];
    doLogin(uid, passwd);
    int tx = 2;
    while (tx < entries.length) {
      char op = entries[tx].charAt(0);
      String request = (entries[tx].length() > 1) ? entries[tx].substring(2) : null;
      switch (op) {
      case 'h':
        doHome(uid);
        break;
      case 'p':
        doPortfolio(uid);
        break;
      case 'q':
        doQuote(request);
        break;
      case 'b':
        doBuy(uid, request);
        break;
      case 'u':
        doUpdate(uid, request);
        break;
      case 'r':
        uid = doRegister(uid, request);
        if (uid == null) return;
        break;
      case 's':
        doSell(uid, request);
        break;
      }
      tx++;
    }
    doLogout(uid);
  }
  
  private int doLogin(String uid, String password) {
    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_L]; i++) {
      try {
        if (VERBOSE) System.err.println("["+threadID+"] Logging in "+uid+"...");
        AccountDataBean account = trade.login(uid, password);
        int rtn = account.hashCode();
        localOpCount[OP_L]++;
        return rtn;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_L] - 1) {
          System.out.println("Error logging in "+uid+": " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return 0;
  }
  
  private int doLogout(String uid) {
    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_O]; i++) {
      try {
        if (VERBOSE) System.err.println("["+threadID+"] Logging out "+uid+"...");
        trade.logout(uid);
        localOpCount[OP_O]++;
        return 1;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_O] - 1) {
          System.out.println("Error logging out "+uid+": " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return 0;
  }
  
  private int doHome(String uid) {
    int rtn = 0;
    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_H]; i++) {
      try {
        rtn = 0;
        if (VERBOSE) System.err.println("["+threadID+"] Getting home page for "+uid+"...");
        AccountDataBean accountData = trade.getAccountData(uid);
        rtn += accountData.hashCode();
        if (VERBOSE) System.err.println("["+threadID+"] Account for "+uid+": "+accountData.toString());
        Collection holdingDataBeans = trade.getHoldings(uid);
        if (holdingDataBeans == null) {
          System.err.println("User "+uid+" has no holdings.");
        } else if (holdingDataBeans.size() > 0) {
          Iterator it = holdingDataBeans.iterator();
          HoldingDataBean holdingData = null;
          while (it.hasNext()) {
            holdingData = (HoldingDataBean) it.next();
            if (holdingData == null && VERBOSE) {
              System.err.println("Collection contained null holding!");
            } else {
              rtn += holdingData.hashCode();
            }
          }
        }
        localOpCount[OP_H]++;
        return rtn;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_H] - 1) {
          System.err.println("Error getting home page for user "+uid+": " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return rtn;
  }
  
  private int doPortfolio(String uid) {
    int rtn = 0;
    int initNQCount = localOpCount[OP_NQ];
    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_P]; i++) {
      try {
        rtn = 0;
        if (VERBOSE) System.err.println("["+threadID+"] Getting portfolio for "+uid+"...");
        Collection holdingDataBeans = trade.getHoldings(uid);
        rtn += holdingDataBeans.hashCode();
        if (holdingDataBeans == null) {
          System.err.println("User "+uid+" has no holdings.");
        } else if (holdingDataBeans.size() > 0) {
          Iterator it = holdingDataBeans.iterator();
          HoldingDataBean holdingData = null;
          while (it.hasNext()) {
            holdingData = (HoldingDataBean) it.next();
            if (holdingData == null) {
              System.err.println("Collection contained null holding!");
            } else {
              rtn += doQuote(holdingData.getQuoteID(), true);
            }
          }
        }
        localOpCount[OP_P]++;
        return rtn;
      } catch (Exception e) {
        localOpCount[OP_NQ] = initNQCount;  // don't count aborted ops
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_P] - 1) {
          System.err.println("Error getting portfolio for "+uid+": " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return rtn;
  }
  
  private int doQuote(String symbol) {
    return doQuote(symbol, false);
  }
  
  private int doQuote(String symbol, boolean nested) {
    for (int i = 0; i < MAX_OP_ATTEMPTS[nested ? OP_NQ : OP_Q]; i++) {
      try {
        if (VERBOSE) System.err.println("["+threadID+"] Quoting "+symbol);
        QuoteDataBean quote = trade.getQuote(symbol);
        if (VERBOSE) System.err.println("["+threadID+"] Quote for "+symbol+" is "+quote.toString());
        int rtn = quote.hashCode();
        localOpCount[nested ? OP_NQ : OP_Q]++;
        return rtn;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[nested ? OP_NQ : OP_Q] - 1) {
          System.err.println("Error getting quote of "+symbol+": " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return 0;
  }
  
  private int doBuy(String uid, String request) {
    String[] rq = request.split(" ");
    String symbol = null;
    int qty = 0;
    try {
      symbol = rq[0];
      qty = Integer.parseInt(rq[1]);
    } catch (Exception e){
      System.err.println("Error parsing buy for "+uid+" (\""+request+"\"):" + e.toString());
      e.printStackTrace();
    }
    if (VERBOSE) System.out.println("Buying "+qty+" "+symbol+" for "+uid);

    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_B]; i++) {
      try {
        OrderDataBean order = trade.buy(uid, symbol, qty, TradeConfig.SYNCH);
        int rtn = order.hashCode();
        localOpCount[OP_B]++;
        return rtn;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_B] - 1) {
          System.out.println("Error performing buy of "+request+" for "+uid+": " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return 0;
  }

  private int doSell(String uid, String request) {
    String[] rq = request.split(" ");
    String symbol = null;
    int qty = 0;
    try {
      symbol = rq[0];
      qty = Integer.parseInt(rq[1]);
    } catch (Exception e){
      System.err.println("Error parsing sell for "+uid+" (\""+request+"\"): " + e.toString());
      e.printStackTrace();
    }
    if (VERBOSE) System.err.println("["+threadID+"] Selling "+request+" for "+uid);

    Integer holdingID = getHoldingIDFromHoldings(uid, symbol, qty);
    if (holdingID != null) {
      for (int i = 0; i < MAX_OP_ATTEMPTS[OP_S]; i++) {
        try {
          OrderDataBean order = trade.sell(uid, holdingID, TradeConfig.SYNCH);
          int rtn = order.hashCode();
          localOpCount[OP_S]++;
          return rtn;
        } catch (Exception e) {
          if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_S] - 1) {
            System.out.println("Error performing sell of "+request+" for "+uid+": " + e.toString());
            e.printStackTrace();
          }
        }
      }
    } else {
      System.err.println("Could not perform sell of "+symbol+" "+qty);
    }
    return 0;
  }
  
  private Integer getHoldingIDFromHoldings(String uid, String symbol, int qty) {
    for (int i = 0; i < MAX_TRANSACTION_RETRIES - 1; i++) {
      try {
        Collection holdingDataBeans = trade.getHoldings(uid);
        if (holdingDataBeans != null && holdingDataBeans.size() > 0) {
          Iterator it = holdingDataBeans.iterator();
          HoldingDataBean holdingData = null;
          while (it.hasNext()) {
            holdingData = (HoldingDataBean) it.next();
            if (holdingData != null && holdingData.getQuantity() == qty && holdingData.getQuoteID().equalsIgnoreCase(symbol)) {
              return holdingData.getHoldingID();
            }
          }
        }
      } catch (Exception e) {
        if (VERBOSE || i == MAX_TRANSACTION_RETRIES - 1) {
          System.out.println("Error getting holdings: " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return null;
  }
  
  private int doUpdate(String uid, String request) {
    if (VERBOSE) System.err.println("["+threadID+"] Updating "+uid);
    String passwd = null, name = null, address = null, email = null, ccard = null;
    String[] details = request.split("\\|");
    try {
      passwd = details[2];
      name = details[3];
      address = details[4];
      email = details[1];
      ccard = details[0];
    } catch (Exception e){
      System.err.println("Error parsing update for "+uid+"(\""+request+"\")" + e.toString());
      e.printStackTrace();
    }
    AccountProfileDataBean profile = new AccountProfileDataBean(uid, passwd, name, address, email, ccard);

    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_U] - 1; i++) {
      try {
        AccountProfileDataBean account = trade.updateAccountProfile(profile);
        int rtn = account.hashCode();
        localOpCount[OP_U]++;
        return rtn;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_U]) {
          System.out.println("Error updating profile: " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return 0;
  }
    
  private String doRegister(String uid, String request) {
    String newuid = null, passwd = null, name = null, address = null, email = null, ccard = null;
    java.math.BigDecimal balance = null;
    String[] details = request.split("\\|");
    try {
      newuid = details[0];
      passwd = details[4];
      name = details[5];
      address = details[6];
      email = details[3];
      ccard = details[2];
      balance = new java.math.BigDecimal(details[1]);
    } catch (Exception e){
      System.err.println("Error parsing register for "+uid+"(\""+request+"\")" + e.toString());
      e.printStackTrace();
    }    
    if (VERBOSE) System.err.println("["+threadID+"] Registering " + newuid);

    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_O] - 1; i++) {
      try {
        trade.logout(uid);
        localOpCount[OP_O]++;
        break;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_O] - 1) {
          System.out.println("Error logging out: " + e.toString());
          e.printStackTrace();
        }
      }
    }

    for (int i = 0; i < MAX_OP_ATTEMPTS[OP_R] - 1; i++) {
      try {
        trade.register(newuid, passwd, name, address, email, ccard, balance);
        localOpCount[OP_R]++;
        break;
      } catch (Exception e) {
        if (VERBOSE || i == MAX_OP_ATTEMPTS[OP_R] - 1) {
          System.out.println("Error registering new user: " + e.toString());
          e.printStackTrace();
        }
      }
    }
    return newuid; // return new user id
  }
  
    /**
   * Translate a resource name into a URL.
   *
   * @param fn
   * @return
   */
  public static URL getURL(String fn) {
    ClassLoader cl = DaCapoTrader.class.getClassLoader();
    URL resource = cl.getResource(fn);
    if (VERBOSE) System.err.println("Util.getURL: returns "+resource);
    return resource;
  }
  
}
