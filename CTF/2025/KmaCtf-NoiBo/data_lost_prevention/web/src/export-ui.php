<?php require __DIR__ . '/lib/util.php'; ?>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Export Logs</title>
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

    .small {
        color: #94a3b8;
        font-size: 13px
    }

    code {
        background: #0b1220;
        padding: 2px 6px;
        border-radius: 6px;
        border: 1px solid #1f2937
    }
    </style>
</head>

<body>
    <div class="container">
        <div class="card">
            <h2>Export Logs</h2>
            <p class="small">Only <code>.log</code> or <code>.txt</code> files are allowed. Base directory:
                <b>/var/log/app/</b></p>
            <form method="get" action="/export.php">
                <input name="file" type="text" placeholder="e.g. app.log">
                <div style="margin-top:10px">
                    <button type="submit">Download</button>
                </div>
            </form>
        </div>
        <a class="small" href="/">← Back</a>
    </div>
</body>

</html>