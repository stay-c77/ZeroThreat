import csv
import socket
import re
from concurrent.futures import ThreadPoolExecutor, as_completed

# Configuration
URLSET_FILE = '/Users/sharonshalonmathew/Downloads/urlset.csv'
URLDATA_FILE = '/Users/sharonshalonmathew/Downloads/urldata.csv'
OUTPUT_FILE = '/Users/sharonshalonmathew/Documents/zerothreat-git/testing_data.csv'

# DNS settings
socket.setdefaulttimeout(1.5)
proto_re = re.compile(r'^https?://', re.IGNORECASE)
www_re = re.compile(r'^www\.', re.IGNORECASE)

def extract_domain(url: str) -> str:
    """Extract domain from URL"""
    d = url.strip()
    d = proto_re.sub('', d)
    d = www_re.sub('', d)
    d = d.split('/')[0]
    d = d.split('?')[0]
    return d.strip()

def check_dns(url: str) -> bool:
    """Check if URL passes DNS resolution"""
    try:
        domain = extract_domain(url)
        if not domain:
            return False
        socket.gethostbyname(domain)
        return True
    except:
        return False

def extract_urls_from_urlset(filepath, limit=600):
    """Extract URLs from urlset.csv that pass DNS check"""
    print(f"\nProcessing urlset.csv...")
    urls = []

    try:
        with open(filepath, 'r', encoding='latin-1', errors='ignore') as f:
            reader = csv.DictReader(f)
            for idx, row in enumerate(reader):
                try:
                    url = row.get('domain', '').strip()
                    if url and url != '""' and len(url) > 5:
                        urls.append(url)
                    if idx % 5000 == 0:
                        print(f"  Read {idx} rows from urlset.csv...")
                except:
                    continue
    except Exception as e:
        print(f"  Error reading urlset.csv: {e}")

    print(f"Total URLs extracted from urlset.csv: {len(urls)}")
    print(f"Checking DNS for URLs...")

    passed_urls = []
    with ThreadPoolExecutor(max_workers=20) as pool:
        futures = [(url, pool.submit(check_dns, url)) for url in urls]
        for i, (url, future) in enumerate(futures):
            try:
                if future.result():
                    passed_urls.append(url)
                    if len(passed_urls) % 50 == 0:
                        print(f"  Found {len(passed_urls)} DNS-passed URLs...")
            except:
                pass

            if len(passed_urls) >= limit:
                break

            if (i + 1) % 100 == 0:
                print(f"  Checked {i + 1} URLs...")

    print(f"✓ Collected {len(passed_urls)} DNS-passed URLs from urlset.csv")
    return passed_urls[:limit]

def extract_urls_from_urldata(filepath, limit=400):
    """Extract URLs from urldata.csv that pass DNS check"""
    print(f"\nProcessing urldata.csv...")
    urls = []

    try:
        with open(filepath, 'r', encoding='latin-1', errors='ignore') as f:
            reader = csv.DictReader(f)
            for idx, row in enumerate(reader):
                try:
                    url = row.get('url', '').strip()
                    if url and url != '""' and len(url) > 5:
                        urls.append(url)
                    if idx % 50000 == 0:
                        print(f"  Read {idx} rows from urldata.csv...")
                except:
                    continue
    except Exception as e:
        print(f"  Error reading urldata.csv: {e}")

    print(f"Total URLs extracted from urldata.csv: {len(urls)}")
    print(f"Checking DNS for URLs...")

    passed_urls = []
    with ThreadPoolExecutor(max_workers=20) as pool:
        futures = [(url, pool.submit(check_dns, url)) for url in urls]
        for i, (url, future) in enumerate(futures):
            try:
                if future.result():
                    passed_urls.append(url)
                    if len(passed_urls) % 50 == 0:
                        print(f"  Found {len(passed_urls)} DNS-passed URLs...")
            except:
                pass

            if len(passed_urls) >= limit:
                break

            if (i + 1) % 500 == 0:
                print(f"  Checked {i + 1} URLs...")

    print(f"✓ Collected {len(passed_urls)} DNS-passed URLs from urldata.csv")
    return passed_urls[:limit]

# Main execution
print("="*60)
print("CREATING TESTING DATA WITH DNS-PASSED URLS")
print("="*60)

# Extract URLs
urlset_urls = extract_urls_from_urlset(URLSET_FILE, limit=600)
urldata_urls = extract_urls_from_urldata(URLDATA_FILE, limit=400)

# Combine URLs
all_urls = urlset_urls + urldata_urls

print(f"\n" + "="*60)
print("SUMMARY")
print("="*60)
print(f"URLSet (DNS-passed):  {len(urlset_urls)}/600")
print(f"URLData (DNS-passed): {len(urldata_urls)}/400")
print(f"Total URLs:           {len(all_urls)}/1000")

# Write to CSV
with open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['url'])
    for url in all_urls:
        writer.writerow([url])

print(f"\n✓ CSV file created: {OUTPUT_FILE}")
print(f"  Total rows: {len(all_urls)}")
