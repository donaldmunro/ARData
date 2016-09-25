SELECT DiscardGeometryColumn('photos', 'location');
DROP TABLE IF EXISTS photos;
CREATE TABLE photos(id INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT, image BLOB);
SELECT AddGeometryColumn('photos', 'location', 4326, 'POINT', 'XY');
SELECT CreateSpatialIndex('photos', 'location');
INSERT INTO photos(description, location) VALUES ('test', GeomFromText('POINT(-34 26)', 4326));
SELECT * FROM photos;
