"""Supply a moving RMC fix on the emulator host to exercise the phone GPS input."""
import datetime
import socketserver
import time

class Phone(socketserver.StreamRequestHandler):
    def handle(self):
        for step in range(180):
            utc = datetime.datetime.now(datetime.timezone.utc)
            minutes = 25.956 + step * .002
            sentence = f"GNRMC,{utc:%H%M%S},A,19{minutes:06.3f},N,09907.992,W,9.72,0,{utc:%d%m%y},,,A"
            checksum = 0
            for byte in sentence.encode("ascii"):
                checksum ^= byte
            try:
                self.wfile.write(b"$" + f"{sentence}*{checksum:02X}".encode("ascii") + bytes([13, 10]))
            except OSError:
                return
            time.sleep(1)

with socketserver.ThreadingTCPServer(("127.0.0.1", 31123), Phone) as server:
    server.serve_forever()
