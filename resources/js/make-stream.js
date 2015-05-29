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
    var sendQueue = ["inited:"+sourceMap.text];
    var lock = 0;
    function queueMessage(cmd, arg) {
        lock++;
        var i = sendQueue.length - 1;
        if (i < 0) {
            sendQueue.push(cmd + ":" + arg);
            lock--;
            return;
        }
        var lastMessage = sendQueue[i];
        var lastCmd = lastMessage.substring(0, cmdLen);
        if (lastCmd == cmd) {
            var lastArg = lastMessage.substring(cmdLen + 1);
            var newCmd = cmd + ":";
            switch (cmd) {
            case "insert":
                newCmd += lastArg + arg;
                break;
            case "delete":
                var p = parseInt(lastArg) + arg;
                newCmd += p;
                break;
            case "cursor":
                var p = parseInt(lastArg) + arg;
                newCmd += p;
                break;
            }
            sendQueue[i] = newCmd;
        } else {
            sendQueue.push(cmd + ":" + arg)
        }
        lock--;
    }
    sock.onmessage = function(e) {
        var p = parseInt(e.data.substring(7));
        if (e.data.substring(0, 6) == "cnnect" && p) {
            console.log('stream: ' + p);
        }
    };
    function sendMessages() {
        if (!lock) {
            for(var i = 0; i < sendQueue.length; i++) {
                sock.send(sendQueue.shift());
            }
            window.setInterval(sendMessages, 500);
        } else {
            window.setInterval(sendMessages, 300);
        }
    }
    sock.onopen = function(e) {
        sendMessages();
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
                deleteN(1, sourceMap);
                queueMessage("delete", 1);
            }
            break;
        case TAB:
            e.preventDefault();
            insertT("\t", sourceMap);
            queueMessage("insert", "\t");
            break;
        case LEFT:
            if (sourceMap.pos > 0) {
                var p = sourceMap.pos - 1;
                cursorP(p, sourceMap);
                queueMessage("cursor", p);
            }
            break;
        case RIGHT:
            if (sourceMap.pos < sourceMap.text.length) {
                var p = sourceMap.pos + 1;
                cursorP(p, sourceMap);
                queueMessage("cursor", p);
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
        insertT(c, sourceMap);
        queueMessage("insert", c);
        drawInElement(el, sourceMap);
    });
}

attachToElementAsEditor(document.getElementById("stream"));
