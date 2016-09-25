package to.augmented.reality.android.facadetest;

import android.graphics.Bitmap;
import to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapCircle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Format;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageDensity;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapOutputImage;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageHeight;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageWidth;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapType;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapsSource;
import to.augmented.reality.android.ardb.sourcefacade.maps.MapSource;

@MapsSource(name = "GoogleMaps", key = "AIzaSyDBocUN5N4JcQSCJpkXCfNt38IzPDaTrr4", source = MapSource.GOOGLE)
@MapCircle(radius = 1000)
public class GoogleMapQuery
//=========================
{
   @ImageWidth int imageWidth = 300;

   @ImageHeight int imageHeight = 300;

   @ImageDensity int density = 2;

   @MapType GoogleMapRequestor.MapType mapType = GoogleMapRequestor.MapType.roadmap;

   @Format GoogleMapRequestor.MapImageType imageType = GoogleMapRequestor.MapImageType.png;

   @MapOutputImage Bitmap image;
}
