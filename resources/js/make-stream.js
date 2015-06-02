/* A very basic editor to test the capabilities of text-streaming */

function attachToElementAsEditor(el) {
    var sourceMap = {
        pos:0,
        text:""
    };
    
    var sock = new WebSocket(relativeURIWithPath("/api/new"));

    sock.onmessage = function(e) {
        var p = parseInt(e.data.substring(7));
        if (e.data.substring(0, 6) == "cnnect" && p >= 0) {
            var share = document.getElementById("share");
            share.href = "/s/" + p;
            share.innerText = "Share this stream!";
        }
    };

    function onKeyDown(e) {
        var BACKSPACE = 8;
        var TAB = 9;
        var LEFT = 37;
        // var UP = 38; // NOT YET IMPLEMENTED
        var RIGHT = 39;
        // var DOWN = 40; // NOT YET IMPLEMENTED
        var DELETE = 46; // AKA "Forward Delete"
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
        case DELETE:
            if (sourceMap.pos < sourceMap.text.length) {
                sock.send(fwdDeleteN(1, sourceMap));
            }
            break;
        default:
            // HANDLE THIS IN KEYPRESS
            break;
        }
        drawInElement(el, sourceMap);
    }
    
    document.addEventListener("keydown", onKeyDown);

    function onKeyPress(e) {
        var charCode = e.which || e.keyCode;
        var c = String.fromCharCode(charCode);
        if (charCode == 13) {
            // No carriage returns!
            c = '\n';
        }
        sock.send(insertT(c, sourceMap));
        drawInElement(el, sourceMap);
    }
    
    document.addEventListener("keypress", onKeyPress);

    function promptTitle() {
        var title = prompt("Please title this stream:", "Untitled Stream");
        sock.send("titled:" + title);
        document.title = "Streaming " + title;
        document.getElementById("title").innerText = title;
    }
    document.getElementById("set-title").addEventListener("click", promptTitle);
    
    sock.onopen = function(e) {
        sock.send("inited:" + sourceMap.text);
        promptTitle();
        sock.onclose = function(e) {
            el.innerHTML += '<h3>CONNECTION CLOSED</h3>s';
            document.removeEventListener('keypress', onKeyPress);
            document.removeEventListener('keydown', onKeyDown);
        };
    };
}

attachToElementAsEditor(document.getElementById("stream"));
