function relativeURIWithPath(path) {
    var loc = window.location, new_uri;
    if (loc.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
    new_uri += "//" + loc.host;
    new_uri += path;
    return new_uri;
}
function insertT(t, sourceMap) {
    var beginText = sourceMap.text.substring(0, sourceMap.pos);
    var endText = sourceMap.text.substring(sourceMap.pos);
    sourceMap.text = beginText + t + endText;
    sourceMap.pos += t.length;
}
function deleteN(n, sourceMap) {
    sourceMap.pos -= n;
    var beginText = sourceMap.text.substring(0, sourceMap.pos);
    var endText = sourceMap.text.substring(sourceMap.pos + n);
    sourceMap.text = beginText + endText;
}
function cursorP(p, sourceMap) {
    sourceMap.pos = p;
}
var cmdLen = 6;
var curs = '<span class="cursor">|</span>';
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
        el.innerHTML =
            sourceMap.text.substring(0, sourceMap.pos) +
            curs +
            sourceMap.text.substring(sourceMap.pos);
    };
    sock.onclose = function() {
        el.innerHTML += '<h3 class="eof">END OF TRANSMISSION</h3>'        
    }
    sock.onopen = function() {
        sock.send("go");
    };
}
attachToElement(document.getElementById("stream"));
