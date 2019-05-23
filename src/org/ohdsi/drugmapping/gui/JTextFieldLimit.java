package org.ohdsi.drugmapping.gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class JTextFieldLimit extends PlainDocument {
	private static final long serialVersionUID = -4469423202266656874L;
	
	private int limit;

	  JTextFieldLimit(int limit) {
	   super();
	   this.limit = limit;
	   }

	  public void insertString( int offset, String  str, AttributeSet attr ) {
	    if (str == null) return;

	    if ((getLength() + str.length()) <= limit) {
	      try {
			super.insertString(offset, str, attr);
	      } 
	      catch (BadLocationException e) {
			// Do nothing
	      }
	    }
	  }

}
