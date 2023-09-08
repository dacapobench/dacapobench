/*
 * 
 */
package dacapo;

/**
 * Exception class for local error conditions.
 * 
 * @author Robin Garner
 * @date $Date: 2006-09-28 11:11:06 +0000 (Thu, 28 Sep 2006) $
 * @id $Id: DacapoException.java 135 2006-09-28 11:11:06 +0000 (Thu, 28 Sep 2006) rgarner $
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
