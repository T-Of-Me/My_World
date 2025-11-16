def patchme = evaluate(new File(webroot + "/patch/patchme.groovy"))
if (patchme != null) {
    return patchme
}

def content = param["content"]
def pageId = param["pageId"]
def path = "/home/sub/" + pageId + ".page"
def fullPath = webroot + path

if (content && pageId && !pageId.contains("..")) {
    def file = new File(fullPath)
    file.text = content
    return """
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Saved</title>
        <link rel="stylesheet" href="/assets/theme.css">
    </head>
    <body>
        <div class="container" style="text-align:center;">
            <h2>File updated successfully</h2>
            <p><strong>${path}</strong></p>
            <a href="?">← Back</a>
        </div>
    </body>
    </html>
    """
} else {
    return """
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Edit Page</title>
        <link rel="stylesheet" href="/assets/theme.css">
        <script>
            function handleSubmit(form) {
                const name = form.pageId.value.trim();
                const content = form.content.value.trim();
                if (!name || !content) {
                    alert("Please fill out both pageId and content.");
                    return false;
                }
                return true;
            }
        </script>
    </head>
    <body>
        <div class="container">
            <h2>Edit Page</h2>
            <form method="post" onsubmit="return handleSubmit(this)">
                <label for="pageId">pageId</label>
                <input type="text" id="pageId" name="pageId" placeholder="example" required>

                <label for="content">Content</label>
                <textarea id="content" name="content" placeholder="Enter your page content here..." required></textarea>

                <button type="submit">Save</button>
            </form>
        </div>
    </body>
    </html>
    """
}