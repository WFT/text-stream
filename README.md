# text-stream

A simple one-way text streaming service.

## Protocol

The streaming protocol is implemented over WebSockets.
Every message consists of a command followed by a `:` followed
by an argument: `command:argument`.

### Shared Commands

`insert:TEXT` : Insert `TEXT` at the cursor position, moving the
cursor to the end of the inserted text.

`delete:N`    : Where `N` is a number which indicates the number of
characters to delete (backwards) from the cursor.

`fwddel:N`    : Where `N` is a number which indicates the number of
characters to delete (forwards) from the cursor.

`cursor:P`    : Where `P` is a number which indicates the index at
which to place the cursor.

`titled:TITLE` : Set the title of this stream to `TITLE`.

### Viewer Commands
Once a WebSocket connection is opened to `/api/s/[SID]` (where `SID`
is a valid socket ID), send `go` and begin mirroring all commands
received.

**NOTE:** Only *shared commands* will be sent. The viewer never needs
to worry about *stream author commands*.

**NOTE:** The commands received won't necessarily match the ones sent by
 the stream's author, but they should reproduce the same `source-map`
 (i.e. exact combination of text, cursor position, and title). This is
 a consequence of the above note, but other optimizations of received
 data may be applied.

`go` : Sent to indicate that the viewer is ready to receive
commands. This command has no arguments, and therefore no `:`.

### Stream Author Commands

`inited:TEXT`  : Set this text as the initial value of the
stream. Equivalent to `insert:TEXT`, but only sent once, at the
beginning of the stream. This message *must* be sent before any other
messages.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

Just run `lein run [port]` (port will default to 8080).

## License

```
The MIT License (MIT)

Copyright (c) 2015 Will Field-Thompson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
