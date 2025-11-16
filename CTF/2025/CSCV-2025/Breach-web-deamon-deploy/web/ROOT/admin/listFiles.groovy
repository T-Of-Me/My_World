import java.nio.file.*

def patchme = evaluate(new File(webroot + "/patch/patchme.groovy"))
if (patchme != null) {
    return patchme
}

def webrootPath = Paths.get(webroot).toAbsolutePath().normalize()
def param = request.getParameter("path") ?: ""
def requested = webrootPath.resolve("home").resolve(param).normalize()


if (!requested.startsWith(webrootPath.resolve("home"))) {
    return "<h3>Access denied</h3>"
}

def currentDir = requested.toFile()
if (!currentDir.exists() || !currentDir.isDirectory()) {
    return "<h3>Invalid folder</h3>"
}

def out = new StringBuilder()
out << """
<html>
<head>
    <meta charset="UTF-8">
    <title>File Listing</title>
    <style>
        body {
            background-color: #f4f6f8;
            font-family: "Segoe UI", Roboto, sans-serif;
            margin: 40px;
            color: #333;
        }
        h2 {
            color: #222;
            font-weight: 500;
            border-bottom: 2px solid #ccc;
            padding-bottom: 8px;
        }
        .file-list {
            margin-top: 20px;
            background: #fff;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .item {
            display: block;
            padding: 10px 14px;
            border-radius: 6px;
            text-decoration: none;
            color: #0066cc;
            font-size: 15px;
        }
        .item:hover {
            background-color: #eaf0f8;
        }
        .folder {
            font-weight: 600;
            color: #2c3e50;
        }
        .file {
            color: #34495e;
        }
        .up {
            margin-bottom: 12px;
            display: inline-block;
            text-decoration: none;
            color: #555;
        }
        .up:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <h2>Listing for: ${currentDir.canonicalPath.replace(webroot, "")}</h2>
    <div class="file-list">
"""

// Add "Up" link if not in root
def homePath = webrootPath.resolve("home")
if (!requested.equals(homePath)) {
    def parentRel = homePath.relativize(requested.parent).toString().replace("\\", "/")
    out << "<a class='up' href='?path=${parentRel}'>↑ Go up</a><br><br>"
}

def entries = currentDir.listFiles()
        ?.findAll { it.isDirectory() || (it.isFile() && it.name.startsWith("_")) }
        ?.sort { it.name.toLowerCase() }

entries?.each { f ->
    def relPath = homePath.relativize(f.toPath()).toString().replace("\\", "/")
    if (f.isDirectory()) {
        out << "<a class='item folder' href='?path=${relPath}'>${f.name}/</a>\n"
    } else {
        out << "<a class='item file' href='view.groovy?path=${relPath}'>${f.name}</a>\n"
    }
}

out << """
    </div>
</body>
</html>
"""
return out.toString()