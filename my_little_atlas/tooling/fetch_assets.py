import concurrent.futures
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'composeApp/src/commonMain/composeResources/drawable'
ASSETS = {
    'cafe': 'photo-1501339847302-ac426a4a7cbb',
    'coffee': 'photo-1442512595331-e89e73853f31',
    'bookshop': 'photo-1507842217343-583bb7270b66',
    'dessert': 'photo-1484723091739-30a097e8f929',
    'city': 'photo-1537531383496-f4749b8032cf',
    'interior': 'photo-1554118811-1e0d58224f24',
}


def fetch(item):
    name, photo = item
    OUT.mkdir(parents=True, exist_ok=True)
    url = f'https://images.unsplash.com/{photo}?auto=format&fit=crop&w=1000&q=85'
    with urllib.request.urlopen(url, timeout=45) as response:
        data = response.read()
    if len(data) < 2000:
        raise ValueError(f'Invalid image: {name}')
    (OUT / f'{name}.jpg').write_bytes(data)
    print(name, len(data), flush=True)


if __name__ == '__main__':
    list(concurrent.futures.ThreadPoolExecutor(max_workers=4).map(fetch, ASSETS.items()))
