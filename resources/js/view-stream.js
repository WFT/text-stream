function attachToElement(el) {
    var sourceMap = {
        pos:0,
        text:""
    };
    var sock = new WebSocket(relativeURIWithPath("/api/s/" + sid));
    sock.onmessage = function(e) {
        var cmd = e.data.substring(0, cmdLen);
        var data = e.data.substring(cmdLen);
        switch (cmd) {
        case "+":
            insertT(data, sourceMap);
            break;
        case "-":
            var n = parseInt(data);
            if (data === "") { n = 1; }
            if (n > 0) {
                deleteN(n, sourceMap);
            }
            break;
        case "c":
            var p = parseInt(data);
            if (p >= 0 && p <= sourceMap.text.length) {
                cursorP(p, sourceMap);
            }
            break;
        case "<":
            var n = parseInt(data);
            if (data === "") { n = 1; }
            if (n <= sourceMap.pos) {
                cursorL(n, sourceMap);
            }
            break;
        case ">":
            var n = parseInt(data);
            if (data === "") { n = 1; }
            if (sourceMap.pos + n <= sourceMap.text.length) {
                cursorR(n, sourceMap);
            }
            break;
        case "d":
            var n = parseInt(data);
            if (data === "") { n = 1; }
            if (n > 0) {
                fwdDeleteN(n, sourceMap);
            }
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
