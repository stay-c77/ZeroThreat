import csv
import socket
from pathlib import Path
from urllib.parse import urlsplit

socket.setdefaulttimeout(1.5)
base = Path('/Users/sharonshalonmathew/Documents/zerothreat-git')
inp = base / 'target_testing_data_1000.csv'
out = base / 'target_testing_data_1000_dns_missing.csv'

cache = {}
rows = []

with inp.open('r', encoding='utf-8', newline='') as f:
    reader = csv.DictReader(f)
    for row in reader:
        idx = int(row['index'])
        url = row['url'].strip()
        parsed = urlsplit(url if '://' in url else 'https://' + url)
        host = (parsed.hostname or '').lower().removeprefix('www.').removesuffix('.')
        if not host:
            rows.append((idx, url, host, 'EMPTY_HOST'))
            continue
        if host in cache:
            ok = cache[host]
        else:
            try:
                socket.gethostbyname(host)
                ok = True
            except Exception:
                ok = False
            cache[host] = ok
        if not ok:
            rows.append((idx, url, host, 'DOMAIN_NOT_FOUND'))

rows.sort(key=lambda x: x[0])
with out.open('w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['index', 'url', 'extracted_domain', 'failure_reason'])
    writer.writerows(rows)

print('total_rows', sum(1 for _ in inp.open('r', encoding='utf-8')) - 1)
print('missing_count', len(rows))
print('unique_hosts_checked', len(cache))
print('output', out)
print('first_40:')
for r in rows[:40]:
    print(r)

