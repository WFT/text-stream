function insertT(t, sourceMap) {
    var beginText = sourceMap.text.substring(0, sourceMap.pos);
    var endText = sourceMap.text.substring(sourceMap.pos);
    sourceMap.text = beginText + t + endText;
    sourceMap.pos += t.length;
    return "+"+t;
}
function deleteN(n, sourceMap) {
    sourceMap.pos -= n;
    var beginText = sourceMap.text.substring(0, sourceMap.pos);
    var endText = sourceMap.text.substring(sourceMap.pos + n);
    sourceMap.text = beginText + endText;
    return "-"+n;
}
function fwdDeleteN(n, sourceMap) {
    cursorP(sourceMap.pos + n, sourceMap);
    deleteN(n, sourceMap);
    return "d"+n;
}
function cursorP(p, sourceMap) {
    sourceMap.pos = p;
    return "c"+p;
}
function cursorL(n, sourceMap) {
    sourceMap.pos -= n;
    return n == 1 ? "<" : "<"+n;
}
function cursorR(n, sourceMap) {
    sourceMap.pos += n;
    return n == 1 ? ">" : ">"+n;
}

var curs = document.createElement('span');
curs.className = 'cursor';

function drawInElement(el, sourceMap) {
    var t = sourceMap.text + ' ';
    el.innerHTML = '';
    el.appendChild(document.createTextNode(t.substring(0, sourceMap.pos)));
    curs.innerText = t.substring(sourceMap.pos, sourceMap.pos+1)
    el.appendChild(curs);
    el.appendChild(document.createTextNode(t.substring(sourceMap.pos + 1)));
}

var cmdLen = 1;

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
