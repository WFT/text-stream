function attachToElement(el) {
    var sourceMap = {
        pos:0,
        text:""
    };
    var sock = new WebSocket(relativeURIWithPath("/api/s/" + sid));
    sock.onmessage = function(e) {
        var cmd = e.data.substring(0, cmdLen);
        var data = e.data.substring(cmdLen + 1);
        switch (cmd) {
        case "insert":
            insertT(data, sourceMap);
            break;
        case "delete":
            var n = parseInt(data);
            if (n) {
                deleteN(n, sourceMap);
            }
            break;
        case "cursor":
            var p = parseInt(data);
            if (p) {
                cursorP(p, sourceMap);
            }
            break;
        }
        drawInElement(el, sourceMap);
    };
    sock.onclose = function() {
        el.innerHTML += '<h3 class="eof">END OF TRANSMISSION</h3>'        
    }
    sock.onopen = function() {
        sock.send("go");
    };
}
attachToElement(document.getElementById("stream"));
