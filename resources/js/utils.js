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

function curs(t) {
    var c = t;
    if (c == '\n' || c == '') {
        c = ' ' + c;
    }
    return '<span class="cursor">' + c + '</span>'
}

function drawInElement(el, sourceMap) {
    var t = sourceMap.text + ' ';
    el.innerHTML =
        t.substring(0, sourceMap.pos) +
        curs(t.substring(sourceMap.pos, sourceMap.pos+1)) +
        t.substring(sourceMap.pos+1);
}

var cmdLen = 6;

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
