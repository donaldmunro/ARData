package to.augmented.reality.android.ardb.http.maps;

public class POI
//==============
{
   public enum POIColor { def, black, brown, green, purple, yellow, blue, gray, orange, red, white, user_defined };

   public enum POISize { def, tiny, mid, small }

   public float latitude, longitude;

   public String label = "";

   private POIColor color = POIColor.def;
   public POIColor getColor() { return color; }
   public void setColor(POIColor color)
   //----------------------------------
   {
      this.color = color;
      if ( (color == POIColor.user_defined) && ( (userDefinedColor == null) || (userDefinedColor.trim().isEmpty()) ) )
         userDefinedColor = "0x00FF00";
   }

   private String userDefinedColor = null; // eg "0xFFFFCC"
   public String getUserDefinedColor() { return (userDefinedColor != null) ? userDefinedColor : "0x00FF00"; }
   public void setUserDefinedColor(String color)
   //------------------------------------------
   {
      userDefinedColor = color;
      this.color = POIColor.user_defined;
   }

   public POISize size = POISize.def;

   public int xOffset =-1, yOffset =-1;

   private boolean isVisible = true;
   public void setIsVisible(boolean isVisible) { this.isVisible = isVisible; }
   public boolean isVisible() { return isVisible; }

   public POI(float latitude, float longitude, String label)
   {
      this(latitude, longitude, label, null, null, -1, -1, true);
   }

   public POI(float latitude, float longitude, boolean isVisible, String label)
   {
      this(latitude, longitude, label, null, null, -1, -1, isVisible);
   }

   public POI(float latitude, float longitude, String label, POIColor color, POISize size, int xOff, int yOff,
              boolean isVisible)
   //------------------------------------------------------------------------------------------------------------
   {
      this.latitude = latitude;
      this.longitude = longitude;
      this.label = label;
      this.color = color;
      this.size = size;
      xOffset = xOff;
      yOffset = yOff;
      this.isVisible = isVisible;
   }
}
