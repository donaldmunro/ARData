package to.augmented.reality.android.ardb;

import to.augmented.reality.android.ardb.http.sparql.GeoSparQLSpatialFunctions;
import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;
import to.augmented.reality.android.ardb.http.sparql.SparQLSpatialFunctions;
import to.augmented.reality.android.ardb.http.sparql.VirtuosoSpatialFunctions;
import to.augmented.reality.android.ardb.jdbc.DatabaseType;
import to.augmented.reality.android.ardb.jdbc.PostgresSpatialFunctions;
import to.augmented.reality.android.ardb.jdbc.SQLServerSpatialFunctions;
import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

public class SpatialFunctionFactory
//=================================
{
   public ISpatialFunctions create(DatabaseType type) { return create(type, 6); }

   public ISpatialFunctions create(DatabaseType type, int decimals)
   //--------------------------------------------------------------
   {
      switch (type)
      {
         case POSTGRES:    return new PostgresSpatialFunctions(decimals);
         case SQLSERVER:   return new SQLServerSpatialFunctions(decimals);
      }
      throw new RuntimeException("DatabaseType " + type + " not defined");
   }


   public ISpatialFunctions create(SparQLDialect type, int decimals)
   //---------------------------------------------------------------------------
   {
      switch (type)
      {
         case GEOSPARQL:   return new GeoSparQLSpatialFunctions(decimals);
         case SPARQL:      return new SparQLSpatialFunctions(decimals);
         case VIRTUOSO:    return new VirtuosoSpatialFunctions(decimals);
      }
      throw new RuntimeException("SemanticQL " + type + " not defined");
   }
}
