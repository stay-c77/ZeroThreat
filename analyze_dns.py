import csv
import re
import socket
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed

inp = '/Users/sharonshalonmathew/Desktop/phishing_urls_claude.csv'
out = '/Users/sharonshalonmathew/Documents/zerothreat-git/dns_check_results_claude.csv'

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
    idx = int(rec['id'])
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

print(f"Processing {len(records)} URLs...")

with ThreadPoolExecutor(max_workers=40) as pool:
    futures = [pool.submit(classify_record, rec) for rec in records]
    for i, fut in enumerate(as_completed(futures)):
        row = fut.result()
        rows.append(row)
        counts[row[3]] += 1
        if (i + 1) % 100 == 0:
            print(f"Processed {i + 1}/{len(records)} URLs...")

rows.sort(key=lambda x: x[0])

with open(out, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['index', 'input_url', 'extracted_domain', 'failure_reason', 'message', 'resolved_ip'])
    writer.writerows(rows)

print("\n" + "="*50)
print("DNS CHECK RESULTS SUMMARY")
print("="*50)
print(f'TOTAL: {len(rows)}')
print(f'PASSED DNS CHECK (NONE): {counts["NONE"]}')
print(f'FAILED DNS CHECK (DOMAIN_NOT_FOUND): {counts["DOMAIN_NOT_FOUND"]}')
print(f'INVALID_URL: {counts["INVALID_URL"]}')
print(f'LOOKUP_ERROR: {counts["LOOKUP_ERROR"]}')
print("\n" + "="*50)
print(f"DNS PASS RATE: {counts['NONE']}/{len(rows)} ({100*counts['NONE']/len(rows):.2f}%)")
print("="*50)
print(f"\nResults saved to: {out}")

