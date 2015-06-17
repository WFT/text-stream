# text-stream

A simple one-way text streaming service.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

You will need [PostgreSQL][] 9.4.x.

[postgresql]: http://www.postgresql.org

## Running

Just run `lein run [port]` (port will default to 8080). You'll need to run postgres. `text-stream` will use a database named `textstream`. The table creation command can be seen in `schema.psql`.


## Protocol

The streaming protocol is implemented over WebSockets.
Every message consists of a command followed by an argument or
nothing simply the command, in which case the default argument will be
used (if the command supports default arguments).

`<COMMAND><ARGUMENT>` i.e. `+Hello, world!` or `<`.

### Shared (Editing) Commands

`+` : Insert argument at the cursor position, moving the
cursor to the end of the inserted text. *No default argument*.

`-` : Where the argument `N` is a number which indicates the number of
characters to delete (backwards) from the cursor. `N` defaults to `1`.

`d` : Where the argument `N` is a number which indicates the number of
characters to delete (forwards) from the cursor. `N` defaults to `1`.

`c` : Where the argument `P` is a number which indicates the index at
which to place the cursor. *No default argument*.

`<` : Where the argument `N` is a number which indicates by how much
the cursor position will be decremented (i.e. moved to the left). `N`
defaults to `1`.

`>` : Where the argument `N` is a number which indicates by how much
the cursor position will be incremented (i.e. moved to the right). `N`
defaults to `1`.

`t` : Set the title of this stream to the argument. *No default argument*

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

`go` : Sent by the viewer to indicate that it is ready to receive
commands. This command has no arguments.

### Stream Author Commands

`i` : Set the argument `TEXT` as the initial value of the
stream. `iTEXT` is equivalent to `+TEXT`, but only sent once, at the
beginning of the stream. This message *must* be sent before any other
messages.

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
