select description, ST_AsText(wgs84_location) as location from location where wgs84_location && ST_MakeEnvelope(-34, 24,-35, 26,4326);
