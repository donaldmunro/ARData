package to.augmented.reality.android.ardb.tests.tiledtest;

import to.augmented.reality.android.ardb.sourcefacade.annotations.Circle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.DatabaseFile;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.From;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.Join;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.SelectColumn;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.SpatiaLiteSource;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.JoinType;

import java.io.File;

@SpatiaLiteSource(name = "SpatiaLite")
@From(value = "location", alias = "l")
@Join(table = "images", alias = "i", joinType = JoinType.INNER, joinCondition="l.location_id = i.location_id")
@Circle(column="wgs84_location", radius=50)
//@BoundingBox(column="wgs84_location", width = 1000, height = 1000)
public class SpatiaLiteQuery implements Cloneable
//===============================================
{
   String source = "SpatiaLite";


   @DatabaseFile File databaseFile;

   @SelectColumn(sequence = 0) String description;

   @SelectColumn(sequence = 1, column = "image") byte[] image;

   @Override public Object clone() throws CloneNotSupportedException { return super.clone(); }
}
