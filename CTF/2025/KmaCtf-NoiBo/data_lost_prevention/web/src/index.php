<?php require __DIR__ . '/lib/util.php'; ?>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>DLP Portal</title>
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

    h1 {
        font-weight: 700;
        margin: 0 0 8px
    }

    .nav {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 16px;
        margin-top: 16px
    }

    a.tile {
        display: block;
        background: #0b1220;
        border: 1px solid #1f2937;
        border-radius: 12px;
        padding: 18px;
        text-decoration: none;
        color: #cbd5e1
    }

    a.tile:hover {
        border-color: #334155;
        background: #0e172a
    }

    .small {
        color: #94a3b8;
        font-size: 13px
    }

    .badge {
        display: inline-block;
        background: #1e293b;
        border: 1px solid #334155;
        border-radius: 8px;
        padding: 2px 8px;
        margin-left: 8px;
        font-size: 12px
    }
    </style>
</head>

<body>
    <div class="container">
        <div class="card">
            <h1>DLP Portal</h1>
            <div class="small">Logged in as <b><?=h($_SESSION['username'])?></b></div>
            <p class="small">Mission: locate a "lost attachment" and export the right file.</p>
        </div>

        <div class="nav">
            <a class="tile" href="/cases.php">
                <div>Cases Search</div>
                <div class="small" title="Exports are under /var/log/app/">Search title… (Hint inside)</div>
            </a>

            <a class="tile" href="/attachments.php">
                <div>Attachment</div>
                <div class="small">Shows "lost" status only (no direct download)</div>
            </a>

            <a class="tile" href="/export-ui.php">
                <div>Export Logs</div>
                <div class="small">Only <code>.log</code> or <code>.txt</code> files are allowed</div>
            </a>
        </div>
    </div>
</body>

</html>