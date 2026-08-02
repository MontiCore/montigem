/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.db.sql;

import de.se_rwth.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects the identifiers whose name changes because {@link SQLNames} shortens them, and renders a
 * migration script for databases that were created before shortening was introduced.
 * <p>
 * Such a database carries the name PostgreSQL had silently truncated to 63 bytes. That name is
 * reproducible, so the rename is derived rather than guessed. Identifiers below the limit are not
 * shortened and therefore never show up here.
 */
public class SQLRenameGenerator {

  private record ColumnRename(String table, String from, String to) {
  }

  /** raw identifier -> identifier as it is emitted now */
  private final Map<String, String> tables = new LinkedHashMap<>();

  private final Set<ColumnRename> columns = new LinkedHashSet<>();

  /** computed once, so the collision warning is not emitted again for every caller */
  private Map<String, String> tableRenames;

  public void clear() {
    tables.clear();
    columns.clear();
    tableRenames = null;
  }

  public boolean isEmpty() {
    return tableRenames().isEmpty() && columns.isEmpty();
  }

  /**
   * @param rawName the identifier as it was built before shortening
   * @param name    the identifier as it is emitted now
   */
  public void addTable(String rawName, String name) {
    tables.put(rawName, name);
    tableRenames = null;
  }

  /**
   * @param table   the current (already shortened) name of the table holding the column
   * @param rawName the column as it was built before shortening
   * @param name    the column as it is emitted now
   */
  public void addColumn(String table, String rawName, String name) {
    if (!rawName.equals(name)) {
      columns.add(new ColumnRename(table, SQLNames.legacyTruncation(rawName), name));
    }
  }

  /**
   * The renames that can actually be carried out, keyed by the name the database still carries.
   * <p>
   * Identifiers that do not differ within the first 63 bytes were the same table to PostgreSQL, so no
   * rename can tell them apart again. Shortening gives them distinct names from now on, which is right
   * for a new database but leaves an existing one beyond repair. Those are reported and left out of the
   * script rather than renamed into one another.
   * <p>
   * Note that the grouping has to consider the identifiers that are <em>not</em> shortened as well: a
   * legacy name is always exactly 63 bytes, and that can be the current, still used name of another
   * table. Renaming it away would move a live table onto a name nothing addresses.
   */
  protected Map<String, String> tableRenames() {
    if (tableRenames != null) {
      return tableRenames;
    }
    Map<String, List<String>> byLegacyName = new LinkedHashMap<>();
    tables.keySet().forEach(rawName ->
        byLegacyName.computeIfAbsent(SQLNames.legacyTruncation(rawName), n -> new ArrayList<>()).add(rawName));

    tableRenames = new LinkedHashMap<>();
    byLegacyName.forEach((legacyName, rawNames) -> {
      if (rawNames.size() > 1) {
        Log.warn("0xdf10f PostgreSQL cannot tell the tables " + String.join(" and ", rawNames)
            + " apart, it truncates all of them to " + legacyName
            + ". They are left out of the rename script, because an already deployed database cannot be"
            + " migrated for them. Newly created databases are unaffected, they get distinct names."
            + " To resolve this, shorten the affected model names or give the associations an explicit"
            + " name in the class diagram.");
      }
      else if (!rawNames.get(0).equals(tables.get(rawNames.get(0)))) {
        tableRenames.put(legacyName, tables.get(rawNames.get(0)));
      }
    });
    return tableRenames;
  }

  public String render(String cdName) {
    StringBuilder script = new StringBuilder();
    script.append("-- Renames the identifiers of ").append(cdName)
        .append(" that PostgreSQL used to truncate silently at ").append(SQLNames.MAX_BYTES).append(" bytes.\n");
    script.append("-- Apply once to a database created before identifier shortening was introduced.\n");
    script.append("-- Every statement skips itself if it does not apply, so the script is safe to re-run\n");
    script.append("-- and safe on a freshly created database.\n");

    tableRenames().forEach((legacyName, name) -> script.append('\n')
        .append("DO $$ BEGIN\n")
        .append("  ALTER TABLE ").append(quote(legacyName)).append(" RENAME TO ").append(quote(name)).append(";\n")
        .append("EXCEPTION\n")
        .append("  WHEN undefined_table THEN RAISE NOTICE 'skipped, no table ").append(legacyName).append("';\n")
        .append("  WHEN duplicate_table THEN RAISE NOTICE 'skipped, ").append(name).append(" already exists';\n")
        .append("END $$;\n"));

    columns.forEach(column -> script.append('\n')
        .append("DO $$ BEGIN\n")
        .append("  ALTER TABLE ").append(quote(column.table()))
        .append(" RENAME COLUMN ").append(quote(column.from()))
        .append(" TO ").append(quote(column.to())).append(";\n")
        .append("EXCEPTION\n")
        .append("  WHEN undefined_table OR undefined_column THEN RAISE NOTICE 'skipped, no column ")
        .append(column.table()).append('.').append(column.from()).append("';\n")
        .append("  WHEN duplicate_column THEN RAISE NOTICE 'skipped, ")
        .append(column.table()).append('.').append(column.to()).append(" already exists';\n")
        .append("END $$;\n"));

    return script.toString();
  }

  /**
   * All generated identifiers are lower case, so quoting does not change which object is addressed. It
   * does make the {@code $} in list attribute tables unambiguous.
   */
  protected String quote(String identifier) {
    return "\"" + identifier + "\"";
  }
}
