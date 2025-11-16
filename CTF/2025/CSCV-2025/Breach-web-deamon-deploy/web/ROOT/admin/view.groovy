import java.nio.file.Paths

def patchme = evaluate(new File(webroot + "/patch/patchme.groovy"))
if (patchme != null) {
    return patchme
}

def webrootPath = Paths.get(webroot).toAbsolutePath().normalize()
def param = request.getParameter("path") ?: ""
def requested = webrootPath.resolve("home").resolve(param).normalize()

System.out.println(requested)


def out = new StringBuilder()

def cssLink = "admin.css" // adjust if your css location differs

def escapeHtml = { String s ->
    if (s == null) return ""
    s.replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;')
}

if (!param) {
    out << """<html><head><meta charset="UTF-8"><title>View File - Admin</title>
    <link rel="stylesheet" href="${cssLink}">
    <style>
      .content-box { max-width:900px; margin:30px auto; background:#fff; padding:18px; border-radius:12px;
                    box-shadow:0 4px 8px rgba(0,0,0,0.05); overflow:auto; white-space:pre-wrap; word-wrap:break-word; }
      .filename { font-weight:600; color:#333; text-align:center; margin-bottom:12px; }
      .error { color:#c0392b; text-align:center; margin:20px; }
      .back-btn { display:inline-block; margin:18px auto; padding:8px 16px; background:#0078d4; color:#fff;
                 text-decoration:none; border-radius:6px; }
      .back-btn:hover { background:#005fa3; }
    </style>
    </head><body>
    <div class="admin-container">
      <header><h1>File Viewer</h1><p class="subtitle">Inspect file content safely</p></header>
      <div class="content-box"><p class="error">Missing file path parameter.</p></div>
      <a class="back-btn" href="listFiles.groovy">← Back to File List</a>
      <footer><p>© 2025 Admin Panel | <a href="/home/">Back to Home</a></p></footer>
    </div>
    </body></html>"""
    return out.toString()
}

if (!requested.startsWith(webrootPath.resolve("home"))) {
    out << """<html><head><meta charset="UTF-8"><title>View File - Admin</title>
    <link rel="stylesheet" href="${cssLink}"></head><body>
    <div class="admin-container">
      <header><h1>File Viewer</h1><p class="subtitle">Inspect file content safely</p></header>
      <div class="content-box"><p class="error">Invalid file path.</p></div>
      <a class="back-btn" href="listFiles.groovy">← Back to File List</a>
      <footer><p>© 2025 Admin Panel | <a href="/home/">Back to Home</a></p></footer>
    </div>
    </body></html>"""
    return out.toString()
}

def file = new File(requested.toString())

if (!file.exists() || !file.isFile()) {
    out << """<html><head><meta charset="UTF-8"><title>View File - Admin</title>
    <link rel="stylesheet" href="${cssLink}"></head><body>
    <div class="admin-container">
      <header><h1>File Viewer</h1><p class="subtitle">Inspect file content safely</p></header>
      <div class="content-box"><p class="error">File not found: ${escapeHtml(requested)}</p></div>
      <a class="back-btn" href="listFiles.groovy">← Back to File List</a>
      <footer><p>© 2025 Admin Panel | <a href="/home/">Back to Home</a></p></footer>
    </div>
    </body></html>"""
    return out.toString()
}

// read file content (assume UTF-8; change if needed)
def fileContent = file.getText('UTF-8')
def safeContent = escapeHtml(fileContent)

out << """<html>
<head>
  <meta charset="UTF-8">
  <title>View: ${escapeHtml(param)}</title>
  <link rel="stylesheet" href="${cssLink}">
  <style>
    .content-box { max-width:900px; margin:30px auto; background:#fff; padding:18px; border-radius:12px;
                  box-shadow:0 4px 8px rgba(0,0,0,0.05); overflow:auto; }
    .filename { font-weight:600; color:#333; text-align:center; margin-bottom:12px; word-break:break-all; }
    pre.content { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; font-size:13px; margin:0; }
    .back-btn { display:inline-block; margin:18px auto; padding:8px 16px; background:#0078d4; color:#fff;
               text-decoration:none; border-radius:6px; }
    .back-btn:hover { background:#005fa3; }
  </style>
</head>
<body>
  <div class="admin-container">
    <header><h1>File Viewer</h1><p class="subtitle">Inspect file content safely</p></header>

    <div class="content-box">
      <div class="filename">${escapeHtml(param)}</div>
      <pre class="content">${safeContent}</pre>
    </div>

    <div style="text-align:center;">
      <a class="back-btn" href="listFiles.groovy">← Back to File List</a>
    </div>

    <footer><p>© 2025 Admin Panel | <a href="/home/">Back to Home</a></p></footer>
  </div>
</body>
</html>
"""

return out.toString()
