/* A very basic editor to test the capabilities of text-streaming */

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
    return "insert:"+t;
}
function deleteN(n, sourceMap) {
    sourceMap.pos -= n;
    var beginText = sourceMap.text.substring(0, sourceMap.pos);
    var endText = sourceMap.text.substring(sourceMap.pos + n);
    sourceMap.text = beginText + endText;
    return "delete:"+n;
}
function cursorP(p, sourceMap) {
    sourceMap.pos = p;
    return "cursor:"+p;
}

var curs = '<span class="cursor">|</span>';
function drawInElement(el, sourceMap) {
    el.innerHTML =
        sourceMap.text.substring(0, sourceMap.pos) +
        curs +
        sourceMap.text.substring(sourceMap.pos);
}
var cmdLen = 6;
function attachToElementAsEditor(el) {
    var sourceMap = {
        pos:el.innerText.length,
        text:el.innerText
    };
    var sock = new WebSocket(relativeURIWithPath("/api/new"));
    sock.onmessage = function(e) {
        var p = parseInt(e.data.substring(7));
        if (e.data.substring(0, 6) == "cnnect" && p) {
            var share = document.getElementById("share");
            share.href = "/s/" + p;
            share.innerText = "Share this stream!";
            /*var status = document.getElementById("status");
            status.innerText = "stream " + p + " connected! OK";*/
        }
    };
    sock.onopen = function(e) {
        sock.send("inited:" + sourceMap.text);
    };

    document.addEventListener("keydown", function(e) {
        var BACKSPACE = 8;
        var TAB = 9;
        var LEFT = 37;
        var UP = 38; // NOT YET IMPLEMENTED
        var RIGHT = 39;
        var DOWN = 40; // NOT YET IMPLEMENTED
        var charCode = e.which || e.keyCode;
        switch (charCode) {
        case BACKSPACE:
            e.preventDefault();
            if (sourceMap.pos > 0) {
                sock.send(deleteN(1, sourceMap));
            }
            break;
        case TAB:
            e.preventDefault();
            sock.send(insertT("\t", sourceMap));
            break;
        case LEFT:
            if (sourceMap.pos > 0) {
                var p = sourceMap.pos - 1;
                sock.send(cursorP(p, sourceMap));
            }
            break;
        case RIGHT:
            if (sourceMap.pos < sourceMap.text.length) {
                var p = sourceMap.pos + 1;
                sock.send(cursorP(p, sourceMap));
            }
            break;
        default:
            // HANDLE THIS IN KEYPRESS
            break;
        }
        drawInElement(el, sourceMap);
    });

    document.addEventListener("keypress", function(e) {
        var charCode = e.which || e.keyCode;
        var c = String.fromCharCode(charCode);
        sock.send(insertT(c, sourceMap));
        drawInElement(el, sourceMap);
    });
}

attachToElementAsEditor(document.getElementById("stream"));
