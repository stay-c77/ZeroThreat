import csv
import re
import socket
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed

inp = '/Users/sharonshalonmathew/Documents/zerothreat-git/phishing_urls.csv'
out = '/Users/sharonshalonmathew/Documents/zerothreat-git/dns_check_results_1000.csv'

proto_re = re.compile(r'^https?://', re.IGNORECASE)
www_re = re.compile(r'^www\.', re.IGNORECASE)


def extract_domain(url: str) -> str:
    d = url.strip()
    d = proto_re.sub('', d)
    d = www_re.sub('', d)
    d = d.split('/')[0]
    d = d.split('?')[0]
    return d.strip()


rows = []
counts = Counter()

socket.setdefaulttimeout(1.5)


def classify_record(rec):
    idx = int(rec['index'])
    raw = rec['url']
    domain = extract_domain(raw)

    if not domain:
        return [idx, raw, domain, 'INVALID_URL', 'Invalid URL format', '']

    try:
        ip = socket.gethostbyname(domain)
        return [idx, raw, domain, 'NONE', 'URL exists - DNS resolved successfully', ip]
    except socket.gaierror as e:
        return [idx, raw, domain, 'DOMAIN_NOT_FOUND', f'URL does not exist - DNS resolution failed ({e})', '']
    except Exception as e:
        return [idx, raw, domain, 'LOOKUP_ERROR', f'Error checking URL: {e.__class__.__name__}: {e}', '']

with open(inp, newline='', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    records = list(reader)

with ThreadPoolExecutor(max_workers=40) as pool:
    futures = [pool.submit(classify_record, rec) for rec in records]
    for fut in as_completed(futures):
        row = fut.result()
        rows.append(row)
        counts[row[3]] += 1

rows.sort(key=lambda x: x[0])

with open(out, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['index', 'input_url', 'extracted_domain', 'failure_reason', 'message', 'resolved_ip'])
    writer.writerows(rows)

phishing_dns_found = 0
phishing_dns_notfound = 0
safe_dns_found = 0
safe_dns_notfound = 0

for row in rows:
    idx = row[0]
    reason = row[3]
    is_phishing = 1 <= idx <= 600
    is_found = reason == 'NONE'

    if is_phishing and is_found:
        phishing_dns_found += 1
    elif is_phishing and not is_found:
        phishing_dns_notfound += 1
    elif not is_phishing and is_found:
        safe_dns_found += 1
    elif not is_phishing and not is_found:
        safe_dns_notfound += 1

print('TOTAL', len(rows))
print('NONE', counts['NONE'])
print('DOMAIN_NOT_FOUND', counts['DOMAIN_NOT_FOUND'])
print('INVALID_URL', counts['INVALID_URL'])
print('LOOKUP_ERROR', counts['LOOKUP_ERROR'])
print()
print('BREAKDOWN BY LABEL:')
print('  Phishing (1-600):')
print('    DNS found:', phishing_dns_found)
print('    DNS not found:', phishing_dns_notfound)
print('  Safe (601-1000):')
print('    DNS found:', safe_dns_found)
print('    DNS not found:', safe_dns_notfound)
print()
print('OUT', out)

