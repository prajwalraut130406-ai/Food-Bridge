/* Register page: hotels and NGOs mark their location on a map (stored as latitude / longitude). */
(function () {
  var box = document.getElementById('locationBox');
  if (!box || typeof L === 'undefined') return;
  var role = document.getElementById('role');
  var addr = document.getElementById('address');
  var lat = document.getElementById('latitude');
  var lng = document.getElementById('longitude');
  var status = document.getElementById('locStatus');
  var map, marker;

  function setPoint(p, zoom) {
    lat.value = p[0].toFixed(6);
    lng.value = p[1].toFixed(6);
    if (!marker) {
      marker = L.marker(p, { draggable: true }).addTo(map);
      marker.on('dragend', function () { var q = marker.getLatLng(); setPoint([q.lat, q.lng]); });
    } else {
      marker.setLatLng(p);
    }
    if (zoom) map.setView(p, zoom);
    status.textContent = 'Location saved: ' + lat.value + ', ' + lng.value + '. Drag the pin to fine-tune.';
  }

  function init() {
    if (map) { map.invalidateSize(); return; }
    map = L.map('locMap').setView([20.5937, 78.9629], 5);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19, attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
    map.on('click', function (e) { setPoint([e.latlng.lat, e.latlng.lng]); });
    if (lat.value && lng.value) setPoint([parseFloat(lat.value), parseFloat(lng.value)], 15);
  }

  function toggle() {
    var needs = role.value === 'ROLE_HOTEL' || role.value === 'ROLE_NGO';
    box.classList.toggle('d-none', !needs);
    if (needs) setTimeout(init, 50);
  }

  document.getElementById('findOnMap').addEventListener('click', async function () {
    if (!addr.value.trim()) { status.textContent = 'Type the address first.'; return; }
    status.textContent = 'Searching...';
    try {
      var r = await fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(addr.value));
      var j = await r.json();
      if (!j.length) { status.textContent = 'Address not found. Click on the map to place the pin yourself.'; return; }
      setPoint([parseFloat(j[0].lat), parseFloat(j[0].lon)], 16);
    } catch (e) {
      status.textContent = 'Search failed. Click on the map to place the pin yourself.';
    }
  });

  role.addEventListener('change', toggle);
  toggle();
})();
