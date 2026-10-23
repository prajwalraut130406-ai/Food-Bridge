/*
 * Volunteer route map (Leaflet + OpenStreetMap).
 * Every ".js-show-route" button carries data-src-* (hotel) and data-dst-* (NGO) attributes.
 * Coordinates are used when present; otherwise the address is geocoded with Nominatim.
 * The driving route comes from the public OSRM demo server (fine for demos; use your own
 * OSRM / a routing provider in production). If routing fails a straight dashed line is drawn.
 */
(function () {
  var el = document.getElementById('routeMap');
  if (!el || typeof L === 'undefined') return;

  var map = L.map('routeMap').setView([20.5937, 78.9629], 5);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(map);
  var layer = L.layerGroup().addTo(map);

  var title = document.getElementById('routeTitle');
  var info = document.getElementById('routeInfo');
  var gmaps = document.getElementById('gmapsLink');
  var cache = {};

  function num(v) {
    var n = parseFloat(v);
    return isNaN(n) ? null : n;
  }

  async function geocode(addr) {
    if (!addr) return null;
    if (cache[addr]) return cache[addr];
    var r = await fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(addr));
    var j = await r.json();
    if (j.length) {
      cache[addr] = [parseFloat(j[0].lat), parseFloat(j[0].lon)];
      return cache[addr];
    }
    return null;
  }

  function pin(letter, color) {
    var div = document.createElement('div');
    div.textContent = letter;
    div.style.cssText = 'width:32px;height:32px;border-radius:50%;background:' + color +
      ';color:#fff;font-weight:700;display:flex;align-items:center;justify-content:center;border:3px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.5)';
    return L.divIcon({ html: div.outerHTML, className: '', iconSize: [32, 32], iconAnchor: [16, 16], popupAnchor: [0, -16] });
  }

  function popup(label, name, address) {
    // built with textContent so hotel / NGO names can never inject HTML
    var box = document.createElement('div');
    var b = document.createElement('strong'); b.textContent = label;
    box.appendChild(b);
    box.appendChild(document.createElement('br'));
    box.appendChild(document.createTextNode(name || ''));
    if (address) {
      box.appendChild(document.createElement('br'));
      var s = document.createElement('small'); s.textContent = address;
      box.appendChild(s);
    }
    return box;
  }

  function say(text, cls) {
    info.className = 'route-info px-1 ' + (cls || 'text-muted');
    info.textContent = text;
  }

  async function show(btn) {
    var d = btn.dataset;
    layer.clearLayers();
    title.textContent = (d.srcName || 'Hotel') + '  \u2192  ' + (d.dstName || 'NGO');
    say('Loading route...');
    gmaps.classList.add('d-none');

    var src, dst;
    try {
      var sLat = num(d.srcLat), sLng = num(d.srcLng), dLat = num(d.dstLat), dLng = num(d.dstLng);
      src = sLat !== null && sLng !== null ? [sLat, sLng] : await geocode(d.srcAddress);
      dst = dLat !== null && dLng !== null ? [dLat, dLng] : await geocode(d.dstAddress);
    } catch (e) {
      say('Could not look up the addresses (network problem).', 'text-danger');
      return;
    }
    if (!src || !dst) {
      say('One of the locations could not be found on the map. Check the hotel / NGO address.', 'text-danger');
      return;
    }

    L.marker(src, { icon: pin('H', '#1d7a4c') }).addTo(layer).bindPopup(popup('Pickup: Hotel', d.srcName, d.srcAddress));
    L.marker(dst, { icon: pin('N', '#c43a2f') }).addTo(layer).bindPopup(popup('Drop-off: NGO', d.dstName, d.dstAddress));
    var bounds = L.latLngBounds([src, dst]);
    map.fitBounds(bounds, { padding: [40, 40] });

    gmaps.href = 'https://www.google.com/maps/dir/?api=1&travelmode=driving&origin=' + src[0] + ',' + src[1] +
      '&destination=' + dst[0] + ',' + dst[1];
    gmaps.classList.remove('d-none');

    try {
      var url = 'https://router.project-osrm.org/route/v1/driving/' + src[1] + ',' + src[0] + ';' + dst[1] + ',' + dst[0] +
        '?overview=full&geometries=geojson';
      var res = await fetch(url);
      var data = await res.json();
      if (data.code !== 'Ok' || !data.routes.length) throw new Error('no route');
      var route = data.routes[0];
      var line = L.geoJSON(route.geometry, { style: { color: '#1f6fa8', weight: 5, opacity: 0.85 } }).addTo(layer);
      map.fitBounds(line.getBounds(), { padding: [40, 40] });
      say('Driving route: ' + (route.distance / 1000).toFixed(1) + ' km, about ' + Math.round(route.duration / 60) + ' min.', 'text-success');
    } catch (e) {
      L.polyline([src, dst], { color: '#1f6fa8', weight: 4, dashArray: '8 8' }).addTo(layer);
      say('Road route unavailable right now, showing a straight line between the two places.', 'text-warning');
    }
  }

  document.querySelectorAll('.js-show-route').forEach(function (b) {
    b.addEventListener('click', function () {
      show(b);
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  });

  // open the first task (active ones are listed first) automatically
  var first = document.querySelector('.js-show-route');
  if (first) {
    show(first);
  } else {
    title.textContent = 'No delivery selected';
    say('Accept a task to see the route from the hotel to the NGO.');
  }
})();
