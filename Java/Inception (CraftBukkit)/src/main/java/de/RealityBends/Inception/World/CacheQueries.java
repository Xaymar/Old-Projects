/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception.World;

public enum CacheQueries {
    CreateWorld("CreateWorld", false, "CREATE TABLE IF NOT EXISTS 'main'.'{0}' ('ChunkX' INTEGER NOT NULL, 'ChunkZ' INTEGER NOT NULL, 'X' INTEGER NOT NULL, 'Z' INTEGER NOT NULL, 'Y' INTEGER NOT NULL, 'Type' INTEGER NOT NULL, 'Data' TEXT NOT NULL, PRIMARY KEY ('ChunkX', 'ChunkZ', 'X', 'Z', 'Y', 'Type') ON CONFLICT REPLACE);"),
    DeleteWorld("DeleteWorld", false, "DROP TABLE IF EXISTS 'main'.'{0}'"),
    AddEvent("AddEvent", true, "INSERT OR REPLACE INTO 'main'.'{0}' VALUES(?1, ?2, ?3, ?4, ?5, ?6, ?7);"),
    GetEvents("GetEvents", true, "SELECT X, Z, Y, Type, Data FROM 'main'.'{0}' WHERE ChunkX = ?1 AND ChunkZ = ?2;"),
    RemoveEvent("RemoveEvent", true, "DELETE FROM 'main'.'{0}' WHERE ChunkX = ?1 AND ChunkZ = ?2 AND X = ?3 AND Z = ?4 AND Y = ?5 AND Type = ?6;");
    
    private String stName;
    private String stDefaultQuery;
    private boolean tIsPreparable;

    private CacheQueries(String pstName, boolean ptIsPreparable, String pstDefaultQuery) {
        stName = pstName;
        tIsPreparable = ptIsPreparable;
        stDefaultQuery = pstDefaultQuery;
    }
    
    public String getName() {
        return stName;
    }
    
    public boolean isPreparable() {
        return tIsPreparable;
    }

    public String getDefaultQuery() {
        return stDefaultQuery;
    }
}
