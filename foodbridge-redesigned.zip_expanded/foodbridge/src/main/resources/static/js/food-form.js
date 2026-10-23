/* Hotel donation form: dynamic food-item rows, live total, photo preview. */
(function () {
  var rows = document.getElementById('itemRows');
  if (!rows) return;
  var tpl = document.getElementById('itemRowTemplate');
  var NON_VEG = /chicken|mutton|fish|egg|prawn|lamb|beef|pork|keema/i;

  // Spring binds items[0], items[1], ... so the names must stay contiguous after add/remove.
  function reindex() {
    var all = rows.querySelectorAll('.item-row');
    all.forEach(function (row, i) {
      row.querySelector('.row-number').textContent = i + 1;
      row.querySelectorAll('[data-field]').forEach(function (el) {
        el.name = 'items[' + i + '].' + el.dataset.field;
        el.id = 'items' + i + '_' + el.dataset.field;
      });
      row.querySelector('.remove-row').disabled = all.length === 1;
    });
    updateTotal();
  }

  function updateTotal() {
    var total = 0;
    rows.querySelectorAll('[data-field="approximatePrice"]').forEach(function (el) {
      var v = parseFloat(el.value);
      if (!isNaN(v)) total += v;
    });
    var out = document.getElementById('totalValue');
    if (out) out.textContent = '\u20B9 ' + total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  document.getElementById('addItemBtn').addEventListener('click', function () {
    var node = tpl.content.firstElementChild.cloneNode(true);
    rows.appendChild(node);
    reindex();
    node.querySelector('[data-field="itemName"]').focus();
  });

  rows.addEventListener('click', function (e) {
    var btn = e.target.closest('.remove-row');
    if (btn && rows.querySelectorAll('.item-row').length > 1) {
      btn.closest('.item-row').remove();
      reindex();
    }
  });

  rows.addEventListener('input', function (e) {
    if (e.target.dataset.field === 'approximatePrice') updateTotal();
  });

  // picking a dish suggests Veg / Non-Veg (the hotel can still change it)
  rows.addEventListener('change', function (e) {
    if (e.target.dataset.field === 'itemName') {
      var type = e.target.closest('.item-row').querySelector('[data-field="foodType"]');
      type.value = NON_VEG.test(e.target.value) ? 'NON_VEG' : 'VEG';
    }
  });

  document.getElementById('donationForm').addEventListener('submit', reindex);

  // photo preview + client-side check (the server validates again)
  var file = document.getElementById('foodImage');
  var prev = document.getElementById('imagePreview');
  file.addEventListener('change', function () {
    var f = file.files[0];
    if (!f) { prev.classList.add('d-none'); return; }
    if (!/^image\/(jpeg|png|gif|webp)$/.test(f.type)) {
      alert('Please choose a JPG, PNG, GIF or WebP image.');
      file.value = ''; prev.classList.add('d-none'); return;
    }
    if (f.size > 5 * 1024 * 1024) {
      alert('The image is larger than 5 MB.');
      file.value = ''; prev.classList.add('d-none'); return;
    }
    prev.src = URL.createObjectURL(f);
    prev.classList.remove('d-none');
  });

  reindex();
})();
