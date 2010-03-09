package org.basex.data;

import static org.basex.util.Token.*;
import static org.basex.data.DataText.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.basex.core.Prop;
import org.basex.io.PrintOutput;
import org.basex.util.TokenBuilder;
import org.basex.util.Tokenizer;

/**
 * This class serializes XML results.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class XMLSerializer extends Serializer {
  /** Indentation. */
  private static final byte[] INDENT = { ' ', ' ' };
  /** New line. */
  private static final byte[] NL = token(Prop.NL);
  /** Colon. */
  private static final byte[] COL = { ':' };

  /** Output stream. */
  private final PrintOutput out;
  /** Indentation flag. */
  private boolean indent = true;
  /** URI for wrapped results. */
  private byte[] wrapUri;
  /** Prefix for wrapped results. */
  private byte[] wrapPre;
  /** Encoding. */
  private String enc = UTF8;

  /** Temporary indentation flag. */
  private boolean ind;
  /** Temporary item flag. */
  private boolean item;

  /**
   * Constructor.
   * @param o output stream reference
   * @throws IOException I/O exception
   */
  public XMLSerializer(final OutputStream o) throws IOException {
    this(o, null);
  }

  /**
   * Constructor.
   * @param o output stream reference
   * @param p serialization properties
   * @throws IOException I/O exception
   */
  public XMLSerializer(final OutputStream o, final Properties p)
      throws IOException {

    out = o instanceof PrintOutput ? (PrintOutput) o : new PrintOutput(o);

    boolean omit = true;
    if(p != null) {
      // parse indent
      String val = p.getProperty("indent");
      if(val != null) indent = Boolean.parseBoolean(val);
      // parse encoding
      val = p.getProperty("encoding");
      omit = val == null;
      if(!omit) enc = enc(val);
      // parse omit-xml-declaration 
      val = p.getProperty("omit-xml-declaration");
      if(val != null) omit = Boolean.parseBoolean(val);
      // hidden setting: set wrapping prefix
      val = p.getProperty("wrap-prefix");
      if(val != null) wrapPre = token(val);
      // hidden setting: set wrapping prefix
      val = p.getProperty("wrap-uri");
      if(val != null) wrapUri = token(val);
    }

    if(!omit) {
      print(PI1);
      print(DOCDECL);
      print(enc);
      print('\'');
      print(PI2);
      print(NL);
    }

    if(wrapPre != null) {
      openElement(concat(wrapPre, COL, RESULTS));
      attribute(concat(XMLNSC, wrapPre), wrapUri);
    }
  }

  /**
   * Doctype declaration.
   * @param t document root element tag
   * @param te external subset
   * @param ti internal subset
   * @throws IOException IOException
   */
  public void doctype(final byte[] t, final byte[] te,
      final byte[] ti) throws IOException {
    print(DOCTYPE);
    print(' ');
    print(t);
    if(te != null) print(" " + string(SYSTEM) + " \"" + string(te) + "\"");
    if(ti != null) print(" \"" + string(ti) + "\"");
    print(ELEM2);
    print(NL);
  }

  @Override
  public void cls() throws IOException {
    if(wrapPre != null) closeElement();
  }

  @Override
  public void openResult() throws IOException {
    if(wrapPre != null) openElement(concat(wrapPre, COL, RESULT));;
  }

  @Override
  public void closeResult() throws IOException {
    if(wrapPre != null) closeElement();
  }

  @Override
  public void attribute(final byte[] n, final byte[] v) throws IOException {
    print(' ');
    print(n);
    print(ATT1);
    for(int k = 0; k < v.length; k += cl(v[k])) {
      final int ch = cp(v, k);
      switch(ch) {
        case '"': print(E_QU);  break;
        case 0x9: print(E_TAB); break;
        case 0xA: print(E_NL); break;
        default:  ch(ch);
      }
    }
    print(ATT2);
  }

  @Override
  public void text(final byte[] b) throws IOException {
    finishElement();
    for(int k = 0; k < b.length; k += cl(b[k])) ch(cp(b, k));
    ind = false;
  }

  @Override
  public void text(final byte[] b, final FTPos ftp) throws IOException {
    finishElement();

    int c = -1, wl = 0;
    final Tokenizer ftt = new Tokenizer(b, null);
    while(ftt.more()) {
      c++;
      for(int i = wl; i < ftt.p; i += cl(b[i])) {
        final int ch = cp(b, i);
        if(ftChar(ch) && ftp.contains(c)) print((char) 0x10);
        ch(ch);
      }
      wl = ftt.p;
    }
    while(wl < b.length) {
      ch(cp(b, wl));
      wl += cl(b[wl]);
    }
    ind = false;
  }

  @Override
  public void comment(final byte[] n) throws IOException {
    finishElement();
    if(ind) indent(true);
    print(COM1);
    print(n);
    print(COM2);
  }

  @Override
  public void pi(final byte[] n, final byte[] v) throws IOException {
    finishElement();
    if(ind) indent(true);
    print(PI1);
    print(n);
    print(' ');
    print(v);
    print(PI2);
  }

  @Override
  public void item(final byte[] b) throws IOException {
    finishElement();
    if(ind) print(' ');
    for(int k = 0; k < b.length; k += cl(b[k])) ch(cp(b, k));
    ind = true;
    item = true;
  }

  /**
   * Prints a single character.
   * @param ch character to be printed
   * @throws IOException I/O exception
   */
  private void ch(final int ch) throws IOException {
    switch(ch) {
      case '&': print(E_AMP); break;
      case '>': print(E_GT); break;
      case '<': print(E_LT); break;
      case 0xD: print(E_CR); break;
      default : print(ch);
    }
  }

  @Override
  public boolean finished() {
    return out.finished();
  }

  @Override
  protected void start(final byte[] t) throws IOException {
    if(ind) indent(false);
    print(ELEM1);
    print(t);
    ind = indent;
  }

  @Override
  protected void empty() throws IOException {
    print(ELEM4);
  }

  @Override
  protected void finish() throws IOException {
    print(ELEM2);
  }

  @Override
  protected void close(final byte[] t) throws IOException {
    if(ind) indent(true);
    print(ELEM3);
    print(t);
    print(ELEM2);
    ind = indent;
  }

  /**
   * Prints the text declaration to the output stream.
   * @param close close flag
   * @throws IOException I/O exception
   */
  private void indent(final boolean close) throws IOException {
    if(item) {
      item = false;
      return;
    }
    print(NL);
    final int s = level() + (close ? 1 : 0);
    for(int l = 1; l < s; l++) print(INDENT);
  }

  /**
   * Writes a token in the current encoding.
   * @param token token to be printed
   * @throws IOException I/O exception
   */
  private void print(final byte[] token) throws IOException {
    // comparison by reference
    if(enc == UTF8) {
      out.write(token);
    } else {
      print(string(token));
    }
  }

  /**
   * Writes a character in the current encoding.
   * @param ch character to be printed
   * @throws IOException I/O exception
   */
  private void print(final int ch) throws IOException {
    if(ch < 0x80) {
      out.write(ch);
    } else if(ch < 0xFFFF) {
      print(String.valueOf((char) ch));
    } else {
      print(new TokenBuilder().addUTF(ch).toString());
    }
  }

  /**
   * Writes a string in the current encoding.
   * @param s string to be printed
   * @throws IOException I/O exception
   */
  private void print(final String s) throws IOException {
    // comparison by reference
    if(enc == UTF8) {
      out.write(token(s));
    } else {
      final boolean le = enc == UTF16LE;
      if(enc == UTF16BE || le) {
        for(int i = 0; i < s.length(); i++) {
          final char ch = s.charAt(i);
          out.write(le ? ch & 0xFF : ch >>> 8);
          out.write(le ? ch >>> 8 : ch & 0xFF);
        }
      } else {
        out.write(s.getBytes(enc));
      }
    }
  }
}
