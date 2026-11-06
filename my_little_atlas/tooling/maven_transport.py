"""Local Maven transport for environments where Java TLS is unavailable.

Only proxies Maven artifact paths from an allowlist; no credentials or arbitrary URLs.
"""
import hashlib
import os
from pathlib import Path
import threading
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parent
CACHE = ROOT / 'maven-cache'
UPSTREAMS = {
    'google': 'https://dl.google.com/dl/android/maven2/',
    'central': 'https://repo.maven.apache.org/maven2/',
    'plugins': 'https://plugins.gradle.org/m2/',
}
LOCKS = {}
GUARD = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        self.serve(False)

    def do_GET(self):
        self.serve(True)

    def serve(self, body):
        parts = self.path.lstrip('/').split('/', 1)
        if len(parts) != 2 or parts[0] not in UPSTREAMS or '..' in parts[1] or '?' in parts[1]:
            self.send_error(400)
            return
        kind, path = parts
        file = CACHE / kind / path
        with GUARD:
            lock = LOCKS.setdefault(self.path, threading.Lock())
        try:
            with lock:
                if not file.exists():
                    data = None
                    # Read only from the existing artifact cache when available.
                    legacy = os.environ.get('ATLAS_MAVEN_SEED')
                    if legacy:
                        seed = Path(legacy) / hashlib.sha256(f'{kind}/{path}'.encode()).hexdigest()
                        if seed.exists():
                            data = seed.read_bytes()
                    if data is None:
                        for attempt in range(3):
                            try:
                                request = urllib.request.Request(UPSTREAMS[kind] + path, headers={'User-Agent': 'Atlas-Build/0.1'})
                                with urllib.request.urlopen(request, timeout=60) as response:
                                    data = response.read()
                                break
                            except urllib.error.HTTPError as error:
                                if error.code == 404:
                                    self.send_error(404)
                                    return
                                if attempt == 2:
                                    raise
                    file.parent.mkdir(parents=True, exist_ok=True)
                    file.write_bytes(data)
            size = file.stat().st_size
            self.send_response(200)
            self.send_header('Content-Length', str(size))
            self.end_headers()
            if body:
                self.wfile.write(file.read_bytes())
        except Exception as error:
            print(f'ERROR {self.path}: {error}', flush=True)
            self.send_error(502)

    def log_message(self, format, *args):
        if args and str(args[1] if len(args) > 1 else '') not in ('200', '404'):
            super().log_message(format, *args)


if __name__ == '__main__':
    print('Atlas Maven transport: http://127.0.0.1:37661', flush=True)
    ThreadingHTTPServer(('127.0.0.1', 37661), Handler).serve_forever()
