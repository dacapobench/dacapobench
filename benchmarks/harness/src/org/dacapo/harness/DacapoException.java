/*
 * 
 */
package org.dacapo.harness;

/**
 * Exception class for local error conditions.
 * 
 * @author Robin Garner
 * @date $Date: 2009-12-03 11:33:16 +1100 (Thu, 03 Dec 2009) $
 * @id $Id: DacapoException.java 634 2009-12-03 00:33:16Z jzigman $
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
