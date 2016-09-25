package to.augmented.reality.android.ardb.sourcefacade.jdbc;

public class JoinContext
//======================
{
   /**
    * Table name
    */
   String table;
   public String getTable() { return table; }

   /**
    * Table alias
    */
   String alias;
   public String getAlias() { return alias; }

   /**
    * The join type
    */
   JoinType joinType;
   public JoinType getJoinType() { return joinType; }

   /**
    * The join (WHERE) condition
    */
   String joinCondition;
   public String getJoinCondition() { return joinCondition; }

   public JoinContext(String table, String alias, JoinType joinType, String joinCondition)
   //-------------------------------------------------------------------------------------
   {
      this.table = table;
      this.alias = alias;
      this.joinType = joinType;
      this.joinCondition = joinCondition;
   }
}
