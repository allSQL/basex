package org.basex.core.proc;

import static org.basex.core.Commands.*;
import static org.basex.core.Text.*;
import java.io.File;
import org.basex.core.Prop;
import org.basex.io.PrintOutput;

/**
 * Evaluates the 'drop database' command and deletes a database.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class DropDB extends ACreate {
  /**
   * Default constructor.
   * @param name name of database
   */
  public DropDB(final String name) {
    super(STANDARD, name);
  }

  @Override
  protected boolean exec(final PrintOutput out) {
    new Close().execute(context, out);

    // check if database is still pinned
    final String db = args[0];
    if(context.prop.dblocked(db)) return error(DBLOCKED, db);

    // try to drop database
    return !prop.dbexists(db) ? error(DBNOTFOUND, db) :
      drop(db, prop) ? info(DBDROPPED, db) : error(DBNOTDROPPED);
  }

  /**
   * Deletes the specified database.
   * @param db database name
   * @param pr database properties
   * @return success flag
   */
  public static boolean drop(final String db, final Prop pr) {
    return delete(db, null, pr);
  }

  /**
   * Recursively deletes a database directory.
   * @param db database to delete
   * @param pat file pattern
   * @param pr database properties
   * @return success of operation
   */
  static synchronized boolean delete(final String db,
      final String pat, final Prop pr) {

    final File path = pr.dbpath(db);
    if(!path.exists()) return false;

    boolean ok = true;
    for(final File sub : path.listFiles()) {
      if(pat == null || sub.getName().matches(pat)) ok &= sub.delete();
    }
    if(pat == null) ok &= path.delete();
    return ok;
  }

  @Override
  public String toString() {
    return Cmd.DROP + " " + CmdDrop.DB + args();
  }
}
