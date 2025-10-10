<?php
require __DIR__ . '/lib/util.php';
require __DIR__ . '/lib/db.php';

$rows = [];
$stmt = $pdo->prepare("SELECT id, filename, is_lost FROM attachments ORDER BY id ASC LIMIT 20");
$stmt->execute();
$rows = $stmt->fetchAll();
$lost = 0;
foreach ($rows as $r) if ((int)$r['is_lost'] === 1) $lost++;
?>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Attachments</title>
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

    .table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 10px
    }

    .table th,
    .table td {
        border-bottom: 1px solid #1f2937;
        padding: 10px 8px;
        text-align: left
    }

    .badge {
        display: inline-block;
        background: #1e293b;
        border: 1px solid #334155;
        border-radius: 8px;
        padding: 2px 8px;
        font-size: 12px
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
            <h2>Attachments</h2>
            <div class="small">Status: <b><?= $lost ?> attachment<?= $lost!==1?'s':'' ?> lost (restoration pending)</b>
            </div>
            <table class="table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Filename</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($rows as $r): ?>
                    <tr>
                        <td><?=h($r['id'])?></td>
                        <td><?=h($r['filename'])?></td>
                        <td>
                            <?php if ((int)$r['is_lost']===1): ?>
                            <span class="badge">LOST</span>
                            <?php else: ?>
                            OK
                            <?php endif; ?>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
            <p class="small">Direct downloads are disabled for security reasons.</p>
        </div>
        <a class="small" href="/">← Back</a>
    </div>
</body>

</html>