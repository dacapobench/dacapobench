/*
 * 
 */
package org.dacapo.harness;

/**
 * Exception class for local error conditions.
 * 
 * @author Robin Garner
 * @date $Date: 2008-07-26 11:23:30 +1000 (Sat, 26 Jul 2008) $
 * @id $Id: DacapoException.java 379 2008-07-26 01:23:30Z steveb-oss $
 *
 */
public class DacapoException extends Exception {
  public DacapoException(String text) {
    super(text);
  }
  
  public DacapoException(Exception e) {
    super(e);
  }
  
  private static final long serialVersionUID = 3726765834685635667l;
}
