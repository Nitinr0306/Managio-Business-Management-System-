package com.managio.platform.persistence;

/** Helpers for SQL parameters built from what people type. */
public final class SqlText {

    private SqlText() {
    }

    /**
     * A LIKE pattern that finds the lower-cased term anywhere in a lower-cased column. %, _ and \ in the term
     * match themselves instead of acting as wildcards.
     */
    public static String containing(String term) {
        String escaped = term.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
