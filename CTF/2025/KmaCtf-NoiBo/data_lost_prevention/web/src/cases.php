<?php require __DIR__ . '/lib/util.php'; ?>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Cases</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <style>
    body {
        font-family: system-ui, Segoe UI, Roboto, Arial, sans-serif;
        background: #0f172a;
        color: #e2e8f0;
        margin: 0
    }

    .container {
        max-width: 960px;
        margin: 40px auto;
        padding: 0 16px
    }

    .card {
        background: #111827;
        border: 1px solid #1f2937;
        border-radius: 12px;
        padding: 20px;
        margin-bottom: 16px
    }

    input[type=text] {
        width: 100%;
        padding: 12px 14px;
        border-radius: 10px;
        border: 1px solid #334155;
        background: #0b1220;
        color: #e2e8f0
    }

    button {
        padding: 10px 14px;
        border-radius: 10px;
        background: #1d4ed8;
        border: 0;
        color: #e2e8f0
    }

    .result {
        margin-top: 12px;
        font-size: 14px;
        color: #cbd5e1
    }

    .small {
        color: #94a3b8;
        font-size: 13px
    }
    </style>
</head>

<body>
    <div class="container">
        <div class="card">
            <h2>Cases Search</h2>
        </div>
        <p class="small">Type a keyword to search title…</p>
        <input id="q" type="text" placeholder="Search title…">
        <div style="margin-top:10px">
            <button onclick="doSearch()">Search</button>
        </div>
        <div id="out" class="result"></div>
    </div>
    <a class="small" href="/">← Back</a>
    </div>

    <script>
    async function doSearch() {
        const q = document.getElementById('q').value || '';
        const r = await fetch('/api/search.php?q=' + encodeURIComponent(q));
        const j = await r.json();
        let t = '';
        if (j.ok) {
            t = "Found case id: <b>" + j.id + "</b>";
        } else {
            t = "No result.";
        }
        document.getElementById('out').innerHTML = t;
    }
    </script>
</body>

</html>