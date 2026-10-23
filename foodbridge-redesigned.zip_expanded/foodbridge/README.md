# FoodBridge

Spring Boot 3 + Thymeleaf + JPA + Spring Security. Requires JDK 17+ and Maven.

    mvn spring-boot:run

Open http://localhost:8080 , register one Hotel, one NGO and one Volunteer account
(use separate browsers / private windows) and walk the flow:

1. Hotel posts food  ->  2. NGO claims  ->  3. Volunteer accepts
4. Hotel generates pickup OTP  ->  volunteer verifies  ->  IN_TRANSIT
5. NGO generates delivery OTP  ->  volunteer verifies  ->  DELIVERED (+100 pts)

Default DB is a file-based H2 database in ./data. Switch to MySQL/PostgreSQL in
src/main/resources/application.properties.
Lombok is used: install the Lombok plugin in your IDE and enable annotation processing.

---

## Update: multi-item donations, photos, approximate value and route map

**Hotel**
* Post one donation with several food items ("+ Add Food Item"). Each item has: menu item (pick from the list or type your own),
  food type (Veg / Non-Veg), quantity + unit (kg / serving / liter) and an approximate value in rupees.
  The total approximate value is calculated automatically (live in the form, and again on the server).
* Optional photo of the food (JPG, PNG, GIF or WebP, max 5 MB). Files are stored under `uploads/food/` (change with `app.upload-dir`)
  and the URL is saved in `donations.food_image_url`. The type is checked from the file contents, names are random UUIDs.

**NGO** - every available donation is a card with the photo, the full item breakdown (type, quantity, unit, value) and the
**total approximate value**; the header shows the combined value of everything on offer. The same info appears under "My claimed donations".

**Volunteer** - a Leaflet / OpenStreetMap map shows the hotel (green **H**) and the NGO (red **N**) joined by the driving route,
with distance and time, plus an "Open in Google Maps" link. Use "Show route" / "Route" on any task.
* Hotels and NGOs mark their location on a map when they register (stored as `users.latitude` / `users.longitude`).
  If a user has no coordinates yet, the map geocodes their address with Nominatim in the browser.
* Routing uses the public OSRM demo server and tiles/geocoding use the public OpenStreetMap services: fine for a project/demo;
  for production use your own routing/tile provider and respect their usage policies. If routing is unavailable a straight line is drawn.

**Database changes** (`ddl-auto=update` creates the new table and columns automatically):
new table `donation_items`; `donations` gains `food_image_url` and `total_approximate_value`; `users` gains `latitude` and `longitude`.
The old single-item columns on `donations` are no longer used. On an existing database drop them, otherwise inserts fail because
`quantity` is NOT NULL:

```sql
ALTER TABLE donations DROP COLUMN quantity, DROP COLUMN quantity_unit, DROP COLUMN food_type;
```
(With the default H2 file database you can simply delete the `data/` folder instead.)
